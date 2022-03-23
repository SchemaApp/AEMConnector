// Copyright (C) 2012 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
package com.schemaapp.core.util;
import java.math.BigInteger;

/**
 * Given JSON-like content, converts it to valid JSON. This can be attached at
 * either end of a data-pipeline to help satisfy Postel's principle:
 * <blockquote> be conservative in what you do, be liberal in what you accept
 * from others </blockquote>
 * <p>
 * Applied to JSON-like content from others, it will produce well-formed JSON
 * that should satisfy any parser you use.
 * <p>
 * Applied to your output before you send, it will coerce minor mistakes in
 * encoding and make it easier to embed your JSON in HTML and XML.
 *
 * <h2>Input</h2> The sanitizer takes JSON like content, and interprets it as JS
 * eval would. Specifically, it deals with these non-standard constructs.
 * <ul>
 * <li>{@code '...'} Single quoted strings are converted to JSON strings.
 * <li>{@code \xAB} Hex escapes are converted to JSON unicode escapes.
 * <li>{@code \012} Octal escapes are converted to JSON unicode escapes.
 * <li>{@code 0xAB} Hex integer literals are converted to JSON decimal numbers.
 * <li>{@code 012} Octal integer literals are converted to JSON decimal numbers.
 * <li>{@code +.5} Decimal numbers are coerced to JSON's stricter format.
 * <li>{@code [0,,2]} Elisions in arrays are filled with {@code null}.
 * <li>{@code [1,2,3,]} Trailing commas are removed.
 * <li><code>{foo:"bar"}</code> Unquoted property names are quoted.
 * <li><code>//comments</code> JS style line and block comments are removed.
 * <li><code>(...)</code> Grouping parentheses are removed.
 * </ul>
 *
 * The sanitizer fixes missing punctuation, end quotes, and mismatched or
 * missing close brackets. If an input contains only white-space then the valid
 * JSON string {@code null} is substituted.
 *
 * <h2>Output</h2> The output is well-formed JSON as defined by
 * <a href="http://www.ietf.org/rfc/rfc4627.txt">RFC 4627</a>. The output
 * satisfies three additional properties:
 * <ol>
 * <li>The output will not contain the substring (case-insensitively)
 * {@code "</script"} so can be embedded inside an HTML script element without
 * further encoding.<li>The output will not contain the substring {@code "]]>"}
 * so can be embedded inside an XML CDATA section without further encoding.</li>
 * <li>The output is a valid Javascript expression, so can be parsed by
 * Javascript's <code>eval</code> builtin (after being wrapped in parentheses)
 * or by <code>JSON.parse</code>. Specifically, the output will not contain any
 * string literals with embedded JS newlines (U+2028 Paragraph separator or
 * U+2029 Line separator).
 * <li>The output contains only valid Unicode scalar values (no isolated UTF-16
 * surrogates) that are <a href="http://www.w3.org/TR/xml/#charsets">allowed in
 * XML</a> unescaped.
 * </ol>
 *
 * <h2>Security</h2> Since the output is well-formed JSON, passing it to
 * <code>eval</code> will have no side-effects and no free variables, so is
 * neither a code-injection vector, nor a vector for exfiltration of secrets.
 *
 * <p>
 * This library only ensures that the JSON string &rarr; Javascript object phase
 * has no side effects and resolves no free variables, and cannot control how
 * other client side code later interprets the resulting Javascript object. So
 * if client-side code takes a part of the parsed data that is controlled by an
 * attacker and passes it back through a powerful interpreter like {@code eval}
 * or {@code innerHTML} then that client-side code might suffer unintended
 * side-effects.
 *
 * <h2>Efficiency</h2> The sanitize method will return the input string without
 * allocating a new buffer when the input is already valid JSON that satisfies
 * the properties above. Thus, if used on input that is usually well formed, it
 * has minimal memory overhead.
 * <p>
 * The sanitize method takes O(n) time where n is the length in UTF-16
 * code-units.
 */
public final class JsonSanitizer {

	public static final int DEFAULT_NESTING_DEPTH = 64;
	public static final int MAXIMUM_NESTING_DEPTH = 4096;
	private final int maximumNestingDepth;
	private final String jsonish;
	private int bracketDepth;
	private boolean[] isMap;
	private StringBuilder sanitizedJson;
	private int cleaned;
	private static final UnbracketedComma UNBRACKETED_COMMA;
	private static final char[] HEX_DIGITS;
	private static final int[] DIGITS_BY_BASE_THAT_FIT_IN_63B;

