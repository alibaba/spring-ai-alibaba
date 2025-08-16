/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.runtime.domain.plugin;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * Plugin model class for managing plugin configurations and metadata.
 *
 * @since 1.0.0.3
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plugin implements Serializable {

	/** Unique identifier for the plugin */
	@JsonProperty("plugin_id")
	private String pluginId;

	/** Name of the plugin */
	private String name;

	/** Description of the plugin's functionality */
	private String description;

	/** Plugin configuration settings */
	private PluginConfig config;

	/** Source of the plugin, defaults to "user" */
	private String source = "user";

	/** Additional metadata for plugin extension */
	private Map<String, Object> extension;

	/** Creation timestamp */
	@JsonProperty("gmt_create")
	private Date gmtCreate;

	/** Last modification timestamp */
	@JsonProperty("gmt_modified")
	private Date gmtModified;

	/** Configuration class for plugin settings */
	@Data
	public static class PluginConfig implements Serializable {

		/** Version of the configuration scheme */
		@JsonProperty("scheme_version")
		private String schemeVersion = "v1";

		/** Server URL for the plugin */
		private String server;

		/** HTTP headers for API requests */
		private Map<String, String> headers;

		/** Authentication configuration */
		private ApiAuth auth;

	}

	/** Authentication configuration class */
	@Data
	public static class ApiAuth implements Serializable {

		/** Type of authentication */
		private ApiAuthType type;

		/** Position of authorization in the request */
		@JsonProperty("authorization_position")
		private AuthorizationPosition authorizationPosition;

		/** Type of authorization */
		@JsonProperty("authorization_type")
		private AuthorizationType authorizationType;

		/** Key used for authorization */
		@JsonProperty("authorization_key")
		private String authorizationKey;

		/** Value used for authorization */
		@JsonProperty("authorization_value")
		private String authorizationValue;

	}

	/** Authentication type enum */
	public enum ApiAuthType {

		@JsonProperty("none")
		NONE,

		@JsonProperty("api_key")
		API_KEY,

	}

	/** Authorization position enum */
	public enum AuthorizationPosition {

		@JsonProperty("header")
		HEADER,

		@JsonProperty("query")
		QUERY

	}

	/** Authorization type enum */
	public enum AuthorizationType {

		@JsonProperty("basic")
		BASIC,

		@JsonProperty("bearer")
		BEARER,

		@JsonProperty("custom")
		CUSTOM

	}

}
