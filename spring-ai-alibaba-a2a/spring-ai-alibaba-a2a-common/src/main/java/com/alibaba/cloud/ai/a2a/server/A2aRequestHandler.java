package com.alibaba.cloud.ai.a2a.server;

import io.a2a.spec.AgentCard;

import org.springframework.web.servlet.function.ServerRequest;

/**
 * A2a interface request handler.
 *
 * @author xiweng.yy
 */
public interface A2aRequestHandler {

	Object onHandler(String body, ServerRequest.Headers headers);

	AgentCard getAgentCard();

}
