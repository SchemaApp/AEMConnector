package com.schema.core.models;

import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;

public final class WebhookEntityResult {

	public static final String TYPE_CONSTANT = "type";
	public static final String ID_CONSTANT = "id";
	public static final String ERROR_MESSAGE = "error";

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

	public Map<String, String> toMap(final Boolean displayable) {
		return new ImmutableMap.Builder<String, String>()
				.put(ID_CONSTANT, id)
				.put(TYPE_CONSTANT, type)
				.build();
	}

	@Override
	public boolean equals(final Object result) {
		return EqualsBuilder.reflectionEquals(this, result);
	}

	@Override
	public int hashCode() {
		return HashCodeBuilder.reflectionHashCode(this);
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
