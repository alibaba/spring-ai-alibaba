// /*
// * Copyright 2025 the original author or authors.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// * https://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */

// package com.alibaba.cloud.ai.manus.coordinator.server;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.function.Supplier;

// import com.alibaba.cloud.ai.manus.coordinator.tool.EndPointUtils;
// import com.alibaba.cloud.ai.manus.config.CoordinatorProperties;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.context.event.ApplicationReadyEvent;
// import org.springframework.context.ApplicationListener;
// import org.springframework.context.annotation.Lazy;
// import org.springframework.core.env.Environment;
// import org.springframework.http.server.reactive.HttpHandler;
// import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
// import org.springframework.stereotype.Component;
// import org.springframework.web.reactive.function.server.RouterFunction;
// import org.springframework.web.reactive.function.server.RouterFunctions;

// import com.fasterxml.jackson.databind.ObjectMapper;

// import com.alibaba.cloud.ai.manus.coordinator.service.CoordinatorService;
// import com.alibaba.cloud.ai.manus.coordinator.tool.CoordinatorTool;

// import io.modelcontextprotocol.server.McpServer;
// import io.modelcontextprotocol.server.McpServerFeatures;
// import
// io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
// import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
// import reactor.netty.DisposableServer;
// import reactor.netty.http.server.HttpServer;

// /**
// * Coordinator Server Application
// *
// * Supports multi-endpoint coordinator server, each endpoint corresponds to a group of
// * tools Reference WebFluxStreamableServerApplication's multi-endpoint logic
// */
// @Component
// public class CoordinatorServer implements ApplicationListener<ApplicationReadyEvent> {

// private static final Logger log = LoggerFactory.getLogger(CoordinatorServer.class);

// // ==================== Log Constants ====================
// private static final String LOG_SEPARATOR =
// "==========================================";

// private static final String LOG_SERVER_TITLE = "JManus Multi EndPoint Streamable Http
// Server";

// private static final String LOG_SERVICE_LIST_TITLE = "Coordinator Service List:";

// private static final String LOG_TOOL_FORMAT = " Tool #{}: {} - {}";

// private static final String LOG_URL_FORMAT = " Full URL: {}";

// private static final String LOG_COUNT_FORMAT = " Tool Count: {}";

// private static final String LOG_DIVIDER = " ----------------------------------------";

// private static final String LOG_NO_SERVICES = "No coordinator services found";

// private static final String LOG_STARTUP_COMPLETE = "Coordinator service startup
// complete, {} endpoints";

// @Autowired
// private Environment environment;

// @Autowired
// private ObjectMapper objectMapper;

// @Autowired
// @Lazy
// private CoordinatorService coordinatorService;

// @Autowired
// private CoordinatorProperties coordinatorProperties;

// // Internal component: MCP server manager
// private final McpServerManager mcpServerManager;

// // Internal component: Tool registry manager
// private final ToolRegistryManager toolRegistryManager;

// // Internal component: HTTP server manager
// private final HttpServerManager httpServerManager;

// public CoordinatorServer() {
// this.mcpServerManager = new McpServerManager();
// this.toolRegistryManager = new ToolRegistryManager();
// this.httpServerManager = new HttpServerManager();
// }

// @Override
// public void onApplicationEvent(ApplicationReadyEvent event) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping coordinator server startup");
// return;
// }

// // Delay coordinator server startup to ensure all Beans are initialized
// try {
// Thread.sleep(1000); // Wait 1 second to ensure all Beans are initialized
// }
// catch (InterruptedException e) {
// Thread.currentThread().interrupt();
// }
// startCoordinatorServer();
// }

// /**
// * Start coordinator server
// */
// private void startCoordinatorServer() {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping coordinator server startup");
// return;
// }

// try {
// logServerStartup();

// // Load coordinator tools
// Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint =
// coordinatorService.loadCoordinatorTools();

// // Combine all router functions
// RouterFunction<?> combinedRouter =
// mcpServerManager.createCombinedRouter(coordinatorToolsByEndpoint);

// // Start HTTP server
// httpServerManager.startHttpServer(combinedRouter);

// logServerStartupComplete(coordinatorToolsByEndpoint);

// }
// catch (Exception e) {
// log.error("Error starting server: {}", e.getMessage(), e);
// }
// }

