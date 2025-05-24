package com.alibaba.cloud.ai.autoconfigure.dashscope;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

import com.alibaba.cloud.ai.dashscope.agent.DashScopeAgentOptions;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties(DashScopeAgentProperties.CONFIG_PREFIX)
public class DashScopeAgentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.agent";

	@NestedConfigurationProperty
	private DashScopeAgentOptions options = DashScopeAgentOptions.builder().build();

	public DashScopeAgentOptions getOptions() {

		return this.options;
	}

	public void setOptions(DashScopeAgentOptions options) {

		this.options = options;
	}

}
