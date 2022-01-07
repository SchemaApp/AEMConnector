package com.schemaapp.core.models;

import com.google.common.base.Objects;

/**
 * The <code>WebhookEntityResult</code> class to prepare Webhook API response.
 * 
 * @author nikhil
 *
 */
public final class WebhookEntityResult {

	private static final String TYPE_CONSTANT = "type";
	private static final String ID_CONSTANT = "id";
	private static final String ERROR_MESSAGE = "error";

	public static WebhookEntityResult fromEntity(WebhookEntity entiry) {
		return new WebhookEntityResult(entiry.getId(), entiry.getType());
	}
	
	public static WebhookEntityResult prepareError(String errorMessage) {
		return new WebhookEntityResult(errorMessage);
	}

	private String id;

	private String type;
	
	private String error;


	private WebhookEntityResult(final String errorMessage) {
		this.error = errorMessage;
	}
	
	private WebhookEntityResult(final String id, final String type) {
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
		return error;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add(ID_CONSTANT, id)
				.add(TYPE_CONSTANT, type)
				.add(ERROR_MESSAGE, error)
				.toString();
	}
}
