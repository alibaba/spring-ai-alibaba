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

package com.alibaba.cloud.ai.example.manus.inhouse.mcp.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

import com.alibaba.cloud.ai.example.manus.inhouse.mcp.service.CoordinatorService;
import com.alibaba.cloud.ai.example.manus.inhouse.mcp.tool.coordinator.CoordinatorTool;

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
public class MultiEndPointStreamableHttpServer implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(MultiEndPointStreamableHttpServer.class);

	private static final int PORT = 20881;

	@Autowired
	private Environment environment;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CoordinatorService coordinatorService;

	private DisposableServer httpServer;

	private List<Object> mcpServers = new ArrayList<>();

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
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
		try {
			log.info("==========================================");
			log.info("JManus Multi EndPoint Streamable Http Server");
			log.info("==========================================");
			log.info("启动 JManus Multi EndPoint Streamable Http Server...");
			
			// 获取服务器信息
			String host = "localhost"; // 默认主机名
			String port = String.valueOf(PORT);
			
			// 尝试从环境变量获取主机名
			String serverHost = environment.getProperty("server.host", host);
			String serverPort = environment.getProperty("server.port", port);
			
			log.info("服务器信息:");
			log.info("  域名: {}", serverHost);
			log.info("  端口: {}", serverPort);
			log.info("  完整地址: http://{}:{}", serverHost, serverPort);

			// 加载协调器工具
			Map<String, List<CoordinatorTool>> coordinatorToolsByEndpoint = coordinatorService.loadCoordinatorTools();

			// 合并所有路由函数
			RouterFunction<?> combinedRouter = createCombinedRouter(coordinatorToolsByEndpoint);

			// 创建 HTTP 处理器
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(combinedRouter);
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// 启动 HTTP 服务器
			this.httpServer = HttpServer.create().port(PORT).handle(adapter).bindNow();

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
					
					log.info("服务 #{}:", serviceIndex++);
					log.info("  域名: {}", serverHost);
					log.info("  端口: {}", serverPort);
					log.info("  Endpoint: {}", endpoint);
					log.info("  完整URL: http://{}:{}{}", serverHost, serverPort, endpoint);
					log.info("  工具数量: {}", tools.size());
					
					// 输出该endpoint下的所有工具
					for (int i = 0; i < tools.size(); i++) {
						CoordinatorTool tool = tools.get(i);
						log.info("    工具 #{}: {} - {}", i + 1, tool.getToolName(), tool.getToolDescription());
					}
					log.info("  ----------------------------------------");
				}
			} else {
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

		RouterFunction<?> combinedRouter = null;

		// 为每个协调器endpoint创建独立的传输提供者和服务器
		for (Map.Entry<String, List<CoordinatorTool>> entry : coordinatorToolsByEndpoint.entrySet()) {
			String endpoint = entry.getKey();
			List<CoordinatorTool> tools = entry.getValue();

			if (tools.isEmpty()) {
				continue;
			}

			// 创建传输提供者
			WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
				.builder()
				.objectMapper(objectMapper)
				.messageEndpoint(endpoint)
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
			mcpServers.add(mcpServer);

			// 合并路由函数
			RouterFunction<?> routerFunction = transportProvider.getRouterFunction();
			if (combinedRouter == null) {
				combinedRouter = routerFunction;
			}
			else {
				combinedRouter = combinedRouter.andOther(routerFunction);
			}

			log.info("为endpoint {} 创建了MCP服务器，包含 {} 个工具", endpoint, tools.size());
		}

		return combinedRouter;
	}

	/**
	 * 停止 MCP 服务器
	 */
	public void stopMcpServer() {
		try {
			if (this.httpServer != null) {
				this.httpServer.disposeNow();
				log.info("HTTP 服务器已停止");
			}

			// 停止所有MCP服务器
			for (Object mcpServer : mcpServers) {
				if (mcpServer instanceof AutoCloseable) {
					try {
						((AutoCloseable) mcpServer).close();
						log.info("MCP 服务器已停止");
					}
					catch (Exception e) {
						log.warn("关闭 MCP 服务器时出现异常: {}", e.getMessage());
					}
				}
			}
			mcpServers.clear();
		}
		catch (Exception e) {
			log.error("停止 MCP 服务器时发生错误: {}", e.getMessage(), e);
		}
	}

}
