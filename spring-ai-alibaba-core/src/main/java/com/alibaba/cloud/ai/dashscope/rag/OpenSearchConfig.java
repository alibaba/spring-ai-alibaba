/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dashscope.rag;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class for the Alibaba OpenSearch. This class holds the necessary
 * configuration parameters such as instance ID, endpoint, access user name, and access
 * password required to interact with the HA3 engine.
 *
 * @author ming（fuyou.lxm）
 * @since 1.0.0-M3
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OpenSearchConfig {

	private String instanceId;

	private String endpoint;

	private String accessUserName;

	private String accessPassWord;

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

	public Map<String, ?> toClientParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("instanceId", this.getInstanceId());
		params.put("endpoint", this.getEndpoint());
		params.put("accessUserName", this.getAccessUserName());
		params.put("accessPassWord", this.getAccessPassWord());
		return params;
	}

}