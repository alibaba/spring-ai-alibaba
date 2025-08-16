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
package com.alibaba.cloud.ai.toolcalling.openalex;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OpenAlex API configuration properties.
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = OpenAlexConstants.CONFIG_PREFIX)
public class OpenAlexProperties extends CommonToolCallProperties {

	/**
	 * The number of results per page (default: 25, max: 200)
	 */
	private int perPage = 25;

	/**
	 * Request timeout in milliseconds (default: 30000)
	 */
	private int timeout = 30000;

	/**
	 * Whether to include abstract in work results (default: false)
	 */
	private boolean includeAbstract = false;

	/**
	 * Whether to include full metadata (default: true)
	 */
	private boolean includeFullMetadata = true;

	/**
	 * Maximum number of pages to fetch (default: 5)
	 */
	private int maxPages = 5;

	public OpenAlexProperties() {
		super(OpenAlexConstants.BASE_URL);
	}

	public int getPerPage() {
		return perPage;
	}

	public void setPerPage(int perPage) {
		this.perPage = Math.min(perPage, 200); // Maximum 200 results per page
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public boolean isIncludeAbstract() {
		return includeAbstract;
	}

	public void setIncludeAbstract(boolean includeAbstract) {
		this.includeAbstract = includeAbstract;
	}

	public boolean isIncludeFullMetadata() {
		return includeFullMetadata;
	}

	public void setIncludeFullMetadata(boolean includeFullMetadata) {
		this.includeFullMetadata = includeFullMetadata;
	}

	public int getMaxPages() {
		return maxPages;
	}

	public void setMaxPages(int maxPages) {
		this.maxPages = maxPages;
	}

}
