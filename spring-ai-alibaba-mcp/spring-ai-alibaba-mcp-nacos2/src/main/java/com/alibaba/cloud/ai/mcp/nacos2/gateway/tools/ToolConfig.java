/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.mcp.nacos2.gateway.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ToolConfig {

	private String name;

	private String description;

	private List<ArgumentDefinition> args;

	private RequestTemplate requestTemplate;

	private ResponseTemplate responseTemplate;

	// Getters and setters

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public List<ArgumentDefinition> getArgs() {
		return args;
	}

	public void setArgs(final List<ArgumentDefinition> args) {
		this.args = args;
	}

	public RequestTemplate getRequestTemplate() {
		return requestTemplate;
	}

	public void setRequestTemplate(final RequestTemplate requestTemplate) {
		this.requestTemplate = requestTemplate;
	}

	public ResponseTemplate getResponseTemplate() {
		return responseTemplate;
	}

	public void setResponseTemplate(final ResponseTemplate responseTemplate) {
		this.responseTemplate = responseTemplate;
	}

	public static class ArgumentDefinition {

		private String name;

		private String description;

		private String type = "string";

		private boolean required = false;

		private Object defaultValue;

		private List<Object> enumValues;

		private Object items;

		private Map<String, Object> properties;

		private String position;

		// Getters and setters

		public String getName() {
			return name;
		}

		public void setName(final String name) {
			this.name = name;
		}

		public String getDescription() {
			return description;
		}

		public void setDescription(final String description) {
			this.description = description;
		}

		public String getType() {
			return type;
		}

		public void setType(final String type) {
			this.type = type;
		}

		public boolean isRequired() {
			return required;
		}

		public void setRequired(final boolean required) {
			this.required = required;
		}

		public Object getDefaultValue() {
			return defaultValue;
		}

		public void setDefaultValue(final Object defaultValue) {
			this.defaultValue = defaultValue;
		}

		public List<Object> getEnumValues() {
			return enumValues;
		}

		public void setEnumValues(final List<Object> enumValues) {
			this.enumValues = enumValues;
		}

		public Object getItems() {
			return items;
		}

		public void setItems(final Object items) {
			this.items = items;
		}

		public Map<String, Object> getProperties() {
			return properties;
		}

		public void setProperties(final Map<String, Object> properties) {
			this.properties = properties;
		}

		public String getPosition() {
			return position;
		}

		public void setPosition(final String position) {
			this.position = position;
		}

	}

	public static class RequestTemplate {

		private String url;

		private String method;

		private List<Header> headers;

		private String body;

		@JsonProperty("argsToJsonBody")
		private boolean argsToJsonBody = false;

		@JsonProperty("argsToUrlParam")
		private boolean argsToUrlParam = false;

		@JsonProperty("argsToFormBody")
		private boolean argsToFormBody = false;

		// Getters and setters

	}

	public static class Header {

		private String key;

		private String value;

		// Getters and setters

		public String getKey() {
			return key;
		}

		public void setKey(final String key) {
			this.key = key;
		}

		public String getValue() {
			return value;
		}

		public void setValue(final String value) {
			this.value = value;
		}

	}

	public static class ResponseTemplate {

		private String body;

		private String prependBody;

		private String appendBody;

		// Getters and setters

		public String getBody() {
			return body;
		}

		public void setBody(final String body) {
			this.body = body;
		}

		public String getPrependBody() {
			return prependBody;
		}

		public void setPrependBody(final String prependBody) {
			this.prependBody = prependBody;
		}

		public String getAppendBody() {
			return appendBody;
		}

		public void setAppendBody(final String appendBody) {
			this.appendBody = appendBody;
		}

	}

}
