/*
 * Copyright 2024 - 2024 the original author or authors.
 */

package com.alibaba.cloud.ai.example.manus.inhouse;

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
import org.springframework.web.reactive.function.server.RouterFunctions;

import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequest;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseUseTool;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.inhouse.registry.McpToolRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import io.modelcontextprotocol.spec.McpSchema.Tool;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * WebFlux Streamable MCP Server 组件
 *
 * 基于 MCP 0.11.0 的完整示例应用程序。 实现了完整的 MCP 服务器功能，包括： 1. HTTP 传输层 2. 多种工具回调实现 3. 流式响应支持 4.
 * 错误处理和日志
 */
@Component
public class WebFluxStreamableServerApplication implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(WebFluxStreamableServerApplication.class);

	private static final int PORT = 20881;

	private static final String CUSTOM_MESSAGE_ENDPOINT = "/mcp/message";

	/**
	 * 空的 JSON Schema 定义
	 */
	private static final String EMPTY_JSON_SCHEMA = """
			{
			"$schema": "http://json-schema.org/draft-07/schema#",
			"type": "object",
			"properties": {}
			}
			""";

	@Autowired
	private Environment environment;

	@Autowired(required = false)
	private DatabaseUseTool databaseUseTool;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private McpToolRegistry mcpToolRegistry;

	private DisposableServer httpServer;

	private Object mcpServer;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// 延迟启动 MCP 服务器，确保所有 Bean 都已初始化完成
		try {
			Thread.sleep(1000); // 等待 1 秒确保所有 Bean 初始化完成
		} catch (InterruptedException e) {
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
			log.info("JManus InHouse MCP 服务器");
			log.info("==========================================");
			log.info("启动 JManus InHouse MCP Server...");
			log.info("服务器将在端口 {} 上运行", PORT);
			log.info("MCP 消息端点: http://localhost:{}{}", PORT, CUSTOM_MESSAGE_ENDPOINT);

			// 创建 WebFlux Streamable Server Transport Provider
			WebFluxStreamableServerTransportProvider transportProvider = WebFluxStreamableServerTransportProvider
				.builder()
				.objectMapper(new ObjectMapper())
				.messageEndpoint(CUSTOM_MESSAGE_ENDPOINT)
				.build();

			// 使用自动注册发现所有工具
			List<McpServerFeatures.SyncToolSpecification> tools = mcpToolRegistry.discoverAndRegisterTools();

			// 创建 MCP 服务器
			McpServer.SyncSpecification<?> serverSpec = McpServer.sync(transportProvider)
				.serverInfo("jmanus-inhouse-mcp-server", "1.0.0")
				.capabilities(ServerCapabilities.builder().tools(true).logging().build())
				.tools(tools.toArray(new McpServerFeatures.SyncToolSpecification[0]));

			// 构建服务器
			this.mcpServer = serverSpec.build();

			// 创建 HTTP 处理器
			HttpHandler httpHandler = RouterFunctions.toHttpHandler(transportProvider.getRouterFunction());
			ReactorHttpHandlerAdapter adapter = new ReactorHttpHandlerAdapter(httpHandler);

			// 启动 HTTP 服务器
			this.httpServer = HttpServer.create().port(PORT).handle(adapter).bindNow();

			log.info("JManus InHouse MCP Server 已启动成功！");
			log.info("可用工具:");
			
			// 动态输出所有注册的工具
			tools.forEach(tool -> {
				String toolName = tool.tool().name();
				String description = tool.tool().description();
				log.info("  - {}: {}", toolName, description);
			});

		}
		catch (Exception e) {
			log.error("启动 MCP 服务器时发生错误: {}", e.getMessage(), e);
		}
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
			if (this.mcpServer != null) {
				try {
					if (this.mcpServer instanceof AutoCloseable) {
						((AutoCloseable) this.mcpServer).close();
					}
					log.info("MCP 服务器已停止");
				}
				catch (Exception e) {
					log.warn("关闭 MCP 服务器时出现异常: {}", e.getMessage());
				}
			}
		}
		catch (Exception e) {
			log.error("停止 MCP 服务器时发生错误: {}", e.getMessage(), e);
		}
	}

}