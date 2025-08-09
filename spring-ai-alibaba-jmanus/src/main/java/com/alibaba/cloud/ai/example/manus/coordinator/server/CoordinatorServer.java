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

package com.alibaba.cloud.ai.example.manus.coordinator.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.example.manus.coordinator.tool.EndPointUtils;
import com.alibaba.cloud.ai.example.manus.config.CoordinatorToolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.alibaba.cloud.ai.example.manus.coordinator.service.CoordinatorService;
import com.alibaba.cloud.ai.example.manus.coordinator.tool.CoordinatorTool;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * Coordinator Server Application
 *
 * Supports multi-endpoint coordinator server, each endpoint corresponds to a group of tools
 * Reference WebFluxStreamableServerApplication's multi-endpoint logic
 */
@Component
public class CoordinatorServer implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorServer.class);

	@Autowired
	private Environment environment;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CoordinatorService coordinatorService;

	@Autowired
	private CoordinatorToolProperties coordinatorToolProperties;

	private DisposableServer httpServer;

	private List<Object> mcpServers = new ArrayList<>();

	// Store registered tools, grouped by endpoint
	private Map<String, List<CoordinatorTool>> registeredTools = new ConcurrentHashMap<>();

	// Store registered MCP servers, grouped by endpoint
	private Map<String, Object> registeredMcpServers = new ConcurrentHashMap<>();

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping coordinator server startup");
			return;
		}

		// Delay coordinator server startup to ensure all Beans are initialized
		try {
			Thread.sleep(1000); // Wait 1 second to ensure all Beans are initialized
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		startCoordinatorServer();
	}

	/**
	 * Start coordinator server
	 */
	private void startCoordinatorServer() {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping coordinator server startup");
			return;
		}

		try {
			log.info("==========================================");
			log.info("JManus Multi EndPoint Streamable Http Server");
			log.info("==========================================");
			log.info("Starting JManus Multi EndPoint Streamable Http Server...");

			log.info("Server Information:");
			log.info("  Full Address: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);

			// Load coordinator tools
			Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint = coordinatorService.loadCoordinatorTools();

			// Combine all router functions
			RouterFunction<?> combinedRouter = createCombinedRouter(coordinatorToolsByEndpoint);

			if (combinedRouter == null) {
				log.warn("No router functions created, server may not function normally");
			}
			else {
				log.info("Successfully created combined router functions");
			}

			// Create HTTP handler
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// Start HTTP server
			this.httpServer = HttpServer.create().port(EndPointUtils.SERVICE_PORT).handle(adapter).bindNow();

			log.info("HTTP server started, listening on port: {}", EndPointUtils.SERVICE_PORT);
			log.info("Server address: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);

			log.info("JManus Multi EndPoint Streamable Http Server started successfully!");
			log.info("==========================================");
			log.info("Coordinator Service List:");
			log.info("==========================================");

			// Output all coordinator service information
			if (!coordinatorToolsByEndpoint.isEmpty()) {
				int serviceIndex = 1;
				for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
					String endpoint = entry.getKey();
					List<CoordinatorTool> tools = entry.getValue();
					log.info("  Full URL: {}", EndPointUtils.getUrl(endpoint));
					log.info("  Tool Count: {}", tools.size());

					// Output all tools for this endpoint
					for (int i = 0; i < tools.size(); i++) {
						CoordinatorTool tool = tools.get(i);
						log.info("    Tool #{}: {} - {}", i + 1, tool.getToolName(), tool.getToolDescription());
					}
					log.info("  ----------------------------------------");
				}
			}
			else {
				log.info("No coordinator services found");
			}

			log.info("==========================================");
			log.info("Coordinator service startup complete, {} endpoints", coordinatorToolsByEndpoint.size());
			log.info("==========================================");

		}
		catch (Exception e) {
			log.error("Error starting server: {}", e.getMessage(), e);
		}
	}

	/**
	 * Create combined router functions
	 */
	private RouterFunction<?> createCombinedRouter(Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint) {

		log.info("Starting to create combined router functions, {} endpoints", coordinatorToolsByEndpoint.size());

		RouterFunction<?> combinedRouter = null;

		// Create independent transport providers and servers for each coordinator endpoint
		for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
			String endpoint = entry.getKey();
			List<CoordinatorTool> tools = entry.getValue();

			if (tools.isEmpty()) {
				continue;
			}

			// Register tools to internal storage
			registeredTools.put(endpoint, new ArrayList<>(tools));

			// Create MCP server and router function
			RouterFunction<?> routerFunction = createMcpServerAndGetRouter(endpoint, tools);
			if (routerFunction != null) {
				if (combinedRouter == null) {
					combinedRouter = routerFunction;
				}
				else {
					combinedRouter = combinedRouter.andOther(routerFunction);
				}
			}

			log.info("Created MCP server for endpoint {}, containing {} tools", endpoint, tools.size());
		}

		return combinedRouter;
	}

	/**
	 * Create MCP server and get router function
	 * @param endpoint Endpoint address
	 * @param tools Tool list
	 * @return Router function
	 */
	private RouterFunction<?> createMcpServerAndGetRouter(String endpoint, List<CoordinatorTool> tools) {
		try {
			// Build messageEndpoint, add default prefix /mcp
			String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

			// Create transport provider
			WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
				.builder()
				.objectMapper(objectMapper)
				.messageEndpoint(messageEndpoint)
				.build();

			// Create tool specification
			List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
			for (CoordinatorTool tool : tools) {
				toolSpecs.add(coordinatorService.createToolSpecification(tool));
			}

			// Create MCP server
			McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
				.serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).logging().build())
				.tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

			Object mcpServer = serverSpec.build();

			// Store MCP server
			registeredMcpServers.put(endpoint, mcpServer);
			mcpServers.add(mcpServer);

			log.info("Successfully created MCP server for endpoint: {}, containing {} tools", endpoint, tools.size());

			// Return router function
			return transportProvider.getRouterFunction();

		}
		catch (Exception e) {
			log.error("Exception occurred while creating MCP server for endpoint: {}, {}", endpoint, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Register CoordinatorTool to coordinator server
	 * @param tool Coordinator tool to register
	 * @return Whether registration was successful
	 */
	public boolean registerCoordinatorTool(CoordinatorTool tool) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping tool registration");
			return false;
		}

		if (tool == null) {
			log.warn("CoordinatorTool is null, cannot register");
			return false;
		}

		String endpoint = tool.getEndpoint();
		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("CoordinatorTool's endpoint is empty, cannot register");
			return false;
		}

		try {
			log.info("Starting to register CoordinatorTool: {} to endpoint: {}", tool.getToolName(), endpoint);

			// Get or create the tool list for this endpoint
			List<CoordinatorTool> toolsForEndpoint = registeredTools.computeIfAbsent(endpoint, k -> new ArrayList<>());

			// Check if tool is already registered, and remove old version if it is
			boolean alreadyRegistered = toolsForEndpoint.stream()
				.anyMatch(existingTool -> existingTool.getToolName().equals(tool.getToolName()));

			if (alreadyRegistered) {
				log.info("CoordinatorTool: {} is already registered to endpoint: {}, will update to new service registration", tool.getToolName(), endpoint);
				// Remove old tool version
				toolsForEndpoint.removeIf(existingTool -> existingTool.getToolName().equals(tool.getToolName()));
			}

			// Add new tool to the list (whether it's new or updated)
			toolsForEndpoint.add(tool);
			log.info("Successfully added CoordinatorTool: {} to the tool list for endpoint: {}", tool.getToolName(), endpoint);

			// Check if MCP server already exists for this endpoint
			Object existingMcpServer = registeredMcpServers.get(endpoint);
			if (existingMcpServer != null) {
				log.info("Endpoint: {} already has MCP server, need to recreate to include new tools", endpoint);
				// Recreate MCP server for this endpoint
				recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);
			}
			else {
				log.info("Endpoint: {} does not have MCP server, creating new MCP server", endpoint);
				// Create new MCP server
				createMcpServerForEndpoint(endpoint, toolsForEndpoint);
			}

			log.info("Successfully registered CoordinatorTool: {} to endpoint: {}", tool.getToolName(), endpoint);
			log.info("Coordinator Service Access Information:");
			log.info("  Full URL: {}", EndPointUtils.getUrl(endpoint));

			return true;

		}
		catch (Exception e) {
			log.error("Exception occurred while registering CoordinatorTool: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Create MCP server for a specific endpoint
	 * @param endpoint Endpoint address
	 * @param tools Tool list for this endpoint
	 */
	private void createMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping MCP server creation");
			return;
		}

		try {
			// Build messageEndpoint, add default prefix /mcp
			String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

			// Create transport provider
			WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
				.builder()
				.objectMapper(objectMapper)
				.messageEndpoint(messageEndpoint)
				.build();

			// Create tool specification
			List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
			for (CoordinatorTool tool : tools) {
				toolSpecs.add(coordinatorService.createToolSpecification(tool));
			}

			// Create MCP server
			McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
				.serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).logging().build())
				.tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

			Object mcpServer = serverSpec.build();

			// Store MCP server
			registeredMcpServers.put(endpoint, mcpServer);
			mcpServers.add(mcpServer);

			log.info("Successfully created MCP server for endpoint: {}, containing {} tools", endpoint, tools.size());

			// Recreate HTTP server to update routes
			recreateHttpServer();

		}
		catch (Exception e) {
			log.error("Exception occurred while creating MCP server for endpoint: {}, {}", endpoint, e.getMessage(), e);
		}
	}

	/**
	 * Recreate MCP server for a specific endpoint
	 * @param endpoint Endpoint address
	 * @param tools Tool list for this endpoint
	 */
	private void recreateMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping MCP server recreation");
			return;
		}

		try {
			log.info("Starting to recreate MCP server for endpoint: {}", endpoint);

			// Delete old MCP server
			Object oldMcpServer = registeredMcpServers.remove(endpoint);
			if (oldMcpServer != null) {
				mcpServers.remove(oldMcpServer);
				if (oldMcpServer instanceof AutoCloseable) {
					try {
						((AutoCloseable) oldMcpServer).close();
						log.info("Old MCP server closed: {}", endpoint);
					}
					catch (Exception e) {
						log.warn("Exception occurred while closing old MCP server: {}", e.getMessage());
					}
				}
			}

			// Create new MCP server
			createMcpServerForEndpoint(endpoint, tools);

			log.info("Successfully recreated MCP server for endpoint: {}", endpoint);

		}
		catch (Exception e) {
			log.error("Exception occurred while recreating MCP server for endpoint: {}, {}", endpoint, e.getMessage(), e);
		}
	}

	/**
	 * Recreate HTTP server
	 */
	private void recreateHttpServer() {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping HTTP server recreation");
			return;
		}

		try {
			log.info("Starting to recreate HTTP server to update routes");

			// Stop current HTTP server
			if (this.httpServer != null) {
				this.httpServer.disposeNow();
				log.info("Current HTTP server stopped");
			}

			// Recreate combined router
			RouterFunction<?> combinedRouter = createCombinedRouter(registeredTools);

			if (combinedRouter == null) {
				log.warn("No router functions created, cannot recreate HTTP server");
				return;
			}

			// Create new HTTP handler
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// Start new HTTP server
			this.httpServer = HttpServer.create().port(EndPointUtils.SERVICE_PORT).handle(adapter).bindNow();

			log.info("Successfully recreated HTTP server, listening on port: {}", EndPointUtils.SERVICE_PORT);

		}
		catch (Exception e) {
			log.error("Exception occurred while recreating HTTP server: {}", e.getMessage(), e);
		}
	}

	/**
	 * Forcefully refresh a specific tool
	 * @param toolName Tool name
	 * @param updatedTool Updated tool
	 * @return Whether refresh was successful
	 */
	public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping tool refresh");
			return false;
		}

		if (updatedTool == null || toolName == null) {
			log.warn("Tool or tool name is empty, cannot refresh");
			return false;
		}

		String endpoint = updatedTool.getEndpoint();
		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("Tool's endpoint is empty, cannot refresh");
			return false;
		}

		try {
			log.info("Starting to force refresh tool: {} in endpoint: {}", toolName, endpoint);

			// Get tool list for this endpoint
			List<CoordinatorTool> toolsForEndpoint = registeredTools.get(endpoint);
			if (toolsForEndpoint == null) {
				log.warn("Tool list not found for endpoint: {}", endpoint);
				return false;
			}

			// Find and replace tool
			boolean found = false;
			for (int i = 0; i < toolsForEndpoint.size(); i++) {
				if (toolsForEndpoint.get(i).getToolName().equals(toolName)) {
					toolsForEndpoint.set(i, updatedTool);
					found = true;
					log.info("Tool found and replaced: {}", toolName);
					break;
				}
			}

			if (!found) {
				log.warn("Tool: {} not found in endpoint: {}", endpoint, toolName);
				return false;
			}

			// Recreate MCP server
			recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);

			log.info("Successfully refreshed tool: {} in endpoint: {}", toolName, endpoint);
			return true;

		}
		catch (Exception e) {
			log.error("Exception occurred while refreshing tool: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * Unregister CoordinatorTool from coordinator server
	 * @param toolName Tool name to unregister
	 * @param endpoint Endpoint address
	 * @return Whether unregistration was successful
	 */
	public boolean unregisterCoordinatorTool(String toolName, String endpoint) {
		// Check if CoordinatorTool feature is enabled
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping tool unregistration");
			return false;
		}

		if (toolName == null || toolName.trim().isEmpty()) {
			log.warn("Tool name is empty, cannot unregister");
			return false;
		}

		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("Endpoint is empty, cannot unregister");
			return false;
		}

		try {
			log.info("Starting to unregister CoordinatorTool: {} from endpoint: {}", toolName, endpoint);

			// Get tool list for this endpoint
			List<CoordinatorTool> toolsForEndpoint = registeredTools.get(endpoint);
			if (toolsForEndpoint == null) {
				log.warn("No tools found for endpoint: {}", endpoint);
				return false;
			}

			// Remove tool from the list
			boolean removed = toolsForEndpoint.removeIf(tool -> tool.getToolName().equals(toolName));

			if (!removed) {
				log.warn("Tool: {} not found in endpoint: {}", toolName, endpoint);
				return false;
			}

			log.info("Successfully removed tool: {} from endpoint: {}", toolName, endpoint);

			// If no tools left for this endpoint, remove the endpoint entirely
			if (toolsForEndpoint.isEmpty()) {
				registeredTools.remove(endpoint);
				Object mcpServer = registeredMcpServers.remove(endpoint);
				if (mcpServer != null) {
					mcpServers.remove(mcpServer);
					if (mcpServer instanceof AutoCloseable) {
						try {
							((AutoCloseable) mcpServer).close();
							log.info("Closed MCP server for empty endpoint: {}", endpoint);
						}
						catch (Exception e) {
							log.warn("Exception occurred while closing MCP server: {}", e.getMessage());
						}
					}
				}
				log.info("Removed empty endpoint: {}", endpoint);
			} else {
				// Recreate MCP server for this endpoint with remaining tools
				recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);
			}

			log.info("Successfully unregistered CoordinatorTool: {} from endpoint: {}", toolName, endpoint);
			return true;

		}
		catch (Exception e) {
			log.error("Exception occurred while unregistering CoordinatorTool: {}", e.getMessage(), e);
			return false;
		}
	}

}
