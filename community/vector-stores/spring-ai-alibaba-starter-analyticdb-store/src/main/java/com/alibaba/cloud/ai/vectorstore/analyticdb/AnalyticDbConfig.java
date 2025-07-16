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

/**
 * @author HeYQ
 * @since 2024-10-23 20:22
 */
public class AnalyticDbConfig {

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

	public AnalyticDbConfig() {
	}

	public AnalyticDbConfig(String accessKeyId, String accessKeySecret, String regionId, String dbInstanceId,
			String managerAccount, String managerAccountPassword, String namespace, String namespacePassword,
			String metrics, Integer readTimeout, String userAgent) {
		this.accessKeyId = accessKeyId;
		this.accessKeySecret = accessKeySecret;
		this.regionId = regionId;
		this.dbInstanceId = dbInstanceId;
		this.managerAccount = managerAccount;
		this.managerAccountPassword = managerAccountPassword;
		this.namespace = namespace;
		this.namespacePassword = namespacePassword;
		this.metrics = metrics;
		this.readTimeout = readTimeout;
		this.userAgent = userAgent;
	}

	public String getAccessKeyId() {
		return accessKeyId;
	}

	public AnalyticDbConfig setAccessKeyId(String accessKeyId) {
		this.accessKeyId = accessKeyId;
		return this;
	}

	public String getAccessKeySecret() {
		return accessKeySecret;
	}

	public AnalyticDbConfig setAccessKeySecret(String accessKeySecret) {
		this.accessKeySecret = accessKeySecret;
		return this;
	}

	public String getRegionId() {
		return regionId;
	}

	public AnalyticDbConfig setRegionId(String regionId) {
		this.regionId = regionId;
		return this;
	}

	public String getDbInstanceId() {
		return dbInstanceId;
	}

	public AnalyticDbConfig setDbInstanceId(String dbInstanceId) {
		this.dbInstanceId = dbInstanceId;
		return this;
	}

	public String getManagerAccount() {
		return managerAccount;
	}

	public AnalyticDbConfig setManagerAccount(String managerAccount) {
		this.managerAccount = managerAccount;
		return this;
	}

	public String getManagerAccountPassword() {
		return managerAccountPassword;
	}

	public AnalyticDbConfig setManagerAccountPassword(String managerAccountPassword) {
		this.managerAccountPassword = managerAccountPassword;
		return this;
	}

	public String getNamespace() {
		return namespace;
	}

	public AnalyticDbConfig setNamespace(String namespace) {
		this.namespace = namespace;
		return this;
	}

	public String getNamespacePassword() {
		return namespacePassword;
	}

	public AnalyticDbConfig setNamespacePassword(String namespacePassword) {
		this.namespacePassword = namespacePassword;
		return this;
	}

	public String getMetrics() {
		return metrics;
	}

	public AnalyticDbConfig setMetrics(String metrics) {
		this.metrics = metrics;
		return this;
	}

	public Integer getReadTimeout() {
		return readTimeout;
	}

	public AnalyticDbConfig setReadTimeout(Integer readTimeout) {
		this.readTimeout = readTimeout;
		return this;
	}

	public String getUserAgent() {
		return userAgent;
	}

	public AnalyticDbConfig setUserAgent(String userAgent) {
		this.userAgent = userAgent;
		return this;
	}

}
