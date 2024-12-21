package com.alibaba.cloud.ai.autoconfigure.prompt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@ConfigurationProperties(NacosPromptTmplProperties.TEMPLATE_PREFIX)
public class NacosPromptTmplProperties {

	public final static String TEMPLATE_PREFIX = "spring.ai.nacos.prompt.template";

	/**
	 * Default not enabled.
	 */
	private boolean enabled = false;

	public boolean isEnabled() {

		return enabled;
	}

	public void setEnabled(boolean enabled) {

		this.enabled = enabled;
	}

}
