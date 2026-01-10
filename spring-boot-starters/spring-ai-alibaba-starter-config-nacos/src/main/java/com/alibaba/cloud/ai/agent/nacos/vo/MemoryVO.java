/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.agent.nacos.vo;

public class MemoryVO {

	String storageType;

	String address;

	String credential;

	String compressionStrategy;

	String searchStrategy;

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public String getCredential() {
		return credential;
	}

	public void setCredential(String credential) {
		this.credential = credential;
	}

	public String getCompressionStrategy() {
		return compressionStrategy;
	}

	public void setCompressionStrategy(String compressionStrategy) {
		this.compressionStrategy = compressionStrategy;
	}

	public String getSearchStrategy() {
		return searchStrategy;
	}

	public void setSearchStrategy(String searchStrategy) {
		this.searchStrategy = searchStrategy;
	}
}
