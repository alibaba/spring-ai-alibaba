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

package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 智能Agent配置属性
 *
 * @author Makoto
 * @since 2025/07/17
 */
@Component
@ConfigurationProperties(prefix = "spring.ai.alibaba.deepresearch.smart-agents")
public class SmartAgentProperties {

	private boolean enabled = true;

	private Map<String, SearchPlatformConfig> searchPlatformMapping;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Map<String, SearchPlatformConfig> getSearchPlatformMapping() {
		return searchPlatformMapping;
	}

	public void setSearchPlatformMapping(Map<String, SearchPlatformConfig> searchPlatformMapping) {
		this.searchPlatformMapping = searchPlatformMapping;
	}

	/**
	 * 搜索平台配置
	 */
	public static class SearchPlatformConfig {

		private String primary;

		public String getPrimary() {
			return primary;
		}

		public void setPrimary(String primary) {
			this.primary = primary;
		}

	}

}
