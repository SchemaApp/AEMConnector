package com.schemaapp.core.services;

import static com.schemaapp.core.util.JsonSanitizer.DEFAULT_NESTING_DEPTH;
import static com.schemaapp.core.util.JsonSanitizer.sanitize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.schemaapp.core.util.JsonSanitizer;

@ExtendWith({ MockitoExtension.class})
class JsonSanitizerTest {

	private static void assertSanitized(String golden, String input) {
		assertSanitized(golden, input, DEFAULT_NESTING_DEPTH);
	}

	private static void assertSanitized(String golden, String input, int maximumNestingDepth) {
		String actual = sanitize(input, maximumNestingDepth);
		assertEquals(input, golden, actual);
		if (actual.equals(input)) {
			assertSame(input, input, actual);
		}
	}

	private static void assertSanitized(String sanitary) {
		assertSanitized(sanitary, sanitary);
	}

	void testSanitize() {
		// On the left is the sanitized output, and on the right the input.
		// If there is a single string, then the input is fine as-is.
		assertSanitized(null, null);
		assertSanitized("null");
		assertSanitized("false");
		assertSanitized("true");
		assertSanitized(" false ");
		assertSanitized("  false");
		assertSanitized("false\n");
		assertSanitized("\"foo\"");
		// \u0130 is a Turkish dotted upper-case 'I' so the lower case version of
		// the tag name is "sc</ScRİpTipt".
		assertSanitized("\"<b>Hello</b>\"");
		assertSanitized("\"<s>Hello</s>\"");
		assertSanitized("[[0]]", "[[0]]>");
		assertSanitized("[1,-1,0.0,-0.5,1e2]", "[1,-1,0.0,-0.5,1e2,");
		assertSanitized("[1,2,3]", "[1,2,3,]");
		assertSanitized("[1,null,3]", "[1,,3,]");
		assertSanitized("[1 ,2 ,3]", "[1 2 3]");
		assertSanitized("{ \"foo\": \"bar\" }");
		assertSanitized("{ \"foo\": \"bar\" }", "{ \"foo\": \"bar\", }");
		assertSanitized("{\"foo\":\"bar\"}", "{\"foo\",\"bar\"}");
		assertSanitized("{ \"foo\": \"bar\" }", "{ foo: \"bar\" }");
		assertSanitized("{ \"foo\": \"bar\"}", "{ foo: 'bar");
		assertSanitized("{ \"foo\": [\"bar\"]}", "{ foo: ['bar");
		assertSanitized("false", "// comment\nfalse");
		assertSanitized("false", "false// comment");
		assertSanitized("false", "false// comment\n");
		assertSanitized("false", "false/* comment */");
		assertSanitized("false", "false/* comment *");
		assertSanitized("false", "false/* comment ");
		assertSanitized("false", "/*/true**/false");
		assertSanitized("1");
		assertSanitized("-1");
		assertSanitized("1.0");
		assertSanitized("-1.0");
		assertSanitized("1.05");
		assertSanitized("427.0953333");
		assertSanitized("6.0221412927e+23");
		assertSanitized("6.0221412927e23");
		assertSanitized("6.0221412927e0", "6.0221412927e");
		assertSanitized("6.0221412927e-0", "6.0221412927e-");
		assertSanitized("6.0221412927e+0", "6.0221412927e+");
		assertSanitized("1.660538920287695E-24");
		assertSanitized("-6.02e-23");
		assertSanitized("1.0", "1.");
		assertSanitized("0.5", ".5");
		assertSanitized("-0.5", "-.5");
		assertSanitized("0.5", "+.5");
		assertSanitized("0.5e2", "+.5e2");
		assertSanitized("1.5e+2", "+1.5e+2");
		assertSanitized("0.5e-2", "+.5e-2");
		assertSanitized("{\"0\":0}", "{0:0}");
		assertSanitized("{\"0\":0}", "{-0:0}");
		assertSanitized("{\"0\":0}", "{+0:0}");
		assertSanitized("{\"1\":0}", "{1.0:0}");
		assertSanitized("{\"1\":0}", "{1.:0}");
		assertSanitized("{\"0.5\":0}", "{.5:0}");
		assertSanitized("{\"-0.5\":0}", "{-.5:0}");
		assertSanitized("{\"0.5\":0}", "{+.5:0}");
		assertSanitized("{\"50\":0}", "{+.5e2:0}");
		assertSanitized("{\"150\":0}", "{+1.5e+2:0}");
		assertSanitized("{\"0.1\":0}", "{+.1:0}");
		assertSanitized("{\"0.01\":0}", "{+.01:0}");
		assertSanitized("{\"0.005\":0}", "{+.5e-2:0}");
		assertSanitized("{\"1e+101\":0}", "{10e100:0}");
		assertSanitized("{\"1e-99\":0}", "{10e-100:0}");
		assertSanitized("{\"1.05e-99\":0}", "{10.5e-100:0}");
		assertSanitized("{\"1.05e-99\":0}", "{10.500e-100:0}");
		assertSanitized("{\"1.234e+101\":0}", "{12.34e100:0}");
		assertSanitized("{\"1.234e-102\":0}", "{.01234e-100:0}");
		assertSanitized("{\"1.234e-102\":0}", "{.01234e-100:0}");
		assertSanitized("{}");
		// Remove grouping parentheses.
		assertSanitized("{}", "({})");
		// Escape code-points and isolated surrogates which are not XML embeddable.
		assertSanitized("\"\\u0000\\u0008\\u001f\"", "'\u0000\u0008\u001f'");
		assertSanitized("\"\ud800\udc00\\udc00\\ud800\"",
				"'\ud800\udc00\udc00\ud800'");
		assertSanitized("\"\ufffd\\ufffe\\uffff\"", "'\ufffd\ufffe\uffff'");
		// These control characters should be elided if they appear outside a string
		// literal.
		assertSanitized("42", "\uffef\u000042\u0008\ud800\uffff\udc00");
		assertSanitized("null", "\uffef\u0000\u0008\ud800\uffff\udc00");
		assertSanitized("[null]", "[,]");
		assertSanitized("[null]", "[null,]");
		assertSanitized("{\"a\":0,\"false\":\"x\",\"\":{\"\":-1}}",
				"{\"a\":0,false\"x\":{\"\":-1}}");
		assertSanitized("[true ,false]", "[true false]");
		assertSanitized("[\"\\u00a0\\u1234\"]");
		assertSanitized("{\"a\\b\":\"c\"}", "{a\\b\"c");
		assertSanitized("{\"a\":\"b\",\"c\":null}", "{\"a\":\"b\",\"c\":");
		assertSanitized(
				"{\"1e0001234567890123456789123456789123456789\":0}",
				// Exponent way out of representable range in a JS double.
				"{1e0001234567890123456789123456789123456789:0}"
				);
		// Our octal recoder interprets an octal-like literal that includes a digit '8' or '9' as
		// decimal.
		assertSanitized("-16923547559", "-016923547559");
	}


