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
package com.alibaba.cloud.ai.vectorstore.analyticdb;

import org.springframework.ai.autoconfigure.vectorstore.CommonVectorStoreProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HeYQ
 */
@ConfigurationProperties(AnalyticDbVectorStoreProperties.CONFIG_PREFIX)
public class AnalyticDbVectorStoreProperties extends CommonVectorStoreProperties {

	public static final String CONFIG_PREFIX = "spring.ai.vectorstore.analytic";

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

	public String getCollectName() {
		return collectName;
	}

	public void setCollectName(String collectName) {
		this.collectName = collectName;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public void setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
	}

	public String getAccessKeySecret() {
		return accessKeySecret;
	}

	public void setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
	}

	public String getRegionId() {
		return regionId;
	}

	public void setRegionId(String regionId) {
		this.regionId = regionId;
	}

	public String getDbInstanceId() {
		return dbInstanceId;
	}

	public void setDbInstanceId(String dbInstanceId) {
		this.dbInstanceId = dbInstanceId;
	}

	public String getManagerAccount() {
		return managerAccount;
	}

	public void setManagerAccount(String managerAccount) {
		this.managerAccount = managerAccount;
	}

	public String getManagerAccountPassword() {
		return managerAccountPassword;
	}

	public void setManagerAccountPassword(String managerAccountPassword) {
		this.managerAccountPassword = managerAccountPassword;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getNamespacePassword() {
		return namespacePassword;
	}

	public void setNamespacePassword(String namespacePassword) {
		this.namespacePassword = namespacePassword;
	}

	public String getMetrics() {
		return metrics;
	}

	public void setMetrics(String metrics) {
		this.metrics = metrics;
	}

	public Integer getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}

	public Integer getDefaultTopK() {
		return defaultTopK;
	}

	public void setDefaultTopK(Integer defaultTopK) {
		this.defaultTopK = defaultTopK;
	}

	public Double getDefaultSimilarityThreshold() {
		return defaultSimilarityThreshold;
	}

	public void setDefaultSimilarityThreshold(Double defaultSimilarityThreshold) {
		this.defaultSimilarityThreshold = defaultSimilarityThreshold;
	}

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
