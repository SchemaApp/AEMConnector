package com.schemaapp.core.exception;

public class InvalidWebHookJsonDataException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidWebHookJsonDataException() {
		super();
	}

	public InvalidWebHookJsonDataException(String message) {
		super(message);
	}
}