// /**
// * Register CoordinatorTool to coordinator server
// * @param tool Coordinator tool to register
// * @return Whether registration was successful
// */
// public boolean registerCoordinatorTool(CoordinatorTool tool) {
// if (tool == null || tool.getEndpoint() == null || tool.getEndpoint().trim().isEmpty())
// {
// log.warn("Invalid tool parameters");
// return false;
// }

// return executeWithValidation("tool registration",
// () -> toolRegistryManager.registerTool(tool, mcpServerManager, httpServerManager),
// tool.getToolName(),
// tool.getEndpoint()) != null;
// }

// /**
// * Unregister CoordinatorTool from coordinator server
// * @param toolName Tool name to unregister
// * @param endpoint Endpoint address
// * @return Whether unregistration was successful
// */
// public boolean unregisterCoordinatorTool(String toolName, String endpoint) {
// if (toolName == null || toolName.trim().isEmpty() || endpoint == null ||
// endpoint.trim().isEmpty()) {
// log.warn("Invalid parameters");
// return false;
// }

// return executeWithValidation("tool unregistration",
// () -> toolRegistryManager.unregisterTool(toolName, endpoint, mcpServerManager,
// httpServerManager),
// toolName, endpoint) != null;
// }

// /**
// * Forcefully refresh a specific tool
// * @param toolName Tool name
// * @param updatedTool Updated tool
// * @return Whether refresh was successful
// */
// public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
// if (updatedTool == null || toolName == null || updatedTool.getEndpoint() == null
// || updatedTool.getEndpoint().trim().isEmpty()) {
// log.warn("Invalid tool parameters");
// return false;
// }

// return executeWithValidation("tool refresh",
// () -> toolRegistryManager.refreshTool(toolName, updatedTool, mcpServerManager,
// httpServerManager),
// toolName, updatedTool.getEndpoint()) != null;
// }

// // ==================== Execution Template Methods ====================

// /**
// * Execution template method, unified handling of validation, logging and exceptions
// * @param operation Operation name
// * @param operationSupplier Operation execution function
// * @param operationParams Operation parameters
// * @return Operation result
// */
// private <T> T executeWithValidation(String operation, Supplier<T> operationSupplier,
// String... operationParams) {
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping {}", operation);
// return null;
// }

// try {
// log.info("Starting {}: {}", operation, String.join(", ", operationParams));
// T result = operationSupplier.get();
// log.info("Successfully completed {}: {}", operation, String.join(", ",
// operationParams));
// return result;
// }
// catch (Exception e) {
// log.error("Exception occurred during {}: {}", operation, e.getMessage(), e);
// return null;
// }
// }

// // ==================== Helper Methods ====================

// /**
// * Log server startup information
// */
// private void logServerStartup() {
// log.info(LOG_SEPARATOR);
// log.info(LOG_SERVER_TITLE);
// log.info(LOG_SEPARATOR);
// log.info("Starting {}...", LOG_SERVER_TITLE);

// log.info("Server Information:");
// log.info(" Full Address: http://{}:{}", EndPointUtils.SERVICE_HOST,
// EndPointUtils.SERVICE_PORT);
// }

// /**
// * Log server startup completion information
// */
// private void logServerStartupComplete(Map<String, List<CoordinatorTool>>
// coordinatorToolsByEndpoint) {
// log.info("{} started successfully!", LOG_SERVER_TITLE);
// log.info(LOG_SEPARATOR);
// log.info(LOG_SERVICE_LIST_TITLE);
// log.info(LOG_SEPARATOR);

// // Output all coordinator service information
// if (!coordinatorToolsByEndpoint.isEmpty()) {
// for (Map.Entry<String, List<CoordinatorTool>> entry :
// coordinatorToolsByEndpoint.entrySet()) {
// String endpoint = entry.getKey();
// List<CoordinatorTool> tools = entry.getValue();
// log.info(LOG_URL_FORMAT, EndPointUtils.getUrl(endpoint));
// log.info(LOG_COUNT_FORMAT, tools.size());

// // Output all tools for this endpoint
// for (int i = 0; i < tools.size(); i++) {
// CoordinatorTool tool = tools.get(i);
// log.info(LOG_TOOL_FORMAT, i + 1, tool.getToolName(), tool.getToolDescription());
// }
// log.info(LOG_DIVIDER);
// }
// }
// else {
// log.info(LOG_NO_SERVICES);
// }

