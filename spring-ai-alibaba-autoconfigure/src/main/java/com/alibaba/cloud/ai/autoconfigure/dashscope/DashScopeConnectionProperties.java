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

import org.springframework.boot.context.properties.ConfigurationProperties;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;
import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_READ_TIMEOUT;

/**
 * Spring AI Alibaba TongYi LLM connection properties.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

@ConfigurationProperties(DashScopeConnectionProperties.CONFIG_PREFIX)
public class DashScopeConnectionProperties extends DashScopeParentProperties {

	public static final String CONFIG_PREFIX = "spring.ai.dashscope";

	private Integer readTimeout;

	public DashScopeConnectionProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
		readTimeout = DEFAULT_READ_TIMEOUT;
	}

	public Integer getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

}
