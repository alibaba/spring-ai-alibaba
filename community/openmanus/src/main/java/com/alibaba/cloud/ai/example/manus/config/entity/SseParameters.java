package com.alibaba.cloud.ai.example.manus.config.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class SseParameters {

	@JsonProperty("base_uri")
	private String baseUri;

	@JsonProperty("headers")
	private Map<String, String> headers;

	@JsonProperty("uri_variables")
	private Map<String, String> uriVariables;

	public String getBaseUri() {
		return baseUri;
	}

	public SseParameters setBaseUri(String baseUri) {
		this.baseUri = baseUri;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public SseParameters setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public Map<String, String> getUriVariables() {
		return uriVariables;
	}

	public SseParameters setUriVariables(Map<String, String> uriVariables) {
		this.uriVariables = uriVariables;
		return this;
	}

}
