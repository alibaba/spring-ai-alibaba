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
package com.alibaba.cloud.ai.toolcalling.worldbankdata;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * World Bank Data API configuration properties. The World Bank API provides free access
 * to development data without authentication. Base URL: https://api.worldbank.org/v2
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = WorldBankDataConstants.CONFIG_PREFIX)
public class WorldBankDataProperties extends CommonToolCallProperties {

	/**
	 * Request timeout in milliseconds (default: 30000)
	 */
	private int timeout = 30000;

	/**
	 * Default number of results per page (default: 10, max: 100)
	 */
	private int perPage = 10;

	/**
	 * Default output format (json or xml, default: json)
	 */
	private String format = "json";

	/**
	 * Default language for responses (en, es, fr, ar, zh, default: en)
	 */
	private String language = "en";

	public WorldBankDataProperties() {
		super(WorldBankDataConstants.BASE_URL);
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public int getPerPage() {
		return Math.min(perPage, 100); // Maximum 100 results per page
	}

	public void setPerPage(int perPage) {
		this.perPage = Math.min(perPage, 100);
	}

	public String getFormat() {
		return format;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