// log.info(LOG_SEPARATOR);
// log.info(LOG_STARTUP_COMPLETE, coordinatorToolsByEndpoint.size());
// log.info(LOG_SEPARATOR);
// }

// // ==================== Internal Component Classes ====================

// /**
// * MCP Server Manager
// */
// private class McpServerManager {

// private final Map<String, Object> registeredMcpServers = new ConcurrentHashMap<>();

// private final List<Object> mcpServers = new ArrayList<>();

// /**
// * Create combined router functions
// */
// public RouterFunction<?> createCombinedRouter(Map<String, List<CoordinatorTool>>
// coordinatorToolsByEndpoint) {
// log.info("Starting to create combined router functions, {} endpoints",
// coordinatorToolsByEndpoint.size());

// RouterFunction<?> combinedRouter = null;

// // Create independent transport providers and servers for each coordinator
// // endpoint
// for (Map.Entry<String, List<CoordinatorTool>> entry :
// coordinatorToolsByEndpoint.entrySet()) {
// String endpoint = entry.getKey();
// List<CoordinatorTool> tools = entry.getValue();

// if (tools.isEmpty()) {
// continue;
// }

// // Register tools to internal storage
// toolRegistryManager.registeredTools.put(endpoint, new ArrayList<>(tools));

// // Create MCP server and router function
// RouterFunction<?> routerFunction = createMcpServerAndGetRouter(endpoint, tools);
// if (routerFunction != null) {
// if (combinedRouter == null) {
// combinedRouter = routerFunction;
// }
// else {
// combinedRouter = combinedRouter.andOther(routerFunction);
// }
// }

// log.info("Created MCP server for endpoint {}, containing {} tools", endpoint,
// tools.size());
// }

// return combinedRouter;
// }

// /**
// * Create MCP server and get router function
// * @param endpoint Endpoint address
// * @param tools Tool list
// * @return Router function
// */
// private RouterFunction<?> createMcpServerAndGetRouter(String endpoint,
// List<CoordinatorTool> tools) {
// try {
// // Build messageEndpoint, add default prefix /mcp
// String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

// // Create transport provider
// WebFluxStreamableServerTransportProvider transportProvider =
// WebFluxStreamableServerTransportProvider
// .builder()
// .objectMapper(objectMapper)
// .messageEndpoint(messageEndpoint)
// .build();

// // Create tool specification
// List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
// for (CoordinatorTool tool : tools) {
// toolSpecs.add(coordinatorService.createToolSpecification(tool));
// }

// // Create MCP server
// McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
// .serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
// .capabilities(ServerCapabilities.builder().tools(true).logging().build())
// .tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

// Object mcpServer = serverSpec.build();

// // Store MCP server
// registeredMcpServers.put(endpoint, mcpServer);
// mcpServers.add(mcpServer);

// log.info("Successfully created MCP server for endpoint: {}, containing {} tools",
// endpoint,
// tools.size());

// // Return router function
// return transportProvider.getRouterFunction();

// }
// catch (Exception e) {
// log.error("Exception occurred while creating MCP server for endpoint: {}, {}",
// endpoint, e.getMessage(),
// e);
// return null;
// }
// }

// /**
// * Create MCP server for a specific endpoint
// * @param endpoint Endpoint address
// * @param tools Tool list for this endpoint
// */
// public void createMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping MCP server creation");
// return;
// }

// try {
// // Build messageEndpoint, add default prefix /mcp
// String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

// // Create transport provider
// WebFluxStreamableServerTransportProvider transportProvider =
// WebFluxStreamableServerTransportProvider
// .builder()
// .objectMapper(objectMapper)
// .messageEndpoint(messageEndpoint)
// .build();

// // Create tool specification
// List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
// for (CoordinatorTool tool : tools) {
// toolSpecs.add(coordinatorService.createToolSpecification(tool));
// }

// // Create MCP server
// McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
// .serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
// .capabilities(ServerCapabilities.builder().tools(true).logging().build())
// .tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

// Object mcpServer = serverSpec.build();

// // Store MCP server
// registeredMcpServers.put(endpoint, mcpServer);
// mcpServers.add(mcpServer);

