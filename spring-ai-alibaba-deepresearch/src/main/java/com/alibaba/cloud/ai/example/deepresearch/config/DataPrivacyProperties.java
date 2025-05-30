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

/**
 * 数据隐私保护配置属性
 * 
 * 注意：从sensitivefilter支持自定义正则表达式后，自定义模式配置已移至sensitivefilter模块
 * 此配置类主要用于控制DeepResearch工作流中的数据脱敏行为
 *
 * @author deepresearch
 * @since 2025/1/15
 */
@ConfigurationProperties(prefix = DataPrivacyProperties.PREFIX)
public class DataPrivacyProperties {

	public static final String PREFIX = "spring.ai.alibaba.deepreserch.data-privacy";

	/**
	 * 是否启用数据隐私保护
	 */
	private boolean enabled = true;

	/**
	 * 是否自动过滤输出内容
	 */
	private boolean autoFilterOutput = true;

	/**
	 * 是否过滤中间结果
	 */
	private boolean filterIntermediateResults = true;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isAutoFilterOutput() {
		return autoFilterOutput;
	}

	public void setAutoFilterOutput(boolean autoFilterOutput) {
		this.autoFilterOutput = autoFilterOutput;
	}

	public boolean isFilterIntermediateResults() {
		return filterIntermediateResults;
	}

	public void setFilterIntermediateResults(boolean filterIntermediateResults) {
		this.filterIntermediateResults = filterIntermediateResults;
	}

} 