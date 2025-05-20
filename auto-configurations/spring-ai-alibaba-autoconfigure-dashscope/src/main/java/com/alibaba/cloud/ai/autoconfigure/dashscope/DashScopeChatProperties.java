/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

@ConfigurationProperties(DashScopeChatProperties.CONFIG_PREFIX)
public class DashScopeChatProperties extends DashScopeParentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.chat";

	/**
	 * Default DashScope Chat model.
	 */
	public static final String DEFAULT_DEPLOYMENT_NAME = "qwen-plus";

	/**
	 * Default temperature speed.
	 */
	private static final Double DEFAULT_TEMPERATURE = 0.8d;

	/**
	 * Enable Dashscope ai chat client.
	 */
	private boolean enabled = true;

	@NestedConfigurationProperty
	private DashScopeChatOptions options = DashScopeChatOptions.builder()
		.withModel(DEFAULT_DEPLOYMENT_NAME)
		.withTemperature(DEFAULT_TEMPERATURE)
		.build();

	public DashScopeChatProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
	}

	public DashScopeChatOptions getOptions() {

		return this.options;
	}

	public void setOptions(DashScopeChatOptions options) {

		this.options = options;
	}

	public boolean isEnabled() {

		return this.enabled;
	}

	public void setEnabled(boolean enabled) {

		this.enabled = enabled;
	}

}
