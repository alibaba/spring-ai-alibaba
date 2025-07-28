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
package com.alibaba.cloud.ai.toolcalling.nationalstatistics;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 国家统计局工具属性配置
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = NationalStatisticsConstants.CONFIG_PREFIX)
public class NationalStatisticsProperties extends CommonToolCallProperties {

	/**
	 * 最大返回结果数
	 */
	private Integer maxResults = 10;

	public NationalStatisticsProperties() {
		super(NationalStatisticsConstants.BASE_URL);
	}

	public Integer getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(Integer maxResults) {
		this.maxResults = maxResults;
	}

}
