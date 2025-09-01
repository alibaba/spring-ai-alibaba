package com.alibaba.cloud.ai.a2a.route;

import com.alibaba.cloud.ai.a2a.server.A2aRequestHandler;

import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * The router provider for A2A protocol.
 *
 * @author xiweng.yy
 */
public interface A2aRouterProvider<S extends A2aRequestHandler> {

	RouterFunction<ServerResponse> getRouter(S a2aRequestHandler);

}
