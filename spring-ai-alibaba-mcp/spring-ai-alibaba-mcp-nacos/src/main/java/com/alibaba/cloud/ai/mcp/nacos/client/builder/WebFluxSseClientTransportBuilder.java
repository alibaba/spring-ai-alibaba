package com.alibaba.cloud.ai.mcp.nacos.client.builder;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.springframework.web.reactive.function.client.WebClient;

public class WebFluxSseClientTransportBuilder {

	public WebFluxSseClientTransport build(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
			String sseEndpoint) {
		return WebFluxSseClientTransport.builder(webClientBuilder)
			.sseEndpoint(sseEndpoint)
			.objectMapper(objectMapper)
			.build();
	}

}
