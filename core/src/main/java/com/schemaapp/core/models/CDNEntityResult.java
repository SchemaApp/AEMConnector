package com.schemaapp.core.models;

/**
 * The <code>WebhookEntityResult</code> class to prepare Webhook API response.
 * 
 * @author nikhil
 *
 */
public class CDNEntityResult {

	public static CDNEntityResult prepareSucessResponse(CDNEntity entiry) {
		return new WebhookEntitySucessResult(entiry.getId(), entiry.getType());
	}

	public static CDNEntityResult prepareError(String errorMessage) {
		return new WebhookEntityErrorResult(errorMessage);
	}
	
}

class WebhookEntitySucessResult extends CDNEntityResult {

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
		return "WebhookEntitySucessResult{"
				+ ID_CONSTANT+"="+id+", "
				+ TYPE_CONSTANT+"="+type+", "
				+ SUCCESS_MESSAGE+"=Successfully, request completed!}";
	}
}

class WebhookEntityErrorResult extends CDNEntityResult {

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
		return "WebhookEntityErrorResult{"+ERROR_MESSAGE+"="+errorMessage+"}";
	}
}