// log.info("Successfully created MCP server for endpoint: {}, containing {} tools",
// endpoint,
// tools.size());

// }
// catch (Exception e) {
// log.error("Exception occurred while creating MCP server for endpoint: {}, {}",
// endpoint, e.getMessage(),
// e);
// }
// }

// /**
// * Recreate MCP server for a specific endpoint
// * @param endpoint Endpoint address
// * @param tools Tool list for this endpoint
// */
// public void recreateMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools)
// {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping MCP server recreation");
// return;
// }

// try {
// // Remove existing MCP server
// removeMcpServer(endpoint);

// // Create new MCP server
// createMcpServerForEndpoint(endpoint, tools);

// log.info("Successfully recreated MCP server for endpoint: {}", endpoint);

// }
// catch (Exception e) {
// log.error("Exception occurred while recreating MCP server for endpoint: {}, {}",
// endpoint,
// e.getMessage(), e);
// }
// }

// /**
// * Remove MCP server for a specific endpoint
// * @param endpoint Endpoint address
// */
// public void removeMcpServer(String endpoint) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping MCP server removal");
// return;
// }

// try {
// Object mcpServer = registeredMcpServers.remove(endpoint);
// if (mcpServer != null) {
// closeMcpServer(mcpServer);
// mcpServers.remove(mcpServer);
// log.info("Successfully removed MCP server for endpoint: {}", endpoint);
// }
// else {
// log.warn("MCP server not found for endpoint: {}", endpoint);
// }

// }
// catch (Exception e) {
// log.error("Exception occurred while removing MCP server for endpoint: {}, {}",
// endpoint, e.getMessage(),
// e);
// }
// }

// /**
// * Close MCP server
// * @param mcpServer MCP server to close
// */
// private void closeMcpServer(Object mcpServer) {
// try {
// if (mcpServer instanceof AutoCloseable) {
// ((AutoCloseable) mcpServer).close();
// log.debug("Successfully closed MCP server");
// }
// }
// catch (Exception e) {
// log.warn("Exception occurred while closing MCP server: {}", e.getMessage());
// }
// }

// }

// /**
// * Tool Registry Manager
// */
// private class ToolRegistryManager {

// private final Map<String, List<CoordinatorTool>> registeredTools = new
// ConcurrentHashMap<>();

// /**
// * Register tool to coordinator server
// * @param tool Tool to register
// * @param mcpManager MCP server manager
// * @param httpManager HTTP server manager
// * @return Whether registration was successful
// */
// public boolean registerTool(CoordinatorTool tool, McpServerManager mcpManager,
// HttpServerManager httpManager) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping tool registration");
// return false;
// }

// try {
// String endpoint = tool.getEndpoint();
// List<CoordinatorTool> tools = registeredTools.get(endpoint);

// if (tools == null) {
// tools = new ArrayList<>();
// registeredTools.put(endpoint, tools);
// }

// // Check if tool already exists
// boolean toolExists = tools.stream()
// .anyMatch(existingTool -> existingTool.getToolName().equals(tool.getToolName()));

// if (toolExists) {
// log.warn("Tool {} already exists in endpoint {}, skipping registration",
// tool.getToolName(),
// endpoint);
// return false;
// }

// // Add tool to list
// tools.add(tool);

// // Recreate MCP server for this endpoint
// mcpManager.recreateMcpServerForEndpoint(endpoint, tools);

// // Recreate HTTP server
// httpManager.recreateHttpServer(registeredTools);

// log.info("Successfully registered tool {} to endpoint {}", tool.getToolName(),
// endpoint);
// return true;

// }
// catch (Exception e) {
// log.error("Exception occurred while registering tool: {}", e.getMessage(), e);
// return false;
// }
// }

// /**
// * Unregister tool from coordinator server
// * @param toolName Tool name to unregister
// * @param endpoint Endpoint address
// * @param mcpManager MCP server manager
// * @param httpManager HTTP server manager
// * @return Whether unregistration was successful
// */
// public boolean unregisterTool(String toolName, String endpoint, McpServerManager
// mcpManager,
// HttpServerManager httpManager) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping tool unregistration");
// return false;
// }

// try {
// List<CoordinatorTool> tools = registeredTools.get(endpoint);

// if (tools == null) {
// log.warn("No tools found for endpoint: {}", endpoint);
// return false;
// }

