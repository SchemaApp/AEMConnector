package com.schemaapp.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEntity {

	@JsonProperty("@type")
	private String type;

	@JsonProperty("@id")
	private String id;

	@JsonProperty("base64encode")
	private String base64encode;

	@JsonProperty("url")
	private String url;

	@JsonProperty("generatedAtTime")
	private String generatedAtTime;

	@JsonProperty("@graph")
	private Object graph;

	public String getType() {
		return type;
	}

	public String getId() {
		return id;
	}

	public String getBase64encode() {
		return base64encode;
	}

	public String getUrl() {
		return url;
	}

	public String getGeneratedAtTime() {
		return generatedAtTime;
	}

	public Object getGraph() {
		return graph;
	}

}
