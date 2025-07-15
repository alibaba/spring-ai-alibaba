package com.alibaba.cloud.ai.mcp.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * @author yingzi
 * @since 2025/7/15
 */
@ConfigurationProperties(prefix = McpRecoveryProperties.CONFIG_PREFIX)
public class McpRecoveryProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.mcp.recovery";

	private Duration delay = Duration.ofSeconds(5L);

	private Duration stop = Duration.ofSeconds(10L);

	public Duration getDelay() {
		return delay;
	}

	public void setDelay(Duration delay) {
		this.delay = delay;
	}

	public Duration getStop() {
		return stop;
	}

	public void setStop(Duration stop) {
		this.stop = stop;
	}

}
