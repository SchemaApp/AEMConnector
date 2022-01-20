package com.schemaapp.core.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class Context {

	@JsonProperty(value="@vocab", required=true)
	private String vocab;
	
	@JsonProperty(value="generatedAtTime", required=true)
	private String generatedAtTime;
	
	public Context(@JsonProperty(value="@vocab", required=true) String vocab,
			@JsonProperty(value="generatedAtTime", required=true) String generatedAtTime) {
		this.vocab = vocab;
		this.generatedAtTime = generatedAtTime;
	}
	
	public String getVocab() {
		return vocab;
	}

	public String getGeneratedAtTime() {
		return generatedAtTime;
	}

}
