package com.alibaba.cloud.ai.oltp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author 肖云涛
 * @date 2024/12/5
 */
@Configuration
@ConfigurationProperties(StudioObservabilityProperties.CONFIG_PREFIX)
public class StudioObservabilityProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.studio";

	private boolean enabled;

	private String outputFile;

	public boolean isEnabled() {
		return this.enabled;
	}

	public String getOutputFile() {
		return this.outputFile;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile;
	}

	public StudioObservabilityProperties() {
		this.enabled = true;
		this.outputFile = "spring-ai-alibaba-studio/spans.json";
	}

}
