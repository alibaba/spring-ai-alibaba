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
 * MCP服务器应用
 *
 * 支持多endpoint的MCP服务器，每个endpoint对应一组工具 参考WebFluxStreamableServerApplication的多endpoint逻辑
 */
@Component
public class CoordinatorMCPServer implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(CoordinatorMCPServer.class);

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

	// 存储已注册的工具，按endpoint分组
	private Map<String, List<CoordinatorTool>> registeredTools = new ConcurrentHashMap<>();

	// 存储已注册的MCP服务器，按endpoint分组
	private Map<String, Object> registeredMcpServers = new ConcurrentHashMap<>();

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过MCP服务器启动");
			return;
		}

		// 延迟启动 MCP 服务器，确保所有 Bean 都已初始化完成
		try {
			Thread.sleep(1000); // 等待 1 秒确保所有 Bean 初始化完成
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		startMcpServer();
	}

	/**
	 * 启动 MCP 服务器
	 */
	private void startMcpServer() {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过MCP服务器启动");
			return;
		}

		try {
			log.info("==========================================");
			log.info("JManus Multi EndPoint Streamable Http Server");
			log.info("==========================================");
			log.info("启动 JManus Multi EndPoint Streamable Http Server...");

			log.info("服务器信息:");
			log.info("  完整地址: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);

			// 加载协调器工具
			Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint = coordinatorService.loadCoordinatorTools();

			// 合并所有路由函数
			RouterFunction<?> combinedRouter = createCombinedRouter(coordinatorToolsByEndpoint);

			if (combinedRouter == null) {
				log.warn("没有创建任何路由函数，服务器可能无法正常工作");
			}
			else {
				log.info("成功创建合并路由函数");
			}

			// 创建 HTTP 处理器
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// 启动 HTTP 服务器
			this.httpServer = HttpServer.create().port(EndPointUtils.SERVICE_PORT).handle(adapter).bindNow();

			log.info("HTTP服务器已启动，监听端口: {}", EndPointUtils.SERVICE_PORT);
			log.info("服务器地址: http://{}:{}", EndPointUtils.SERVICE_HOST, EndPointUtils.SERVICE_PORT);

			log.info("JManus Multi EndPoint Streamable Http Server 已启动成功！");
			log.info("==========================================");
			log.info("MCP服务列表:");
			log.info("==========================================");

			// 输出所有MCP服务信息
			if (!coordinatorToolsByEndpoint.isEmpty()) {
				int serviceIndex = 1;
				for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
					String endpoint = entry.getKey();
					List<CoordinatorTool> tools = entry.getValue();
					log.info("  完整URL: {}", EndPointUtils.getUrl(endpoint));
					log.info("  工具数量: {}", tools.size());

					// 输出该endpoint下的所有工具
					for (int i = 0; i < tools.size(); i++) {
						CoordinatorTool tool = tools.get(i);
						log.info("    工具 #{}: {} - {}", i + 1, tool.getToolName(), tool.getToolDescription());
					}
					log.info("  ----------------------------------------");
				}
			}
			else {
				log.info("未找到任何MCP服务");
			}

			log.info("==========================================");
			log.info("MCP服务启动完成，共 {} 个endpoint", coordinatorToolsByEndpoint.size());
			log.info("==========================================");

		}
		catch (Exception e) {
			log.error("启动服务器时发生错误: {}", e.getMessage(), e);
		}
	}

	/**
	 * 创建合并的路由函数
	 */
	private RouterFunction<?> createCombinedRouter(Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint) {

		log.info("开始创建合并路由函数，共有 {} 个endpoint", coordinatorToolsByEndpoint.size());

		RouterFunction<?> combinedRouter = null;

		// 为每个协调器endpoint创建独立的传输提供者和服务器
		for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
			String endpoint = entry.getKey();
			List<CoordinatorTool> tools = entry.getValue();

			if (tools.isEmpty()) {
				continue;
			}

			// 将工具注册到内部存储
			registeredTools.put(endpoint, new ArrayList<>(tools));

			// 创建MCP服务器和路由函数
			RouterFunction<?> routerFunction = createMcpServerAndGetRouter(endpoint, tools);
			if (routerFunction != null) {
				if (combinedRouter == null) {
					combinedRouter = routerFunction;
				}
				else {
					combinedRouter = combinedRouter.andOther(routerFunction);
				}
			}

			log.info("为endpoint {} 创建了MCP服务器，包含 {} 个工具", endpoint, tools.size());
		}

		return combinedRouter;
	}

	/**
	 * 创建MCP服务器并获取路由函数
	 * @param endpoint 端点地址
	 * @param tools 工具列表
	 * @return 路由函数
	 */
	private RouterFunction<?> createMcpServerAndGetRouter(String endpoint, List<CoordinatorTool> tools) {
		try {
			// 构建messageEndpoint，增加默认前缀/mcp
			String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

			// 创建传输提供者
			WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
				.builder()
				.objectMapper(objectMapper)
				.messageEndpoint(messageEndpoint)
				.build();

			// 创建工具规范
			List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
			for (CoordinatorTool tool : tools) {
				toolSpecs.add(coordinatorService.createToolSpecification(tool));
			}

			// 创建MCP服务器
			McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
				.serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).logging().build())
				.tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

			Object mcpServer = serverSpec.build();

			// 存储MCP服务器
			registeredMcpServers.put(endpoint, mcpServer);
			mcpServers.add(mcpServer);

			log.info("成功为endpoint: {} 创建MCP服务器，包含 {} 个工具", endpoint, tools.size());

			// 返回路由函数
			return transportProvider.getRouterFunction();

		}
		catch (Exception e) {
			log.error("为endpoint: {} 创建MCP服务器时发生异常: {}", endpoint, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * 注册CoordinatorTool到MCP服务器
	 * @param tool 要注册的协调器工具
	 * @return 是否注册成功
	 */
	public boolean registerCoordinatorTool(CoordinatorTool tool) {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过工具注册");
			return false;
		}

		if (tool == null) {
			log.warn("CoordinatorTool为空，无法注册");
			return false;
		}

		String endpoint = tool.getEndpoint();
		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("CoordinatorTool的endpoint为空，无法注册");
			return false;
		}

		try {
			log.info("开始注册CoordinatorTool: {} 到endpoint: {}", tool.getToolName(), endpoint);

			// 获取或创建该endpoint的工具列表
			List<CoordinatorTool> toolsForEndpoint = registeredTools.computeIfAbsent(endpoint, k -> new ArrayList<>());

			// 检查工具是否已经注册，如果已注册则先删除旧版本
			boolean alreadyRegistered = toolsForEndpoint.stream()
				.anyMatch(existingTool -> existingTool.getToolName().equals(tool.getToolName()));

			if (alreadyRegistered) {
				log.info("CoordinatorTool: {} 已经注册到endpoint: {}，将更新为新的服务注册", tool.getToolName(), endpoint);
				// 删除旧版本的工具
				toolsForEndpoint.removeIf(existingTool -> existingTool.getToolName().equals(tool.getToolName()));
			}

			// 添加新工具到列表（无论是新增还是更新）
			toolsForEndpoint.add(tool);
			log.info("成功添加CoordinatorTool: {} 到endpoint: {} 的工具列表", tool.getToolName(), endpoint);

			// 检查该endpoint是否已经有MCP服务器
			Object existingMcpServer = registeredMcpServers.get(endpoint);
			if (existingMcpServer != null) {
				log.info("endpoint: {} 已有MCP服务器，需要重新创建以包含新工具", endpoint);
				// 重新创建该endpoint的MCP服务器
				recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);
			}
			else {
				log.info("endpoint: {} 没有MCP服务器，创建新的MCP服务器", endpoint);
				// 创建新的MCP服务器
				createMcpServerForEndpoint(endpoint, toolsForEndpoint);
			}

			log.info("成功注册CoordinatorTool: {} 到endpoint: {}", tool.getToolName(), endpoint);
			log.info("MCP服务访问信息:");
			log.info("  完整URL: {}", EndPointUtils.getUrl(endpoint));

			return true;

		}
		catch (Exception e) {
			log.error("注册CoordinatorTool时发生异常: {}", e.getMessage(), e);
			return false;
		}
	}

	/**
	 * 为指定endpoint创建MCP服务器
	 * @param endpoint 端点地址
	 * @param tools 该端点的工具列表
	 */
	private void createMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过创建MCP服务器");
			return;
		}

		try {
			// 构建messageEndpoint，增加默认前缀/mcp
			String messageEndpoint = EndPointUtils.buildMessageEndpoint(endpoint);

			// 创建传输提供者
			WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
				.builder()
				.objectMapper(objectMapper)
				.messageEndpoint(messageEndpoint)
				.build();

			// 创建工具规范
			List<McpServerFeatures.SyncToolSpecification> toolSpecs = new ArrayList<>();
			for (CoordinatorTool tool : tools) {
				toolSpecs.add(coordinatorService.createToolSpecification(tool));
			}

			// 创建MCP服务器
			McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
				.serverInfo("jmanus-coordinator-server-" + endpoint, "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).logging().build())
				.tools(toolSpecs.toArray(new McpServerFeatures.SyncToolSpecification[0]));

			Object mcpServer = serverSpec.build();

			// 存储MCP服务器
			registeredMcpServers.put(endpoint, mcpServer);
			mcpServers.add(mcpServer);

			log.info("成功为endpoint: {} 创建MCP服务器，包含 {} 个工具", endpoint, tools.size());

			// 重新创建HTTP服务器以更新路由
			recreateHttpServer();

		}
		catch (Exception e) {
			log.error("为endpoint: {} 创建MCP服务器时发生异常: {}", endpoint, e.getMessage(), e);
		}
	}

	/**
	 * 重新创建指定endpoint的MCP服务器
	 * @param endpoint 端点地址
	 * @param tools 该端点的工具列表
	 */
	private void recreateMcpServerForEndpoint(String endpoint, List<CoordinatorTool> tools) {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过重新创建MCP服务器");
			return;
		}

		try {
			log.info("开始重新创建endpoint: {} 的MCP服务器", endpoint);

			// 删除旧的MCP服务器
			Object oldMcpServer = registeredMcpServers.remove(endpoint);
			if (oldMcpServer != null) {
				mcpServers.remove(oldMcpServer);
				if (oldMcpServer instanceof AutoCloseable) {
					try {
						((AutoCloseable) oldMcpServer).close();
						log.info("已关闭旧的MCP服务器: {}", endpoint);
					}
					catch (Exception e) {
						log.warn("关闭旧的MCP服务器时出现异常: {}", e.getMessage());
					}
				}
			}

			// 创建新的MCP服务器
			createMcpServerForEndpoint(endpoint, tools);

			log.info("成功重新创建endpoint: {} 的MCP服务器", endpoint);

		}
		catch (Exception e) {
			log.error("重新创建endpoint: {} 的MCP服务器时发生异常: {}", endpoint, e.getMessage(), e);
		}
	}

	/**
	 * 重新创建HTTP服务器
	 */
	private void recreateHttpServer() {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过重新创建HTTP服务器");
			return;
		}

		try {
			log.info("开始重新创建HTTP服务器以更新路由");

			// 停止当前HTTP服务器
			if (this.httpServer != null) {
				this.httpServer.disposeNow();
				log.info("已停止当前HTTP服务器");
			}

			// 重新创建合并路由
			RouterFunction<?> combinedRouter = createCombinedRouter(registeredTools);

			if (combinedRouter == null) {
				log.warn("没有创建任何路由函数，无法重新创建HTTP服务器");
				return;
			}

			// 创建新的HTTP处理器
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// 启动新的HTTP服务器
			this.httpServer = HttpServer.create().port(EndPointUtils.SERVICE_PORT).handle(adapter).bindNow();

			log.info("成功重新创建HTTP服务器，监听端口: {}", EndPointUtils.SERVICE_PORT);

		}
		catch (Exception e) {
			log.error("重新创建HTTP服务器时发生异常: {}", e.getMessage(), e);
		}
	}

	/**
	 * 强制刷新特定工具
	 * @param toolName 工具名称
	 * @param updatedTool 更新后的工具
	 * @return 是否刷新成功
	 */
	public boolean refreshTool(String toolName, CoordinatorTool updatedTool) {
		// 检查是否启用CoordinatorTool功能
		if (!coordinatorToolProperties.isEnabled()) {
			log.info("CoordinatorTool功能已禁用，跳过工具刷新");
			return false;
		}

		if (updatedTool == null || toolName == null) {
			log.warn("工具或工具名称为空，无法刷新");
			return false;
		}

		String endpoint = updatedTool.getEndpoint();
		if (endpoint == null || endpoint.trim().isEmpty()) {
			log.warn("工具的endpoint为空，无法刷新");
			return false;
		}

		try {
			log.info("开始强制刷新工具: {} 在endpoint: {}", toolName, endpoint);

			// 获取该endpoint的工具列表
			List<CoordinatorTool> toolsForEndpoint = registeredTools.get(endpoint);
			if (toolsForEndpoint == null) {
				log.warn("endpoint: {} 没有找到工具列表", endpoint);
				return false;
			}

			// 查找并替换工具
			boolean found = false;
			for (int i = 0; i < toolsForEndpoint.size(); i++) {
				if (toolsForEndpoint.get(i).getToolName().equals(toolName)) {
					toolsForEndpoint.set(i, updatedTool);
					found = true;
					log.info("找到并替换了工具: {}", toolName);
					break;
				}
			}

			if (!found) {
				log.warn("在endpoint: {} 中没有找到工具: {}", endpoint, toolName);
				return false;
			}

			// 重新创建MCP服务器
			recreateMcpServerForEndpoint(endpoint, toolsForEndpoint);

			log.info("成功刷新工具: {} 在endpoint: {}", toolName, endpoint);
			return true;

		}
		catch (Exception e) {
			log.error("刷新工具时发生异常: {}", e.getMessage(), e);
			return false;
		}
	}

}
