package com.alibaba.cloud.ai.autoconfigure.a2a.server;

import com.alibaba.cloud.ai.a2a.A2aServerProperties;
import com.alibaba.cloud.ai.a2a.route.JsonRpcA2aRouterProvider;
import com.alibaba.cloud.ai.a2a.server.JsonRpcA2aRequestHandler;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * The AutoConfiguration for A2A server.
 *
 * @author xiweng.yy
 */
@AutoConfiguration(after = A2aServerHandlerAutoConfiguration.class)
@EnableConfigurationProperties({ A2aServerProperties.class })
public class A2aServerAutoConfiguration {

	@Bean
	@ConditionalOnBean({ JsonRpcA2aRequestHandler.class })
	public RouterFunction<ServerResponse> a2aRouterFunction(A2aServerProperties a2aServerProperties,
			JsonRpcA2aRequestHandler a2aRequestHandler) {
		return new JsonRpcA2aRouterProvider(a2aServerProperties.getAgentCardUrl(), a2aServerProperties.getMessageUrl())
			.getRouter(a2aRequestHandler);
	}

}
