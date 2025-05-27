package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Allen Hu
 * @since 0.1.0
 */
@ConfigurationProperties(prefix = ObservationProperties.PREFIX)
public class ObservationProperties {

	public static final String PREFIX = DeepResearchProperties.PREFIX + ".observation";

	private boolean enabled;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

}
