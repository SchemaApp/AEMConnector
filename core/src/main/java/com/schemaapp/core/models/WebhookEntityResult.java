package com.schemaapp.core.models;

import com.google.common.base.Objects;

/**
 * The <code>WebhookEntityResult</code> class to prepare Webhook API response.
 * 
 * @author nikhil
 *
 */
public class WebhookEntityResult {

	public static WebhookEntityResult prepareSucessResponse(WebhookEntity entiry) {
		return new WebhookEntitySucessResult(entiry.getId(), entiry.getType());
	}

	public static WebhookEntityResult prepareError(String errorMessage) {
		return new WebhookEntityErrorResult(errorMessage);
	}
	
}

class WebhookEntitySucessResult extends WebhookEntityResult {

	private static final String TYPE_CONSTANT = "type";
	private static final String ID_CONSTANT = "id";
	private static final String SUCCESS_MESSAGE = "message";

	private String id;

	private String type;

	private String message;

	public WebhookEntitySucessResult(String id, String type) {
		this.id = id;
		this.type = type;
	}

	public String getId() {
		return id;
	}

	public String getType() {
		return type;
	}

	public String getError() {
		return message;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add(ID_CONSTANT, id)
				.add(TYPE_CONSTANT, type)
				.add(SUCCESS_MESSAGE, "Successfully, request completed!")
				.toString();
	}
}

class WebhookEntityErrorResult extends WebhookEntityResult {

	private static final String ERROR_MESSAGE = "errorMessage";

	public WebhookEntityErrorResult(String message) {
		this.errorMessage = message;
	}
	private String errorMessage;

	public String getErrorMessage() {
		return errorMessage;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add(ERROR_MESSAGE, errorMessage)
				.toString();
	}
}