	@Test
	void testMaximumNestingLevel() {
		String nestedMaps = "{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{{}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}";
		String sanitizedNestedMaps = "{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}}";

		boolean exceptionIfTooMuchNesting = false;
		try {
			assertSanitized(sanitizedNestedMaps, nestedMaps, DEFAULT_NESTING_DEPTH);
		} catch (ArrayIndexOutOfBoundsException e) {
			Logger.getAnonymousLogger().log(Level.FINEST, "Expected exception in testing maximum nesting level", e);
			exceptionIfTooMuchNesting = true;
		}
		//assertTrue("Expecting failure for too nested JSON", exceptionIfTooMuchNesting);
	}

	@Test
	void testMaximumNestingLevelAssignment() {
		assertEquals(1, new JsonSanitizer("", Integer.MIN_VALUE).getMaximumNestingDepth());
		assertEquals(JsonSanitizer.MAXIMUM_NESTING_DEPTH, new JsonSanitizer("", Integer.MAX_VALUE).getMaximumNestingDepth());
	}

	@Test
	void testIssue13() {
		assertSanitized(
				"[ { \"description\": \"aa##############aa\" }, 1 ]",
				"[ { \"description\": \"aa##############aa\" }, 1 ]");
	}


	@Test
	void testLongNumberInUnclosedInputWithU80() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		assertEquals(
				"{\"\":{\"\":{\"\":{\"\":{\"\":{\"\":{\"x80\":{\"\":{\"\":[-400557869725698078427]}}}}}}}}}",
				JsonSanitizer.sanitize("{{{{{{{\\x80{{([-053333333304233333333333")
				);
	}

	@Test
	void testSlashFour() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		assertEquals("\"y\\u0004\"", JsonSanitizer.sanitize("y\\4")); // "y\4"
	}

	@Test
	void testUnterminatedObject() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		String input = "?\u0000\u0000\u0000{{\u0000\ufffd\u0003]ve{R]\u00000\ufffd\u0016&e{\u0003]\ufffda<!.b<!<!cc1x\u0000\u00005{281<\u0000.{t\u0001\ufffd5\ufffd{5\ufffd\ufffd0\ufffd15\r\ufffd\u0000\u0000\u0000~~-0081273222428822883223759,55\ufffd\u0000\ufffd\t\u0000\ufffd";
		String got = JsonSanitizer.sanitize(input);
		String want = "{\"\":{},\"ve\":{\"R\":null},\"0\":\"e\",\"\":{},\"a<!.b<!<!cc1x\":5,\"\":{\"281\":0.0,\"\":{\"t\":5,\"\":{\"5\":0,\"15\"\r:-81273222428822883223759,\"55\"\t:null}}}}";
		assertEquals(want, got);
	}

	@Test
	void testCrash1() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		String input = "?\u0000\u0000\u0000{{\u0000\ufffd\u0003]ve{R]\u00000\ufffd\ufffd\u0016&e{\u0003]\ufffda<!.b<!<!c\u00005{281<\u0000.{t\u0001\ufffd5\ufffd{515\r[\u0000\u0000\u0000~~-008127322242\ufffd\ufffd\ufffd\ufffd\ufffd\ufffd\ufffd\ufffd23759,551x\u0000\u00006{281<\u0000.{t\u0001\ufffd5\ufffd{5\ufffd\ufffd0\ufffd15\r[\u0000\u0000\u0000~~-0081273222428822883223759,\ufffd";
		String want = "{\"\":{},\"ve\":{\"R\":null},\"0\":\"e\",\"\":{},\"a<!.b<!<!c\":5,\"\":{\"281\":0.0,\"\":{\"t\":5,\"\":{\"515\"\r:[-8127322242,23759,551,6,{\"281\":0.0,\"\":{\"t\":5,\"\":{\"5\":0,\"15\"\r:[-81273222428822883223759]}}}]}}}}";
		String got = JsonSanitizer.sanitize(input);
		assertEquals(want, got);
	}

	@Test
	void testDisallowedSubstrings() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		String[] inputs = {
				"x<\\script>",
				"x</\\script>",
				"x</sc\\ript>",
				"x<\\163cript>",
				"x</\\163cript>",
				"x<\\123cript>",
				"x</\\123cript>",
				"u\\u\\uu\ufffd\ufffd\\u7u\\u\\u\\u\ufffdu<\\script>5",
				"z\\<\\!--",
				"z\\<!\\--",
				"z\\<!-\\-",
				"z\\<\\!--",
				"\"\\]]\\>",
		};
		for (String input: inputs) {
			String out = JsonSanitizer.sanitize(input).toLowerCase(Locale.ROOT);
//			assertFalse(out, out.contains("<!--"));
//			assertFalse(out, out.contains("-->"));
//			assertFalse(out, out.contains("<script"));
//			assertFalse(out, out.contains("</script"));
//			assertFalse(out, out.contains("]]>"));
//			assertFalse(out, out.contains("<![cdata["));
		}
	}

	@Test
	void testXssPayload() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		String input = "x</\\script>u\\u\\uu\ufffd\ufffd\\u7u\\u\\u\\u\ufffdu<\\script>5+alert(1)//";
		assertEquals(
				"\"x\\u003c/script>uuuu\uFFFD\uFFFDu7uuuu\uFFFDu\\u003cscript>5+alert(1)//\"",
				JsonSanitizer.sanitize(input)
				);
	}

	@Test
	void testInvalidOutput() {
		// Found by Fabian Meumertzheim using CI Fuzz (https://www.code-intelligence.com)
		String input = "\u0010{'\u0000\u0000'\"\u0000\"{.\ufffd-0X29295909049550970,\n\n0";
		String want = "{\"\\u0000\\u0000\":\"\\u0000\",\"\":{\"0\":-47455995597866469744,\n\n\"0\":null}}";
		String got = JsonSanitizer.sanitize(input);
		assertEquals(want, got);
	}

	@Test
	void testBadNumber() {
		String input = "¶0x.\\蹃4\\À906";
		String want = "0.0";
		String got = JsonSanitizer.sanitize(input);
		assertEquals(want, got);
	}

	@Test
	void testDashDashGtEscaped() {
		String input = "'->??-\\->";
		String want = "\"->??--\\u003e\"";
		String got1 = JsonSanitizer.sanitize(input);
		assertEquals(want, got1);
		String got2 = JsonSanitizer.sanitize(got1);
		assertEquals(want, got2);
	}

	@Test
	void testDashDashGtUEscaped() {
		String input = "'.\\u002D->'";
		String want = "\".\\u002D-\\u003e\"";
		String got1 = JsonSanitizer.sanitize(input);
		assertEquals(want, got1);
		String got2 = JsonSanitizer.sanitize(got1);
		assertEquals(want, got2);
	}

	@Test
	void testEscHtmlCommentClose() {
		String input = "x--\\>";
		String want = "\"x--\\u003e\"";
		String got1 = JsonSanitizer.sanitize(input);
		assertEquals(want, got1);
		String got2 = JsonSanitizer.sanitize(got1);
		assertEquals(want, got2);
	}
}
