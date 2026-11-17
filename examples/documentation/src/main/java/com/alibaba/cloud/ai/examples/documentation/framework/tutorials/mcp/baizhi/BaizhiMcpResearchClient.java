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
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.json.jackson.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.ToolCallback;

/**
 * Connects to the optionally configured Baizhi MCP service and registers only the tools
 * that were explicitly selected and actually discovered.
 *
 * <p>
 * Behavior intentionally kept narrower than the generic MCP tutorials:
 * <ul>
 * <li>the API key is validated before any network call;</li>
 * <li>the key is sent only as a {@code Bearer} header to the single configured origin,
 * and HTTP redirects are disabled so the header cannot follow a redirect;</li>
 * <li>if one of the explicitly required tools is not discovered, connection fails
 * instead of falling back to all available tools;</li>
 * <li>at most three tools are registered, using the discovered input schema rather than
 * a fixed {@code Map} input type.</li>
 * </ul>
 *
 * <p>
 * This class is an example, not a production MCP client. See the package README for its
 * verification boundary.
 *
 * @author Spring AI Alibaba
 */
public final class BaizhiMcpResearchClient implements AutoCloseable {

	private static final Logger log = LoggerFactory.getLogger(BaizhiMcpResearchClient.class);

	private static final String AUTHORIZATION_HEADER = "Authorization";

	private final McpSyncClient mcpClient;

	private final List<String> selectedToolNames;

	private final List<ToolCallback> toolCallbacks;

	private BaizhiMcpResearchClient(McpSyncClient mcpClient, List<String> selectedToolNames,
			List<ToolCallback> toolCallbacks) {
		this.mcpClient = mcpClient;
		this.selectedToolNames = List.copyOf(selectedToolNames);
		this.toolCallbacks = List.copyOf(toolCallbacks);
	}

	/**
	 * Initialize the MCP session, discover tools and register exactly the required tools.
	 * @param config validated configuration; its API key is checked again before the
	 * HTTP client is created
	 * @return connected client holding the selected callbacks
	 * @throws IllegalStateException when initialization fails or a required tool is
	 * missing
	 */
	public static BaizhiMcpResearchClient connect(BaizhiMcpResearchConfig config) {
		Objects.requireNonNull(config, "config must not be null");

		// Fail on a missing key before building the transport so no request is attempted.
		if (config.apiKey() == null || config.apiKey().isBlank()) {
			throw new IllegalStateException(BaizhiMcpResearchConfig.API_KEY_ENV + " is not set");
		}

		McpClientTransport transport = buildTransport(config);
		McpSyncClient mcpClient = McpClient.sync(transport)
			.requestTimeout(config.requestTimeout())
			.initializationTimeout(config.requestTimeout())
			.build();

		try {
			mcpClient.initialize();
			List<McpSchema.Tool> discoveredTools = mcpClient.listTools().tools();
			List<McpSchema.Tool> selectedTools = selectRequiredTools(config.requiredTools(), discoveredTools);
			List<ToolCallback> callbacks = new ArrayList<>(selectedTools.size());
			for (McpSchema.Tool tool : selectedTools) {
				callbacks.add(new TextOnlyMcpToolCallback(mcpClient, tool));
			}
			List<String> names = selectedTools.stream().map(McpSchema.Tool::name).toList();
			log.info("Initialized Baizhi MCP session at {} and registered {} explicitly selected tools: {}",
					config.serverUri(), names.size(), names);
			return new BaizhiMcpResearchClient(mcpClient, names, callbacks);
		}
		catch (RuntimeException ex) {
			closeQuietly(mcpClient);
			throw new IllegalStateException("Baizhi MCP initialization at " + config.serverUri() + " failed: "
					+ ex.getMessage(), ex);
		}
	}

	/**
	 * Select only the required tools out of the discovered set.
	 * @param requiredToolNames explicitly required tool names
	 * @param discoveredTools tools returned by the service
	 * @return discovered tools in the requested order
	 * @throws IllegalStateException when any required tool is absent; the caller must not
	 * fall back to all tools
	 */
	static List<McpSchema.Tool> selectRequiredTools(List<String> requiredToolNames,
			List<McpSchema.Tool> discoveredTools) {
		Map<String, McpSchema.Tool> byName = new LinkedHashMap<>();
		for (McpSchema.Tool tool : discoveredTools) {
			byName.put(tool.name(), tool);
		}
		List<String> missing = requiredToolNames.stream().filter(name -> !byName.containsKey(name)).toList();
		if (!missing.isEmpty()) {
			throw new IllegalStateException("Required MCP tools not found: " + missing + "; discovered tools: "
					+ byName.keySet() + ". The example stops instead of falling back to all discovered tools");
		}
		return requiredToolNames.stream().map(byName::get).toList();
	}

	private static McpClientTransport buildTransport(BaizhiMcpResearchConfig config) {
		URI serverUri = config.serverUri();
		String baseUri = serverUri.getScheme() + "://" + serverUri.getRawAuthority();
		String endpoint = serverUri.getRawPath();

		return HttpClientStreamableHttpTransport.builder(baseUri)
			.endpoint(endpoint)
			.clientBuilder(HttpClient.newBuilder()
				.version(HttpClient.Version.HTTP_1_1)
				.connectTimeout(config.connectTimeout())
				// Do not let a redirect forward the Authorization header to another origin.
				.followRedirects(HttpClient.Redirect.NEVER))
			.jsonMapper(new JacksonMcpJsonMapper(new ObjectMapper()))
			// The transport only talks to the configured origin; attach the credential
			// here so it is never placed in a URL or a log message.
			.customizeRequest(request -> request.header(AUTHORIZATION_HEADER, "Bearer " + config.apiKey()))
			.build();
	}

	/** Names of the tools registered with the agent, in the required order. */
	public List<String> selectedToolNames() {
		return this.selectedToolNames;
	}

	/** Schema-preserving tool callbacks for the selected tools. */
	public List<ToolCallback> toolCallbacks() {
		return this.toolCallbacks;
	}

	@Override
	public void close() {
		try {
			this.mcpClient.close();
		}
		catch (RuntimeException ex) {
			log.warn("Failed to close the Baizhi MCP client cleanly", ex);
		}
	}

	private static void closeQuietly(McpSyncClient client) {
		try {
			client.close();
		}
		catch (RuntimeException ex) {
			log.warn("Failed to close the Baizhi MCP client after a failed initialization", ex);
		}
	}

}
