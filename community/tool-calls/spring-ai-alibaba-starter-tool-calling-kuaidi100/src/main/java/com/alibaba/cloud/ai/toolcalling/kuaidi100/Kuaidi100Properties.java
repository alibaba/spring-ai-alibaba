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
package com.alibaba.cloud.ai.toolcalling.kuaidi100;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author XiaoYunTao
 * @since 2024/12/25
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.toolcalling.kuaidi100")
public class Kuaidi100Properties {

	/**
	 * 授权key <a href="https://api.kuaidi100.com/manager/v2/myinfo/enterprise">获取授权key</a>
	 */
	private String key;

	/**
	 * customer
	 * <a href="https://api.kuaidi100.com/manager/v2/myinfo/enterprise">获取customer</a>
	 */
	private String customer;

	public Kuaidi100Properties(String key, String customer) {
		this.key = key;
		this.customer = customer;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setCustomer(String customer) {
		this.customer = customer;
	}

	public String getKey() {
		return key;
	}

	public String getCustomer() {
		return customer;
	}

}