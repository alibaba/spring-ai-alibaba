/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.documentation.framework.tutorials.mcp.baizhi;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Explicit configuration for the optional Baizhi MCP limited-tool research example.
 *
 * <p>
 * The service address and the user's own API key are both required. Credentials have no
 * default value here and are never written to the repository, command line arguments or
 * logs. Validation happens before any HTTP client is created, so a missing key fails
 * before the example reaches the network.
 *
 * <p>
 * See the package {@code README.md} for the environment variables and for the credential
 * separation between the MCP service ({@code BAIZHI_API_KEY}) and the model provider
 * ({@code AI_DASHSCOPE_API_KEY}).
 *
 * @author Spring AI Alibaba
 */
public record BaizhiMcpResearchConfig(URI serverUri, String apiKey, List<String> requiredTools,
		Duration connectTimeout, Duration requestTimeout) {

	/** Environment variable holding the full Streamable HTTP endpoint. */
	public static final String SERVER_URI_ENV = "BAIZHI_MCP_URL";

	/** Environment variable holding the user's own Baizhi API key. */
	public static final String API_KEY_ENV = "BAIZHI_API_KEY";

	/** Environment variable overriding the connect timeout in seconds. */
	public static final String CONNECT_TIMEOUT_ENV = "BAIZHI_MCP_CONNECT_TIMEOUT_SECONDS";

	/** Environment variable overriding the request (call and initialization) timeout. */
	public static final String REQUEST_TIMEOUT_ENV = "BAIZHI_MCP_REQUEST_TIMEOUT_SECONDS";

	/**
	 * The three tools documented by the Baizhi Agent Toolkit. The example registers at
	 * most these tools and stops when one of them is not discovered.
	 */
	public static final List<String> DEFAULT_REQUIRED_TOOLS = List.of("websearch_search", "web_scrape",
			"web_extract");

	private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

	private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(120);

	private static final int MAX_REQUIRED_TOOLS = 3;

	public BaizhiMcpResearchConfig {
		Objects.requireNonNull(serverUri, "serverUri must not be null");
		if (!serverUri.isAbsolute() || serverUri.getHost() == null) {
			throw new IllegalArgumentException(SERVER_URI_ENV + " must be an absolute URL with a host");
		}
		if (serverUri.getUserInfo() != null) {
			throw new IllegalArgumentException(SERVER_URI_ENV + " must not embed credentials");
		}
		if (serverUri.getQuery() != null || serverUri.getFragment() != null) {
			throw new IllegalArgumentException(SERVER_URI_ENV + " must not contain a query or fragment");
		}
		if (serverUri.getRawPath() == null || serverUri.getRawPath().isBlank()
				|| "/".equals(serverUri.getRawPath())) {
			throw new IllegalArgumentException(
					SERVER_URI_ENV + " must include the MCP endpoint path, for example /mcp");
		}
		String scheme = serverUri.getScheme().toLowerCase(Locale.ROOT);
		boolean loopback = isLoopbackHost(serverUri.getHost());
		if (!"https".equals(scheme) && !("http".equals(scheme) && loopback)) {
			throw new IllegalArgumentException(
					SERVER_URI_ENV + " must use HTTPS; plain HTTP is only accepted for loopback test servers");
		}

		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
					API_KEY_ENV + " is not set. Export your own Baizhi API key before running the example");
		}
		apiKey = apiKey.trim();

		Objects.requireNonNull(requiredTools, "requiredTools must not be null");
		if (requiredTools.isEmpty() || requiredTools.size() > MAX_REQUIRED_TOOLS) {
			throw new IllegalArgumentException(
					"Select between 1 and " + MAX_REQUIRED_TOOLS + " required tools, got " + requiredTools.size());
		}
		if (requiredTools.stream().anyMatch(tool -> tool == null || tool.isBlank())) {
			throw new IllegalArgumentException("Required tool names must not be blank");
		}
		if (requiredTools.stream().distinct().count() != requiredTools.size()) {
			throw new IllegalArgumentException("Required tool names must be distinct");
		}
		requiredTools = List.copyOf(requiredTools);

		requirePositive(connectTimeout, "connectTimeout");
		requirePositive(requestTimeout, "requestTimeout");
	}

	/**
	 * Read the configuration from the process environment.
	 * @return validated configuration
	 * @throws IllegalStateException when a required variable is missing or invalid
	 */
	public static BaizhiMcpResearchConfig fromEnvironment() {
		return fromEnvironment(System.getenv());
	}

	/**
	 * Read the configuration from an explicit environment map, keeping the example
	 * testable without mutating process state.
	 * @param environment environment variables
	 * @return validated configuration
	 * @throws IllegalStateException when a required variable is missing or invalid
	 */
	public static BaizhiMcpResearchConfig fromEnvironment(Map<String, String> environment) {
		Objects.requireNonNull(environment, "environment must not be null");
		String serverUrl = environment.get(SERVER_URI_ENV);
		if (serverUrl == null || serverUrl.isBlank()) {
			throw new IllegalStateException(SERVER_URI_ENV
					+ " is not set. Export the Streamable HTTP endpoint of the MCP service before running the example");
		}
		URI serverUri;
		try {
			serverUri = URI.create(serverUrl.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(SERVER_URI_ENV + " is not a valid absolute URL", ex);
		}
		String apiKey = environment.get(API_KEY_ENV);
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
					API_KEY_ENV + " is not set. Export your own Baizhi API key before running the example");
		}
		return new BaizhiMcpResearchConfig(serverUri, apiKey, DEFAULT_REQUIRED_TOOLS,
				parseDuration(environment, CONNECT_TIMEOUT_ENV, DEFAULT_CONNECT_TIMEOUT),
				parseDuration(environment, REQUEST_TIMEOUT_ENV, DEFAULT_REQUEST_TIMEOUT));
	}

	private static Duration parseDuration(Map<String, String> environment, String name, Duration defaultValue) {
		String raw = environment.get(name);
		if (raw == null || raw.isBlank()) {
			return defaultValue;
		}
		try {
			long seconds = Long.parseLong(raw.trim());
			if (seconds <= 0) {
				throw new NumberFormatException("not positive");
			}
			return Duration.ofSeconds(seconds);
		}
		catch (NumberFormatException ex) {
			throw new IllegalStateException(name + " must be a positive number of seconds, got: " + raw, ex);
		}
	}

	private static void requirePositive(Duration duration, String name) {
		Objects.requireNonNull(duration, name + " must not be null");
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException(name + " must be positive");
		}
	}

	private static boolean isLoopbackHost(String host) {
		return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host)
				|| "0:0:0:0:0:0:0:1".equals(host) || "[::1]".equals(host);
	}

	@Override
	public String toString() {
		return "BaizhiMcpResearchConfig[serverUri=" + serverUri + ", apiKey=***, requiredTools=" + requiredTools
				+ ", connectTimeout=" + connectTimeout + ", requestTimeout=" + requestTimeout + "]";
	}

}
