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
package com.alibaba.cloud.ai.toolcalling.aliyunaisearch;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Docs:
 * https://help.aliyun.com/zh/open-search/search-platform/developer-reference/web-search?spm=opensearchspma.rag-server-market-detail.0.0.268e6857xmmLNT
 * base-url and api-key is required
 *
 * @author vlsmb
 */
@ConfigurationProperties(prefix = AliyunAiSearchConstants.CONFIG_PREFIX)
public class AliyunAiSearchProperties extends CommonToolCallProperties {

	public AliyunAiSearchProperties() {
		super("");
		this.setPropertiesFromEnv(AliyunAiSearchConstants.API_KEY_ENV, null, null, null);
		if (!StringUtils.hasText(this.getBaseUrl())) {
			this.setBaseUrl(System.getenv(AliyunAiSearchConstants.BASE_URL_ENV));
		}
	}

}
