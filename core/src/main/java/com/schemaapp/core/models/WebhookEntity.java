package com.schemaapp.core.models;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEntity {

	@JsonProperty(value="@context", required = true)
	private Context context;

	@JsonProperty(value="@type", required = true)
	private String type;

	@JsonProperty(value="@id", required = true)
	private String id;

	@JsonProperty(value="base64encode", required = true)
	private String base64encode;

	@JsonProperty("url")
	private String url;

	@JsonProperty(value="generatedAtTime", required = true)
	private Date generatedAtTime = new Date();

	@JsonProperty(value="@graph", required = true)
	private Object graph;

	@JsonCreator
	public WebhookEntity(@JsonProperty(value="@context", required = true) Context context, 
			@JsonProperty(value="@type", required = true) String type,
			@JsonProperty(value="@id", required = true) String id,
			@JsonProperty(value="base64encode", required = true) String base64encode,
			@JsonProperty(value="generatedAtTime", required = true) @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") Date generatedAtTime,
			@JsonProperty(value="@graph", required = true) Object graph) {
		this.context = context;
		this.type = type;
		this.id = id;
		this.base64encode = base64encode;
		this.generatedAtTime = (Date) generatedAtTime.clone();
		this.graph = graph;
	}
	
	public Context getContext() {
		return context;
	}
	
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

	public Date getGeneratedAtTime() {
		return generatedAtTime;
	}

	public Object getGraph() {
		return graph;
	}

}
