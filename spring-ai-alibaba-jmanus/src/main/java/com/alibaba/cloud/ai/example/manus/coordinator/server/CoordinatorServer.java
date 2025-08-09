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
import java.util.function.Supplier;

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

	// ==================== Log Constants ====================
	private static final String LOG_SEPARATOR = "==========================================";
	private static final String LOG_SERVER_TITLE = "JManus Multi EndPoint Streamable Http Server";
	private static final String LOG_SERVICE_LIST_TITLE = "Coordinator Service List:";
	private static final String LOG_TOOL_FORMAT = "    Tool #{}: {} - {}";
	private static final String LOG_URL_FORMAT = "  Full URL: {}";
	private static final String LOG_COUNT_FORMAT = "  Tool Count: {}";
	private static final String LOG_DIVIDER = "  ----------------------------------------";
	private static final String LOG_NO_SERVICES = "No coordinator services found";
	private static final String LOG_STARTUP_COMPLETE = "Coordinator service startup complete, {} endpoints";

	@Autowired
	private Environment environment;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CoordinatorService coordinatorService;

	@Autowired
	private CoordinatorToolProperties coordinatorToolProperties;

	// Internal component: MCP server manager
	private final McpServerManager mcpServerManager;
	// Internal component: Tool registry manager  
	private final ToolRegistryManager toolRegistryManager;
	// Internal component: HTTP server manager
	private final HttpServerManager httpServerManager;

	public CoordinatorServer() {
		this.mcpServerManager = new McpServerManager();
		this.toolRegistryManager = new ToolRegistryManager();
		this.httpServerManager = new HttpServerManager();
	}

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
			logServerStartup();

			// Load coordinator tools
			Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint = coordinatorService.loadCoordinatorTools();

			// Combine all router functions
			RouterFunction<?> combinedRouter = mcpServerManager.createCombinedRouter(coordinatorToolsByEndpoint);

			// Start HTTP server
			httpServerManager.startHttpServer(combinedRouter);

			logServerStartupComplete(coordinatorToolsByEndpoint);

		}
		catch (Exception e) {
			log.error("Error starting server: {}", e.getMessage(), e);
		}
	}

	/**
	 * Register CoordinatorTool to coordinator server
	 * @param tool Coordinator tool to register
	 * @return Whether registration was successful
	 */
	public boolean registerCoordinatorTool(CoordinatorTool tool) {
		if (tool == null || tool.getEndpoint() == null || tool.getEndpoint().trim().isEmpty()) {
			log.warn("Invalid tool parameters");
			return false;
		}
		
		return executeWithValidation("tool registration", 
			() -> toolRegistryManager.registerTool(tool, mcpServerManager, httpServerManager),
			tool.getToolName(), tool.getEndpoint()) != null;
	}

	/**
	 * Unregister CoordinatorTool from coordinator server
	 * @param toolName Tool name to unregister
	 * @param endpoint Endpoint address
	 * @return Whether unregistration was successful
	 */
	public boolean unregisterCoordinatorTool(String toolName, String endpoint) {
		if (toolName == null || toolName.trim().isEmpty() || 
			endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("Invalid parameters");
			return false;
		}
		
		return executeWithValidation("tool unregistration",
			() -> toolRegistryManager.unregisterTool(toolName, endpoint, mcpServerManager, httpServerManager),
			toolName, endpoint) != null;
	}

	/**
	 * Forcefully refresh a specific tool
	 * @param toolName Tool name
	 * @param updatedTool Updated tool
	 * @return Whether refresh was successful
	 */
	public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
		if (updatedTool == null || toolName == null || 
			updatedTool.getEndpoint() == null || updatedTool.getEndpoint().trim().isEmpty()) {
			log.warn("Invalid tool parameters");
			return false;
		}
		
		return executeWithValidation("tool refresh",
			() -> toolRegistryManager.refreshTool(toolName, updatedTool, mcpServerManager, httpServerManager),
			toolName, updatedTool.getEndpoint()) != null;
	}

	// ==================== Execution Template Methods ====================

	/**
	 * Execution template method, unified handling of validation, logging and exceptions
	 * @param operation Operation name
	 * @param operationSupplier Operation execution function
	 * @param operationParams Operation parameters
	 * @return Operation result
	 */
	private <T> T executeWithValidation(String operation, Supplier<T> operationSupplier, 
	                                   String... operationParams) {
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool feature is disabled, skipping {}", operation);
			return null;
		}
		
		try {
			log.info("Starting {}: {}", operation, String.join(", ", operationParams));
			T result = operationSupplier.get();
			log.info("Successfully completed {}: {}", operation, String.join(", ", operationParams));
			return result;
		} catch (Exception e) {
			log.error("Exception occurred during {}: {}", operation, e.getMessage(), e);
			return null;
		}
	}

	// ==================== Helper Methods ====================

	/**
	 * Log server startup information
	 */
	private void logServerStartup() {
		log.info(LOG_SEPARATOR);
		log.info(LOG_SERVER_TITLE);
		log.info(LOG_SEPARATOR);
		log.info("Starting {}...", LOG_SERVER_TITLE);

		log.info("Server Information:");
		log.info("  Full Address: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);
	}

	/**
	 * Log server startup completion information
	 */
	private void logServerStartupComplete(Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint) {
		log.info("{} started successfully!", LOG_SERVER_TITLE);
		log.info(LOG_SEPARATOR);
		log.info(LOG_SERVICE_LIST_TITLE);
		log.info(LOG_SEPARATOR);

		// Output all coordinator service information
		if (!coordinatorToolsByEndpoint.isEmpty()) {
			for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
				String endpoint = entry.getKey();
				List<CoordinatorTool> tools = entry.getValue();
				log.info(LOG_URL_FORMAT, EndPointUtils.getUrl(endpoint));
				log.info(LOG_COUNT_FORMAT, tools.size());

				// Output all tools for this endpoint
				for (int i = 0; i < tools.size(); i++) {
					CoordinatorTool tool = tools.get(i);
					log.info(LOG_TOOL_FORMAT, i + 1, tool.getToolName(), tool.getToolDescription());
				}
				log.info(LOG_DIVIDER);
			}
		}
		else {
			log.info(LOG_NO_SERVICES);
		}

		log.info(LOG_SEPARATOR);
		log.info(LOG_STARTUP_COMPLETE, coordinatorToolsByEndpoint.size());
		log.info(LOG_SEPARATOR);
	}

	// ==================== Internal Component Classes ====================

	/**
	 * MCP Server Manager
	 */
	private class McpServerManager {
		private final Map<String, Object> registeredMcpServers = new ConcurrentHashMap<>();
		private final List<Object> mcpServers = new ArrayList<>();

		/**
		 * Create combined router functions
		 */
		public RouterFunction<?> createCombinedRouter(Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint) {
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
				toolRegistryManager.registeredTools.put(endpoint, new ArrayList<>(tools));

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
		 * Create MCP server for a specific endpoint
		 * @param endpoint Endpoint address
		 * @param tools Tool list for this endpoint
		 */
		public void createMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
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
		public void recreateMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
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
					closeMcpServer(oldMcpServer);
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
		 * Remove MCP server for a specific endpoint
		 * @param endpoint Endpoint address
		 */
		public void removeMcpServer(String endpoint) {
			Object mcpServer = registeredMcpServers.remove(endpoint);
			if (mcpServer != null) {
				mcpServers.remove(mcpServer);
				closeMcpServer(mcpServer);
				log.info("Removed MCP server for endpoint: {}", endpoint);
			}
		}

		/**
		 * Close MCP server
		 * @param mcpServer MCP server to close
		 */
		private void closeMcpServer(Object mcpServer) {
			if (mcpServer instanceof AutoCloseable) {
				try {
					((AutoCloseable) mcpServer).close();
					log.info("MCP server closed successfully");
				}
				catch (Exception e) {
					log.warn("Exception occurred while closing MCP server: {}", e.getMessage());
				}
			}
		}
	}

	/**
	 * Tool Registry Manager
	 */
	private class ToolRegistryManager {
		private final Map<String, List<CoordinatorTool>> registeredTools = new ConcurrentHashMap<>();

		/**
		 * Register tool
		 * @param tool Tool to register
		 * @param mcpManager MCP server manager
		 * @param httpManager HTTP server manager
		 * @return Whether registration was successful
		 */
		public boolean registerTool(CoordinatorTool tool, McpServerManager mcpManager, HttpServerManager httpManager) {
			try {
				log.info("Starting to register CoordinatorTool: {} to endpoint: {}", tool.getToolName(), tool.getEndpoint());

				String endpoint = tool.getEndpoint();
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
				if (registeredTools.containsKey(endpoint)) {
					log.info("Endpoint: {} already has MCP server, need to recreate to include new tools", endpoint);
					// Recreate MCP server for this endpoint
					mcpManager.recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);
				}
				else {
					log.info("Endpoint: {} does not have MCP server, creating new MCP server", endpoint);
					// Create new MCP server
					mcpManager.createMcpServerForEndpoint(endpoint, toolsForEndpoint);
				}

				// Recreate HTTP server to update routes
				httpManager.recreateHttpServer(registeredTools);

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
		 * Unregister tool
		 * @param toolName Tool name to unregister
		 * @param endpoint Endpoint address
		 * @param mcpManager MCP server manager
		 * @param httpManager HTTP server manager
		 * @return Whether unregistration was successful
		 */
		public boolean unregisterTool(String toolName, String endpoint, McpServerManager mcpManager, HttpServerManager httpManager) {
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
					mcpManager.removeMcpServer(endpoint);
					log.info("Removed empty endpoint: {}", endpoint);
				} else {
					// Recreate MCP server for this endpoint with remaining tools
					mcpManager.recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);
				}

				// Recreate HTTP server to update routes
				httpManager.recreateHttpServer(registeredTools);

				log.info("Successfully unregistered CoordinatorTool: {} from endpoint: {}", toolName, endpoint);
				return true;

			}
			catch (Exception e) {
				log.error("Exception occurred while unregistering CoordinatorTool: {}", e.getMessage(), e);
				return false;
			}
		}

		/**
		 * Refresh tool
		 * @param toolName Tool name
		 * @param updatedTool Updated tool
		 * @param mcpManager MCP server manager
		 * @param httpManager HTTP server manager
		 * @return Whether refresh was successful
		 */
		public boolean refreshTool(String toolName, CoordinatorTool updatedTool, McpServerManager mcpManager, HttpServerManager httpManager) {
			try {
				log.info("Starting to force refresh tool: {} in endpoint: {}", toolName, updatedTool.getEndpoint());

				String endpoint = updatedTool.getEndpoint();
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
				mcpManager.recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);

				// Recreate HTTP server to update routes
				httpManager.recreateHttpServer(registeredTools);

				log.info("Successfully refreshed tool: {} in endpoint: {}", toolName, endpoint);
				return true;

			}
			catch (Exception e) {
				log.error("Exception occurred while refreshing tool: {}", e.getMessage(), e);
				return false;
			}
		}
	}

	/**
	 * HTTP Server Manager
	 */
	private class HttpServerManager {
		private DisposableServer httpServer;

		/**
		 * Start HTTP server
		 * @param combinedRouter Combined router function
		 */
		public void startHttpServer(RouterFunction<?> combinedRouter) {
			if (combinedRouter == null) {
				log.warn("No router functions created, server may not function normally");
				return;
			}

			// Create HTTP handler
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// Start HTTP server
			this.httpServer = HttpServer.create().port(EndPointUtils.SERVICE_PORT).handle(adapter).bindNow();

			log.info("HTTP server started, listening on port: {}", EndPointUtils.SERVICE_PORT);
			log.info("Server address: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);
		}

		/**
		 * Recreate HTTP server
		 * @param registeredTools Registered tools map
		 */
		public void recreateHttpServer(Map<String, List<CoordinatorTool>> registeredTools) {
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
				RouterFunction<?> combinedRouter = mcpServerManager.createCombinedRouter(registeredTools);

				if (combinedRouter == null) {
					log.warn("No router functions created, cannot recreate HTTP server");
					return;
				}

				// Start new HTTP server
				startHttpServer(combinedRouter);

				log.info("Successfully recreated HTTP server, listening on port: {}", EndPointUtils.SERVICE_PORT);

			}
			catch (Exception e) {
				log.error("Exception occurred while recreating HTTP server: {}", e.getMessage(), e);
			}
		}
	}

}
