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
package com.alibaba.cloud.ai.dashscope.rag;

/**
 * @author HeYQ
 * @since 2024-10-23 20:22
 */
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AnalyticdbConfig {

	private String accessKeyId;

	private String accessKeySecret;

	private String regionId;

	private String DBInstanceId;

	private String managerAccount;

	private String managerAccountPassword;

	private String namespace;

	private String namespacePassword;

	private String metrics = "cosine";

	private Integer readTimeout = 60000;

	private Long embeddingDimension = 1536L;

	private String userAgent = "index";

	public AnalyticdbConfig() {

	}

	public AnalyticdbConfig(String accessKeyId, String accessKeySecret, String regionId, String DBInstanceId,
			String managerAccount, String managerAccountPassword, String namespace, String namespacePassword,
			String metrics, Integer readTimeout, Long embeddingDimension, String userAgent) {
		this.accessKeyId = accessKeyId;
		this.accessKeySecret = accessKeySecret;
		this.regionId = regionId;
		this.DBInstanceId = DBInstanceId;
		this.managerAccount = managerAccount;
		this.managerAccountPassword = managerAccountPassword;
		this.namespace = namespace;
		this.namespacePassword = namespacePassword;
		this.metrics = metrics;
		this.readTimeout = readTimeout;
		this.embeddingDimension = embeddingDimension;
		this.userAgent = userAgent;
	}

	public Map<String, Object> toAnalyticdbClientParams() {
		Map<String, Object> params = new HashMap<>();
		params.put("accessKeyId", this.accessKeyId);
		params.put("accessKeySecret", this.accessKeySecret);
		params.put("regionId", this.regionId);
		params.put("readTimeout", this.readTimeout);
		params.put("userAgent", this.userAgent);
		return params;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public AnalyticdbConfig setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
		return this;
	}

	public String getAccessKeySecret() {
		return accessKeySecret;
	}

	public AnalyticdbConfig setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
		return this;
	}

	public String getRegionId() {
		return regionId;
	}

	public AnalyticdbConfig setRegionId(String regionId) {
		this.regionId = regionId;
		return this;
	}

	public String getDBInstanceId() {
		return DBInstanceId;
	}

	public AnalyticdbConfig setDBInstanceId(String DBInstanceId) {
		this.DBInstanceId = DBInstanceId;
		return this;
	}

	public String getManagerAccount() {
		return managerAccount;
	}

	public AnalyticdbConfig setManagerAccount(String managerAccount) {
		this.managerAccount = managerAccount;
		return this;
	}

	public String getManagerAccountPassword() {
		return managerAccountPassword;
	}

	public AnalyticdbConfig setManagerAccountPassword(String managerAccountPassword) {
		this.managerAccountPassword = managerAccountPassword;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public AnalyticdbConfig setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getNamespacePassword() {
		return namespacePassword;
	}

	public AnalyticdbConfig setNamespacePassword(String namespacePassword) {
		this.namespacePassword = namespacePassword;
		return this;
	}

	public String getMetrics() {
		return metrics;
	}

	public AnalyticdbConfig setMetrics(String metrics) {
		this.metrics = metrics;
		return this;
	}

	public Integer getReadTimeout() {
		return readTimeout;
	}

	public AnalyticdbConfig setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}

	public Long getEmbeddingDimension() {
		return embeddingDimension;
	}

	public AnalyticdbConfig setEmbeddingDimension(Long embeddingDimension) {
		this.embeddingDimension = embeddingDimension;
		return this;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public AnalyticdbConfig setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}

}