// // Remove tool from list
// boolean removed = tools.removeIf(tool -> tool.getToolName().equals(toolName));

// if (!removed) {
// log.warn("Tool {} not found in endpoint: {}", toolName, endpoint);
// return false;
// }

// // If no tools left for this endpoint, remove the endpoint
// if (tools.isEmpty()) {
// registeredTools.remove(endpoint);
// mcpManager.removeMcpServer(endpoint);
// }
// else {
// // Recreate MCP server for this endpoint
// mcpManager.recreateMcpServerForEndpoint(endpoint, tools);
// }

// // Recreate HTTP server
// httpManager.recreateHttpServer(registeredTools);

// log.info("Successfully unregistered tool {} from endpoint {}", toolName, endpoint);
// return true;

// }
// catch (Exception e) {
// log.error("Exception occurred while unregistering tool: {}", e.getMessage(), e);
// return false;
// }
// }

// /**
// * Refresh tool in coordinator server
// * @param toolName Tool name
// * @param updatedTool Updated tool
// * @param mcpManager MCP server manager
// * @param httpManager HTTP server manager
// * @return Whether refresh was successful
// */
// public boolean refreshTool(String toolName, CoordinatorTool updatedTool,
// McpServerManager mcpManager,
// HttpServerManager httpManager) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping tool refresh");
// return false;
// }

// try {
// String endpoint = updatedTool.getEndpoint();
// List<CoordinatorTool> tools = registeredTools.get(endpoint);

// if (tools == null) {
// log.warn("No tools found for endpoint: {}", endpoint);
// return false;
// }

// // Find and update tool
// boolean found = false;
// for (int i = 0; i < tools.size(); i++) {
// CoordinatorTool tool = tools.get(i);
// if (tool.getToolName().equals(toolName)) {
// tools.set(i, updatedTool);
// found = true;
// break;
// }
// }

// if (!found) {
// log.warn("Tool {} not found in endpoint: {}", toolName, endpoint);
// return false;
// }

// // Recreate MCP server for this endpoint
// mcpManager.recreateMcpServerForEndpoint(endpoint, tools);

// // Recreate HTTP server
// httpManager.recreateHttpServer(registeredTools);

// log.info("Successfully refreshed tool {} in endpoint {}", toolName, endpoint);
// return true;

// }
// catch (Exception e) {
// log.error("Exception occurred while refreshing tool: {}", e.getMessage(), e);
// return false;
// }
// }

// }

// /**
// * HTTP Server Manager
// */
// private class HttpServerManager {

// private DisposableServer httpServer;

// /**
// * Start HTTP server
// * @param combinedRouter Combined router function
// */
// public void startHttpServer(RouterFunction<?> combinedRouter) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping HTTP server startup");
// return;
// }

// try {
// // Create HTTP handler
// HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
// ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

// // Start HTTP server
// httpServer = HttpServer.create()
// .host(EndPointUtils.SERVICE_HOST)
// .port(EndPointUtils.SERVICE_PORT)
// .handle(adapter)
// .bindNow();

// log.info("Successfully started HTTP server on {}:{}", EndPointUtils.SERVICE_HOST,
// EndPointUtils.SERVICE_PORT);

// }
// catch (Exception e) {
// log.error("Exception occurred while starting HTTP server: {}", e.getMessage(), e);
// }
// }

// /**
// * Recreate HTTP server
// * @param registeredTools Registered tools
// */
// public void recreateHttpServer(Map<String, List<CoordinatorTool>> registeredTools) {
// // Check if CoordinatorTool feature is enabled
// if (!coordinatorProperties.isEnabled()) {
// log.info("CoordinatorTool feature is disabled, skipping HTTP server recreation");
// return;
// }

// try {
// // Stop existing HTTP server
// if (httpServer != null) {
// httpServer.disposeNow();
// log.debug("Successfully stopped existing HTTP server");
// }

// // Create combined router function
// RouterFunction<?> combinedRouter =
// mcpServerManager.createCombinedRouter(registeredTools);

// // Start new HTTP server
// startHttpServer(combinedRouter);

// log.info("Successfully recreated HTTP server");

// }
// catch (Exception e) {
// log.error("Exception occurred while recreating HTTP server: {}", e.getMessage(), e);
// }
// }

// }

// }