	public static String sanitize(final String jsonish) {
		return sanitize(jsonish, 64);
	}

	public static String sanitize(final String jsonish, final int maximumNestingDepth) {
		final JsonSanitizer s = new JsonSanitizer(jsonish, maximumNestingDepth);
		s.sanitize();
		return s.toString();
	}

	/**
	   * Given JSON-like content, produces a string of JSON that is safe to embed,
	   * safe to pass to JavaScript's {@code eval} operator.
	   *
	   * @param jsonish JSON-like content.
	   * @return embeddable JSON
	   */
	JsonSanitizer(final String jsonish) {
		this(jsonish, 64);
	}

	 /**
	   * Same as {@link JsonSanitizer#sanitize(String)}, but allows to set a custom
	   * maximum nesting depth.
	   *
	   * @param jsonish JSON-like content.
	   * @param maximumNestingDepth maximum nesting depth.
	   * @return embeddable JSON
	   */
	JsonSanitizer(final String jsonish, final int maximumNestingDepth) {
		this.maximumNestingDepth = Math.min(Math.max(1, maximumNestingDepth), 4096);
		this.jsonish = ((jsonish != null) ? jsonish : "null");
	}

	int getMaximumNestingDepth() {
		return this.maximumNestingDepth;
	}

	void sanitize() {
		final int n2 = 0;
		this.cleaned = n2;
		this.bracketDepth = n2;
		this.sanitizedJson = null;
		State state = State.START_ARRAY;
		final int n = this.jsonish.length();
		Label_1192:
			for (int i = 0; i < n; ++i) {
				try {
					final char ch = this.jsonish.charAt(i);
					switch (ch) {
					case '\t':
					case '\n':
					case '\r':
					case ' ': {
						break;
					}
					case '\"':
					case '\'': {
						state = this.requireValueState(i, state, true);
						final int strEnd = endOfQuotedString(this.jsonish, i);
						this.sanitizeString(i, strEnd);
						i = strEnd - 1;
						break;
					}
					case '(':
					case ')': {
						this.elide(i, i + 1);
						break;
					}
					case '[':
					case '{': {
						state = this.requireValueState(i, state, false);
						if (this.isMap == null) {
							this.isMap = new boolean[this.maximumNestingDepth];
						}
						final boolean map = ch == '{';
						this.isMap[this.bracketDepth] = map;
						++this.bracketDepth;
						state = (map ? State.START_MAP : State.START_ARRAY);
						break;
					}
					case ']':
					case '}': {
						if (this.bracketDepth == 0) {
							this.elide(i, this.jsonish.length());
							break Label_1192;
						}
						switch (state) {
						case BEFORE_VALUE: {
							this.insert(i, "null");
							break;
						}
						case BEFORE_ELEMENT:
						case BEFORE_KEY: {
							this.elideTrailingComma(i);
							break;
						}
						case AFTER_KEY: {
							this.insert(i, ":null");
							break;
						}
						default:
							break;
						}
						--this.bracketDepth;
						final char closeBracket = this.isMap[this.bracketDepth] ? '}' : ']';
						if (ch != closeBracket) {
							this.replace(i, i + 1, closeBracket);
						}
						state = ((this.bracketDepth == 0 || !this.isMap[this.bracketDepth - 1]) ? State.AFTER_ELEMENT : State.AFTER_VALUE);
						break;
					}
					case ',': {
						if (this.bracketDepth == 0) {
							throw JsonSanitizer.UNBRACKETED_COMMA;
						}
						switch (state) {
						case AFTER_ELEMENT: {
							state = State.BEFORE_ELEMENT;
							break;
						}
						case AFTER_VALUE: {
							state = State.BEFORE_KEY;
							break;
						}
						case BEFORE_ELEMENT:
						case START_ARRAY: {
							this.insert(i, "null");
							state = State.BEFORE_ELEMENT;
							break;
						}
						case BEFORE_KEY:
						case AFTER_KEY:
						case START_MAP: {
							this.elide(i, i + 1);
							break;
						}
						case BEFORE_VALUE: {
							this.insert(i, "null");
							state = State.BEFORE_KEY;
							break;
						}
						}
						break;
					}
					case ':': {
						if (state == State.AFTER_KEY) {
							state = State.BEFORE_VALUE;
							break;
						}
						this.elide(i, i + 1);
						break;
					}
					case '/': {
						int end = i + 1;
						if (i + 1 < n) {
							switch (this.jsonish.charAt(i + 1)) {
							case '/': {
								end = n;
								for (int j = i + 2; j < n; ++j) {
									final char cch = this.jsonish.charAt(j);
									if (cch == '\n' || cch == '\r' || cch == '\u2028' || cch == '\u2029') {
										end = j + 1;
										break;
									}
								}
								break;
							}
							case '*': {
								end = n;
								if (i + 3 < n) {
									int j = i + 2;
									while ((j = this.jsonish.indexOf(47, j + 1)) >= 0) {
										if (this.jsonish.charAt(j - 1) == '*') {
											end = j + 1;
											break;
										}
									}
									break;
								}
								break;
							}
							}
						}
						this.elide(i, end);
						i = end - 1;
						break;
					}
					default: {
						int runEnd;
						for (runEnd = i; runEnd < n; ++runEnd) {
							final char tch = this.jsonish.charAt(runEnd);
							if (('a' > tch || tch > 'z') && ('0' > tch || tch > '9') && tch != '+' && tch != '-' && tch != '.' && ('A' > tch || tch > 'Z') && tch != '_' && tch != '$') {
								break;
							}
						}
						if (runEnd == i) {
							this.elide(i, i + 1);
							break;
						}
						state = this.requireValueState(i, state, true);
						final boolean isNumber = ('0' <= ch && ch <= '9') || ch == '.' || ch == '+' || ch == '-';
						final boolean isKeyword = !isNumber && this.isKeyword(i, runEnd);
						if (!isNumber && !isKeyword) {
							while (runEnd < n && !this.isJsonSpecialChar(runEnd)) {
								++runEnd;
							}
							if (runEnd < n && this.jsonish.charAt(runEnd) == '\"') {
								++runEnd;
							}
						}
						if (state == State.AFTER_KEY) {
							this.insert(i, '\"');
							if (isNumber) {
								this.canonicalizeNumber(i, runEnd);
								this.insert(runEnd, '\"');
							}
							else {
								this.sanitizeString(i, runEnd);
							}
						}
						else if (isNumber) {
							this.normalizeNumber(i, runEnd);
						}
						else if (!isKeyword) {
							this.insert(i, '\"');
							this.sanitizeString(i, runEnd);
						}
						i = runEnd - 1;
						break;
					}
					}
				}
				catch (UnbracketedComma e) {
					this.elide(i, this.jsonish.length());
					break;
				}
			}
		if (state == State.START_ARRAY && this.bracketDepth == 0) {
			this.insert(n, "null");
			state = State.AFTER_ELEMENT;
		}
		if ((this.sanitizedJson != null && this.sanitizedJson.length() != 0) || this.cleaned != 0 || this.bracketDepth != 0) {
			if (this.sanitizedJson == null) {
				this.sanitizedJson = new StringBuilder(n + this.bracketDepth);
			}
			this.sanitizedJson.append(this.jsonish, this.cleaned, n);
			this.cleaned = n;
			switch (state) {
			case BEFORE_ELEMENT:
			case BEFORE_KEY: {
				this.elideTrailingComma(n);
				break;
			}
			case AFTER_KEY: {
				this.sanitizedJson.append(":null");
				break;
			}
			case BEFORE_VALUE: {
				this.sanitizedJson.append("null");
				break;
			}
			default:
			break;
			}
			while (this.bracketDepth != 0) {
				final StringBuilder sanitizedJson = this.sanitizedJson;
				final boolean[] isMap = this.isMap;
				final int bracketDepth = this.bracketDepth - 1;
				this.bracketDepth = bracketDepth;
				sanitizedJson.append((char)(isMap[bracketDepth] ? 125 : 93));
			}
		}
	}

