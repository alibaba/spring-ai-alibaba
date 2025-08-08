/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CoordinatorTool 配置属性
 */
@Component
@ConfigurationProperties(prefix = "coordinator.tool")
public class CoordinatorToolProperties {

	/**
	 * 是否启用CoordinatorTool功能
	 */
	private boolean enabled = true;

	/**
	 * 是否显示发布MCP服务按钮
	 */
	private boolean showPublishButton = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isShowPublishButton() {
		return showPublishButton;
	}

	public void setShowPublishButton(boolean showPublishButton) {
		this.showPublishButton = showPublishButton;
	}

}