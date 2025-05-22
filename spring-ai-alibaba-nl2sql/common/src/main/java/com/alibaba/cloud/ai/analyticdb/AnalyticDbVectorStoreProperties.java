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
package com.alibaba.cloud.ai.analyticdb;

import lombok.Getter;
import lombok.Setter;
import org.springframework.ai.vectorstore.properties.CommonVectorStoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HeYQ
 */
@Setter
@Getter
@ConfigurationProperties("spring.ai.vectorstore.analytic")
public class AnalyticDbVectorStoreProperties extends CommonVectorStoreProperties {

	private String collectName;

	private String accessKeyId;

	private String accessKeySecret;

	private String regionId;

	private String dbInstanceId;

	private String managerAccount;

	private String managerAccountPassword;

	private String namespace;

	private String namespacePassword;

	private String metrics = "cosine";

	private Integer readTimeout = 60000;

	private String userAgent = "index";

	private Integer defaultTopK = -1;

	private Double defaultSimilarityThreshold = -1.0;

	public Map<String, Object> toAnalyticDbClientParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("accessKeyId", this.accessKeyId);
		params.put("accessKeySecret", this.accessKeySecret);
		params.put("regionId", this.regionId);
		params.put("readTimeout", this.readTimeout);
		params.put("userAgent", this.userAgent);
		return params;
	}

}
