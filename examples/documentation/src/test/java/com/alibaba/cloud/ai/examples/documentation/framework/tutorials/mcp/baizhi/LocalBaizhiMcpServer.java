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

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServer;

/**
 * A local Streamable HTTP MCP server used only by tests.
 *
 * <p>
 * It verifies the real HTTP transport, session initialization, tool discovery, tool
 * dispatch and bearer authentication without contacting the hosted Baizhi service or a
 * model provider. All credentials are synthetic.
 *
 * @author Spring AI Alibaba
 */
final class LocalBaizhiMcpServer implements AutoCloseable {

	private static final String MCP_PATH = "/mcp";

	private final McpSyncServer mcpServer;

	private final WebServer webServer;

	private final AtomicReference<String> lastAuthorization = new AtomicReference<>();

	private final AtomicInteger requestCount = new AtomicInteger();

	private final AtomicInteger unauthorizedRequestCount = new AtomicInteger();

	LocalBaizhiMcpServer(String expectedBearerToken, List<McpServerFeatures.SyncToolSpecification> tools) {
		HttpServletStreamableServerTransportProvider transportProvider = HttpServletStreamableServerTransportProvider
			.builder()
			.mcpEndpoint(MCP_PATH)
			.build();
		this.mcpServer = McpServer.sync(transportProvider)
			.serverInfo("local-baizhi-mcp", "1.0.0")
			.capabilities(McpSchema.ServerCapabilities.builder().tools(true).build())
			.tools(tools)
			.build();

		TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory(0);
		this.webServer = factory.getWebServer(servletContext -> {
			FilterRegistration.Dynamic auth = servletContext.addFilter("bearer-auth", new BearerAuthFilter(
					expectedBearerToken, this.lastAuthorization, this.requestCount, this.unauthorizedRequestCount));
			auth.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, MCP_PATH);

			ServletRegistration.Dynamic servlet = servletContext.addServlet("mcp", transportProvider);
			servlet.setAsyncSupported(true);
			servlet.addMapping(MCP_PATH);
		});
		this.webServer.start();
	}

	String url() {
		return "http://127.0.0.1:" + this.webServer.getPort() + MCP_PATH;
	}

	String lastAuthorizationHeader() {
		return this.lastAuthorization.get();
	}

	int requestCount() {
		return this.requestCount.get();
	}

	int unauthorizedRequestCount() {
		return this.unauthorizedRequestCount.get();
	}

	@Override
	public void close() {
		try {
			this.mcpServer.close();
		}
		catch (RuntimeException ignored) {
			// Test teardown should still stop the servlet container.
		}
		this.webServer.stop();
	}

	static McpServerFeatures.SyncToolSpecification tool(String name, String description,
			Map<String, Object> properties, List<String> required,
			BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler) {
		McpSchema.Tool tool = McpSchema.Tool.builder()
			.name(name)
			.description(description)
			.inputSchema(new McpSchema.JsonSchema("object", properties, required, false, null, null))
			.build();
		return McpServerFeatures.SyncToolSpecification.builder()
			.tool(tool)
			.callHandler((exchange, request) -> handler.apply(exchange, request.arguments()))
			.build();
	}

	private static final class BearerAuthFilter implements Filter {

		private final String expectedBearerToken;

		private final AtomicReference<String> lastAuthorization;

		private final AtomicInteger requestCount;

		private final AtomicInteger unauthorizedRequestCount;

		private BearerAuthFilter(String expectedBearerToken, AtomicReference<String> lastAuthorization,
				AtomicInteger requestCount, AtomicInteger unauthorizedRequestCount) {
			this.expectedBearerToken = expectedBearerToken;
			this.lastAuthorization = lastAuthorization;
			this.requestCount = requestCount;
			this.unauthorizedRequestCount = unauthorizedRequestCount;
		}

		@Override
		public void init(FilterConfig filterConfig) {
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
				throws IOException, ServletException {
			this.requestCount.incrementAndGet();
			String authorization = ((HttpServletRequest) request).getHeader("Authorization");
			this.lastAuthorization.set(authorization);
			if (!("Bearer " + this.expectedBearerToken).equals(authorization)) {
				this.unauthorizedRequestCount.incrementAndGet();
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
				httpResponse.setContentType("application/json");
				httpResponse.getWriter().write("{\"error\":\"invalid synthetic token\"}");
				return;
			}
			chain.doFilter(request, response);
		}

		@Override
		public void destroy() {
		}

	}

}
