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
package com.alibaba.cloud.ai.vectorstore.oceanbase;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.ConstructorBinding;

/**
 * Configuration properties for OceanBase Vector Store. 自然语言相关的属性，用于配置
 * OceanBaseVecotrStore 以确保正确的初始化和运行。
 *
 * @author xxsc0529
 */
@ConfigurationProperties(prefix = OceanBaseVectorStoreProperties.CONFIG_PREFIX)
public class OceanBaseVectorStoreProperties {

	public static final String CONFIG_PREFIX = "spring.ai.vectorstore.oceanbase";

	private final String url;

	private final String username;

	private final String password;

	private final String tableName; // 数据表名

	private Integer defaultTopK = -1;

	private Double defaultSimilarityThreshold = -1.0;

	@ConstructorBinding
	public OceanBaseVectorStoreProperties(String url, String username, String password, String tableName,
			Integer vectordimnum, boolean enabled) {
		this.url = url;
		this.username = username;
		this.password = password;
		this.tableName = tableName;
	}

	// Getters
	public String getUrl() {
		return url;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getTableName() {
		return tableName;
	}

	public Integer getDefaultTopK() {
		return defaultTopK;
	}

	public Double getDefaultSimilarityThreshold() {
		return defaultSimilarityThreshold;
	}

}
