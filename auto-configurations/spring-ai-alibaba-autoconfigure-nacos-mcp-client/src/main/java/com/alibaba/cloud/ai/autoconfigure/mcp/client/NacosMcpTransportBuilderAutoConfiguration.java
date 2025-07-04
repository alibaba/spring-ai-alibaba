package com.alibaba.cloud.ai.autoconfigure.mcp.client;

import com.alibaba.cloud.ai.mcp.nacos.client.builder.WebFluxSseClientTransportBuilder;
import io.modelcontextprotocol.client.transport.WebFluxSseClientTransport;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

/**
 * @author Sunrisea
 * @since 2025/7/4 11:16
 */
@AutoConfiguration(before = { NacosMcpClientAutoConfiguration.class })
@ConditionalOnClass(WebFluxSseClientTransport.class)
public class NacosMcpTransportBuilderAutoConfiguration {

	@Bean
	public WebFluxSseClientTransportBuilder webFluxSseClientTransportBuilder() {
		return new WebFluxSseClientTransportBuilder();
	}

}
