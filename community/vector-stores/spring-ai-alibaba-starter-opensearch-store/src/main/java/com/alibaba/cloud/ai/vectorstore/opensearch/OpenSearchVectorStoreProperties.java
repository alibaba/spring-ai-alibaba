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
package com.alibaba.cloud.ai.vectorstore.opensearch;

import org.springframework.ai.vectorstore.properties.CommonVectorStoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

import static com.alibaba.cloud.ai.vectorstore.opensearch.OpenSearchVectorStoreProperties.DEFAULT_ALIBABA_OPENSEARCH_CONFIG_PREFIX;

/**
 * @author 北极星
 */

@ConfigurationProperties(prefix = DEFAULT_ALIBABA_OPENSEARCH_CONFIG_PREFIX)
public class OpenSearchVectorStoreProperties extends CommonVectorStoreProperties {

	protected static final String DEFAULT_ALIBABA_OPENSEARCH_CONFIG_PREFIX = "spring.ai.alibaba.vectorstore.opensearch";

	private Boolean enabled;

	private String instanceId;

	private String endpoint;

	private String accessUserName;

	private String accessPassWord;

	public Boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public void setInstanceId(String instanceId) {
		this.instanceId = instanceId;
	}

	public String getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(String endpoint) {
		this.endpoint = endpoint;
	}

	public String getAccessUserName() {
		return accessUserName;
	}

	public void setAccessUserName(String accessUserName) {
		this.accessUserName = accessUserName;
	}

	public String getAccessPassWord() {
		return accessPassWord;
	}

	public void setAccessPassWord(String accessPassWord) {
		this.accessPassWord = accessPassWord;
	}

	public Map<String, Object> toClientParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("instanceId", this.getInstanceId());
		params.put("endpoint", this.getEndpoint());
		params.put("accessUserName", this.getAccessUserName());
		params.put("accessPassWord", this.getAccessPassWord());
		return params;
	}

}