	/**
	   * Ensures that the output corresponding to {@code jsonish[start:end]} is a
	   * valid JSON string that has the same meaning when parsed by Javascript
	   * {@code eval}.
	   * <ul>
	   *   <li>Making sure that it is fully quoted with double-quotes.
	   *   <li>Escaping any Javascript newlines : CR, LF, U+2028, U+2029
	   *   <li>Escaping HTML special characters to allow it to be safely embedded
	   *       in HTML {@code <script>} elements and XML {@code <!CDATA[...]]>}
	   *       sections.
	   *   <li>Rewrite hex, octal, and other escapes that are valid in Javascript
	   *       but not in JSON.
	   * </ul>
	   * @param start inclusive
	   * @param end   exclusive
	   */
	private void sanitizeString(final int start, final int end) {
		boolean closed = false;
		for (int i = start; i < end; ++i) {
			final char ch = this.jsonish.charAt(i);
			switch (ch) {
			case '\t': {
				this.replace(i, i + 1, "\\t");
				break;
			}
			case '\n': {
				this.replace(i, i + 1, "\\n");
				break;
			}
			case '\r': {
				this.replace(i, i + 1, "\\r");
				break;
			}
			case '\u2028': {
				this.replace(i, i + 1, "\\u2028");
				break;
			}
			case '\u2029': {
				this.replace(i, i + 1, "\\u2029");
				break;
			}
			case '\"':
			case '\'': {
				if (i == start) {
					if (ch == '\'') {
						this.replace(i, i + 1, '\"');
						break;
					}
					break;
				}
				else {
					if (i + 1 == end) {
						char startDelim = this.jsonish.charAt(start);
						if (startDelim != '\'') {
							startDelim = '\"';
						}
						closed = (startDelim == ch);
					}
					if (closed) {
						if (ch == '\'') {
							this.replace(i, i + 1, '\"');
							break;
						}
						break;
					}
					else {
						if (ch == '\"') {
							this.insert(i, '\\');
							break;
						}
						break;
					}
				}
			}
			case '<': {
				if (i + 3 >= end) {
					break;
				}
				int la = i + 1;
				final int c1AndDelta = unescapedChar(this.jsonish, la);
				final char c1 = (char)c1AndDelta;
				la += c1AndDelta >>> 16;
							final long c2AndDelta = unescapedChar(this.jsonish, la);
							final char c2 = (char)c2AndDelta;
							la += (int)(c2AndDelta >>> 16);
							final long c3AndEnd = unescapedChar(this.jsonish, la);
							final char c3 = (char)c3AndEnd;
							final char lc1 = (char)(c1 | ' ');
							final char lc2 = (char)(c2 | ' ');
							final char lc3 = (char)(c3 | ' ');
							if ((c1 == '!' && c2 == '-' && c3 == '-') || (lc1 == 's' && lc2 == 'c' && lc3 == 'r') || (c1 == '/' && lc2 == 's' && lc3 == 'c')) {
								this.replace(i, i + 1, "\\u003c");
								break;
							}
							break;
			}
			case '>': {
				if (i - 2 >= start) {
					int lb = i - 1;
					if ((runSlashPreceding(this.jsonish, lb) & 0x1) == 0x1) {
						--lb;
					}
					final int cm1AndDelta = unescapedCharRev(this.jsonish, lb);
					final char cm1 = (char)cm1AndDelta;
					if ('-' == cm1) {
						lb -= cm1AndDelta >>> 16;
						final int cm2AndDelta = unescapedCharRev(this.jsonish, lb);
						final char cm2 = (char)cm2AndDelta;
						if ('-' == cm2) {
							this.replace(i, i + 1, "\\u003e");
						}
					}
					break;
				}
				break;
			}
			case ']': {
				if (i + 2 < end) {
					int la = i + 1;
					final long c1AndDelta2 = unescapedChar(this.jsonish, la);
					final char c4 = (char)c1AndDelta2;
					la += (int)(c1AndDelta2 >>> 16);
					final long c2AndEnd = unescapedChar(this.jsonish, la);
					final char c5 = (char)c2AndEnd;
					if (']' == c4 && '>' == c5) {
						this.replace(i, i + 1, "\\u005d");
					}
					break;
				}
				break;
			}
			case '\\': {
				if (i + 1 == end) {
					this.elide(i, i + 1);
					break;
				}
				final char sch = this.jsonish.charAt(i + 1);
				switch (sch) {
				case '\"':
				case '/':
				case '\\':
				case 'b':
				case 'f':
				case 'n':
				case 'r':
				case 't': {
					++i;
					continue;
				}
				case 'v': {
					this.replace(i, i + 2, "\\u0008");
					++i;
					continue;
				}
				case 'x': {
					if (i + 4 < end && this.isHexAt(i + 2) && this.isHexAt(i + 3)) {
						this.replace(i, i + 2, "\\u00");
						i += 3;
						continue;
					}
					this.elide(i, i + 1);
					continue;
				}
				case 'u': {
					if (i + 6 < end && this.isHexAt(i + 2) && this.isHexAt(i + 3) && this.isHexAt(i + 4) && this.isHexAt(i + 5)) {
						i += 5;
						continue;
					}
					this.elide(i, i + 1);
					continue;
				}
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7': {
					int octalEnd;
					final int octalStart = octalEnd = i + 1;
					if (++octalEnd < end && this.isOctAt(octalEnd)) {
						++octalEnd;
						if (sch <= '3' && octalEnd < end && this.isOctAt(octalEnd)) {
							++octalEnd;
						}
					}
					int value = 0;
					for (int j = octalStart; j < octalEnd; ++j) {
						final char digit = this.jsonish.charAt(j);
						value = (value << 3 | digit - '0');
					}
					this.replace(octalStart, octalEnd, "u00");
					this.appendHex(value, 2);
					i = octalEnd - 1;
					continue;
				}
				default: {
					this.elide(i, i + 1);
					continue;
				}
				}
			}
			default: {
				if (ch >= ' ') {
					if (ch < '\ud800') {
						break;
					}
					if (ch < '\ue000') {
						if (Character.isHighSurrogate(ch) && i + 1 < end && Character.isLowSurrogate(this.jsonish.charAt(i + 1))) {
							++i;
							break;
						}
					}
					else if (ch <= '\ufffd') {
						break;
					}
				}
				this.replace(i, i + 1, "\\u");
				int k = 4;
				while (--k >= 0) {
					this.sanitizedJson.append(JsonSanitizer.HEX_DIGITS[ch >>> (k << 2) & 0xF]);
				}
				break;
			}
			}
		}
		if (!closed) {
			this.insert(end, '\"');
		}
	}

	private State requireValueState(final int pos, final State state, final boolean canBeKey) throws UnbracketedComma {
		switch (state) {
		case BEFORE_KEY:
		case START_MAP: {
			if (canBeKey) {
				return State.AFTER_KEY;
			}
			this.insert(pos, "\"\":");
			return State.AFTER_VALUE;
		}
		case AFTER_KEY: {
			this.insert(pos, ':');
			return State.AFTER_VALUE;
		}
		case BEFORE_VALUE: {
			return State.AFTER_VALUE;
		}
		case AFTER_VALUE: {
			if (canBeKey) {
				this.insert(pos, ',');
				return State.AFTER_KEY;
			}
			this.insert(pos, ",\"\":");
			return State.AFTER_VALUE;
		}
		case BEFORE_ELEMENT:
		case START_ARRAY: {
			return State.AFTER_ELEMENT;
		}
		case AFTER_ELEMENT: {
			if (this.bracketDepth == 0) {
				throw JsonSanitizer.UNBRACKETED_COMMA;
			}
			this.insert(pos, ',');
			return State.AFTER_ELEMENT;
		}
		default: {
			throw new AssertionError();
		}
		}
	}

	private void insert(final int pos, final char ch) {
		this.replace(pos, pos, ch);
	}

	private void insert(final int pos, final String s) {
		this.replace(pos, pos, s);
	}

	private void elide(final int start, final int end) {
		if (this.sanitizedJson == null) {
			this.sanitizedJson = new StringBuilder(this.jsonish.length() + 16);
		}
		this.sanitizedJson.append(this.jsonish, this.cleaned, start);
		this.cleaned = end;
	}

	private void replace(final int start, final int end, final char ch) {
		this.elide(start, end);
		this.sanitizedJson.append(ch);
	}

	private void replace(final int start, final int end, final String s) {
		this.elide(start, end);
		this.sanitizedJson.append(s);
	}

	private static int endOfQuotedString(final String s, final int start) {
		final char quote = s.charAt(start);
		int i = start;
		while ((i = s.indexOf(quote, i + 1)) >= 0) {
			int slashRunStart;
			for (slashRunStart = i; slashRunStart > start && s.charAt(slashRunStart - 1) == '\\'; --slashRunStart) {}
			if ((i - slashRunStart & 0x1) == 0x0) {
				return i + 1;
			}
		}
		return s.length();
	}

	private void elideTrailingComma(final int closeBracketPos) {
		int i = closeBracketPos;
		while (--i >= this.cleaned) {
			switch (this.jsonish.charAt(i)) {
			case '\t':
			case '\n':
			case '\r':
			case ' ': {
				continue;
			}
			case ',': {
				this.elide(i, i + 1);
				return;
			}
			default: {
				throw new AssertionError((Object)("" + this.jsonish.charAt(i)));
			}
			}
		}
		assert this.sanitizedJson != null;
		i = this.sanitizedJson.length();
		while (--i >= 0) {
			switch (this.sanitizedJson.charAt(i)) {
			case '\t':
			case '\n':
			case '\r':
			case ' ': {
				continue;
			}
			case ',': {
				this.sanitizedJson.setLength(i);
				return;
			}
			default: {
				throw new AssertionError((Object)("" + this.sanitizedJson.charAt(i)));
			}
			}
		}
		throw new AssertionError((Object)("Trailing comma not found in " + this.jsonish + " or " + (Object)this.sanitizedJson));
	}

	private void normalizeNumber(final int start, final int end) {
		int pos = start;
		if (pos < end) {
			switch (this.jsonish.charAt(pos)) {
			case '+': {
				this.elide(pos, pos + 1);
				++pos;
				break;
			}
			case '-': {
				++pos;
				break;
			}
			}
		}
		int intEnd = this.endOfDigitRun(pos, end);
		if (pos == intEnd) {
			this.insert(pos, '0');
		}
		else if ('0' == this.jsonish.charAt(pos)) {
			boolean reencoded = false;
			int maxDigVal = 0;
			int probableBase = 10;
			int firstDigitIndex = -1;
			if (intEnd - pos == 1 && intEnd < end && 0x78 == (this.jsonish.charAt(intEnd) | ' ')) {
				probableBase = 16;
				firstDigitIndex = intEnd + 1;
				for (++intEnd; intEnd < end; ++intEnd) {
					char ch = this.jsonish.charAt(intEnd);
					int digVal;
					if ('0' <= ch && ch <= '9') {
						digVal = ch - '0';
					}
					else {
						ch |= ' ';
						if ('a' > ch || ch > 'f') {
							break;
						}
						digVal = ch - 'W';
					}
					maxDigVal = Math.max(digVal, maxDigVal);
				}
				reencoded = true;
			}
			else if (intEnd - pos > 1) {
				probableBase = 8;
				firstDigitIndex = pos;
				for (int i = pos; i < intEnd; ++i) {
					final int digVal = this.jsonish.charAt(i) - '0';
					if (digVal < 0) {
						break;
					}
					maxDigVal = Math.max(digVal, maxDigVal);
				}
				reencoded = true;
			}
			if (reencoded) {
				this.elide(pos, intEnd);
				final String digits = this.jsonish.substring(firstDigitIndex, intEnd);
				final int nDigits = digits.length();
				final int base = (probableBase > maxDigVal) ? probableBase : ((maxDigVal > 10) ? 16 : 10);
				if (nDigits == 0) {
					this.sanitizedJson.append('0');
				}
				else if (JsonSanitizer.DIGITS_BY_BASE_THAT_FIT_IN_63B[base] >= nDigits) {
					final long value = Long.parseLong(digits, base);
					this.sanitizedJson.append(value);
				}
				else {
					final BigInteger value2 = new BigInteger(digits, base);
					this.sanitizedJson.append(value2);
				}
			}
		}
		pos = intEnd;
		if (pos < end && this.jsonish.charAt(pos) == '.') {
			++pos;
			final int fractionEnd = this.endOfDigitRun(pos, end);
			if (fractionEnd == pos) {
				this.insert(pos, '0');
			}
			pos = fractionEnd;
		}
		if (pos < end && 0x65 == (this.jsonish.charAt(pos) | ' ')) {
			if (++pos < end) {
				switch (this.jsonish.charAt(pos)) {
				case '+':
				case '-': {
					++pos;
					break;
				}
				}
			}
			final int expEnd = this.endOfDigitRun(pos, end);
			if (expEnd == pos) {
				this.insert(pos, '0');
			}
			pos = expEnd;
		}
		if (pos != end) {
			this.elide(pos, end);
		}
	}

	private boolean canonicalizeNumber(final int start, final int end) {
		this.elide(start, start);
		final int sanStart = this.sanitizedJson.length();
		this.normalizeNumber(start, end);
		this.elide(end, end);
		final int sanEnd = this.sanitizedJson.length();
		return canonicalizeNumber(this.sanitizedJson, sanStart, sanEnd);
	}

	private static boolean canonicalizeNumber(final StringBuilder sanitizedJson, final int sanStart, final int sanEnd) {
		int intEnd;
		int intStart;
		for (intStart = (intEnd = sanStart + ((sanitizedJson.charAt(sanStart) == '-') ? 1 : 0)); intEnd < sanEnd; ++intEnd) {
			final char ch = sanitizedJson.charAt(intEnd);
			if ('0' > ch) {
				break;
			}
			if (ch > '9') {
				break;
			}
		}
		int fractionStart;
		int fractionEnd;
		if (intEnd == sanEnd || '.' != sanitizedJson.charAt(intEnd)) {
			fractionEnd = (fractionStart = intEnd);
		}
		else {
			for (fractionStart = (fractionEnd = intEnd + 1); fractionEnd < sanEnd; ++fractionEnd) {
				final char ch = sanitizedJson.charAt(fractionEnd);
				if ('0' > ch) {
					break;
				}
				if (ch > '9') {
					break;
				}
			}
		}
		int expEnd;
		int expStart;
		if (fractionEnd == sanEnd) {
			expEnd = sanEnd;
			expStart = sanEnd;
		}
		else {
			assert 0x65 == (sanitizedJson.charAt(fractionEnd) | ' ');
			expStart = fractionEnd + 1;
			if (sanitizedJson.charAt(expStart) == '+') {
				++expStart;
			}
			expEnd = sanEnd;
		}
		assert intStart <= intEnd && intEnd <= fractionStart && fractionStart <= fractionEnd && fractionEnd <= expStart && expStart <= expEnd;
		int exp;
		if (expEnd == expStart) {
			exp = 0;
		}
		else {
			try {
				exp = Integer.parseInt(sanitizedJson.substring(expStart, expEnd), 10);
			}
			catch (NumberFormatException ex) {
				return false;
			}
		}
		int n = exp;
		boolean sawDecimal = false;
		boolean zero = true;
		int digitOutPos = intStart;
		int i = intStart;
		int nZeroesPending = 0;
		while (i < fractionEnd) {
			final char ch2 = sanitizedJson.charAt(i);
			if (ch2 == '.') {
				sawDecimal = true;
				if (zero) {
					nZeroesPending = 0;
				}
			}
			else {
				char digit = ch2;
				if ((!zero || digit != '0') && !sawDecimal) {
					++n;
				}
				if (digit == '0') {
					++nZeroesPending;
				}
				else {
					if (zero) {
						if (sawDecimal) {
							n -= nZeroesPending;
						}
						nZeroesPending = 0;
					}
					zero = false;
					while (nZeroesPending != 0 || digit != '\0') {
						char vdigit;
						if (nZeroesPending == 0) {
							vdigit = digit;
							digit = '\0';
						}
						else {
							vdigit = '0';
							--nZeroesPending;
						}
						sanitizedJson.setCharAt(digitOutPos++, vdigit);
					}
				}
			}
			++i;
		}
		sanitizedJson.setLength(digitOutPos);
		final int k = digitOutPos - intStart;
		if (zero) {
			sanitizedJson.setLength(sanStart);
			sanitizedJson.append('0');
			return true;
		}
		if (k <= n && n <= 21) {
			for (int j = k; j < n; ++j) {
				sanitizedJson.append('0');
			}
		}
		else if (0 < n && n <= 21) {
			sanitizedJson.insert(intStart + n, '.');
		}
		else if (-6 < n && n <= 0) {
			sanitizedJson.insert(intStart, "0.000000".substring(0, 2 - n));
		}
		else {
			if (k != 1) {
				sanitizedJson.insert(intStart + 1, '.');
			}
			final int nLess1 = n - 1;
			sanitizedJson.append('e').append((nLess1 < 0) ? '-' : '+').append(Math.abs(nLess1));
		}
		return true;
	}

	private boolean isKeyword(final int start, final int end) {
		final int n = end - start;
		if (n == 5) {
			return "false".regionMatches(0, this.jsonish, start, n);
		}
		return n == 4 && ("null".regionMatches(0, this.jsonish, start, n) || "true".regionMatches(0, this.jsonish, start, n));
	}

	private boolean isOctAt(final int i) {
		return isOct(this.jsonish.charAt(i));
	}

	private static boolean isOct(final char ch) {
		return '0' <= ch && ch <= '7';
	}

	private boolean isHexAt(final int i) {
		return isHex(this.jsonish.charAt(i));
	}

	private static boolean isHex(final char ch) {
		if ('0' <= ch && ch <= '9') {
			return true;
		}
		final int lch = ch | ' ';
		return 97 <= lch && lch <= 102;
	}

	private static int hexVal(final char ch) {
		final int lch = ch | ' ';
		return lch - ((lch <= 57) ? 48 : 87);
	}

	private boolean isJsonSpecialChar(final int i) {
		final char ch = this.jsonish.charAt(i);
		if (ch <= ' ') {
			return true;
		}
		switch (ch) {
		case '\"':
		case ',':
		case ':':
		case '[':
		case ']':
		case '{':
		case '}': {
			return true;
		}
		default: {
			return false;
		}
		}
	}

	private void appendHex(final int n, final int nDigits) {
		int quadsToShift = nDigits;
		while (--quadsToShift >= 0) {
			final int dig = n >>> 4 * quadsToShift & 0xF;
		this.sanitizedJson.append((char)(dig + ((dig < 10) ? 48 : 87)));
		}
	}

	private int endOfDigitRun(final int start, final int limit) {
		for (int end = start; end < limit; ++end) {
			final char ch = this.jsonish.charAt(end);
			if ('0' > ch || ch > '9') {
				return end;
			}
		}
		return limit;
	}

	CharSequence toCharSequence() {
		return (CharSequence)((this.sanitizedJson != null) ? this.sanitizedJson : this.jsonish);
	}

	@Override
	public String toString() {
		return (this.sanitizedJson != null) ? this.sanitizedJson.toString() : this.jsonish;
	}

	private static int unescapedChar(final String s, final int left) {
		final int n = s.length();
		if (left >= n) {
			return 0;
		}
		final char c = s.charAt(left);
		if (c != '\\') {
			return 0x10000 | c;
		}
		if (left + 1 == n) {
			return 65536;
		}
		final char nc = s.charAt(left + 1);
		switch (nc) {
		case '0':
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7': {
			int octalEnd;
			final int octalStart = octalEnd = left + 1;
			if (++octalEnd < n && isOct(s.charAt(octalEnd))) {
				++octalEnd;
				if (nc <= '3' && octalEnd < n && isOct(s.charAt(octalEnd))) {
					++octalEnd;
				}
			}
			int value = 0;
			for (int j = octalStart; j < octalEnd; ++j) {
				final char digit = s.charAt(j);
				value = (value << 3 | digit - '0');
			}
			return octalEnd - left << 16 | value;
		}
		case 'x': {
			if (left + 3 >= n) {
				break;
			}
			final char d0 = s.charAt(left + 2);
			final char d2 = s.charAt(left + 3);
			if (isHex(d0) && isHex(d2)) {
				return 0x40000 | hexVal(d0) << 4 | hexVal(d2);
			}
			break;
		}
		case 'u': {
			if (left + 5 >= n) {
				break;
			}
			final char d0 = s.charAt(left + 2);
			final char d2 = s.charAt(left + 3);
			final char d3 = s.charAt(left + 4);
			final char d4 = s.charAt(left + 5);
			if (isHex(d0) && isHex(d2) && isHex(d3) && isHex(d4)) {
				return 0x60000 | hexVal(d0) << 12 | hexVal(d2) << 8 | hexVal(d3) << 4 | hexVal(d4);
			}
			break;
		}
		case 'b': {
			return 131080;
		}
		case 'f': {
			return 131084;
		}
		case 'n': {
			return 131082;
		}
		case 'r': {
			return 131085;
		}
		case 't': {
			return 131081;
		}
		case 'v': {
			return 131080;
		}
		}
		return 0x20000 | nc;
	}

	private static int unescapedCharRev(final String s, final int rightIncl) {
		if (rightIncl < 0) {
			return 0;
		}
		int i = 1;
		while (i < 6) {
			final int left = rightIncl - i;
			if (left < 0) {
				break;
			}
			if (s.charAt(left) == '\\') {
				int n;
				for (n = 1; left - n >= 0 && s.charAt(left - n) == '\\'; ++n) {}
				if ((n & 0x1) != 0x1) {
					break;
				}
				final int unescaped = unescapedChar(s, left);
				if ((unescaped >>> 16) - 1 == i) {
					return unescaped;
				}
				break;
			}
			else {
				++i;
			}
		}
		return 0x10000 | s.charAt(rightIncl);
	}

	private static int runSlashPreceding(final String jsonish, final int pos) {
		int startOfRun;
		for (startOfRun = pos; startOfRun >= 0 && jsonish.charAt(startOfRun) == '\\'; --startOfRun) {}
		return pos - startOfRun;
	}

	static {
		(UNBRACKETED_COMMA = new UnbracketedComma()).setStackTrace(new StackTraceElement[0]);
		HEX_DIGITS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
		DIGITS_BY_BASE_THAT_FIT_IN_63B = new int[] { -1, -1, 63, 39, 31, 27, 24, 22, 21, 19, 18, 18, 17, 17, 16, 16, 15 };
	}

	private enum State
	{
		START_ARRAY, 
		BEFORE_ELEMENT, 
		AFTER_ELEMENT, 
		START_MAP, 
		BEFORE_KEY, 
		AFTER_KEY, 
		BEFORE_VALUE, 
		AFTER_VALUE;
	}

	private static final class UnbracketedComma extends Exception
	{
		private static final long serialVersionUID = 783239978717247850L;
	}
}
