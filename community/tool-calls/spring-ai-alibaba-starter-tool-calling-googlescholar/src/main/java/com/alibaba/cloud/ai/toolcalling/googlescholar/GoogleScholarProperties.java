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
package com.alibaba.cloud.ai.toolcalling.googlescholar;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Google Scholar search configuration properties.
 *
 * @author Makoto
 */
@ConfigurationProperties(prefix = GoogleScholarConstants.CONFIG_PREFIX)
public class GoogleScholarProperties extends CommonToolCallProperties {

	/**
	 * The number of results per page (default: 10, max: 20)
	 */
	private int numResults = 10;

	/**
	 * Language preference for search results (default: en)
	 */
	private String language = "en";

	/**
	 * Whether to include citations count (default: true)
	 */
	private boolean includeCitations = true;

	/**
	 * Request timeout in milliseconds (default: 30000)
	 */
	private int timeout = 30000;

	public GoogleScholarProperties() {
		super(GoogleScholarConstants.BASE_URL);
	}

	public int getNumResults() {
		return numResults;
	}

	public void setNumResults(int numResults) {
		this.numResults = Math.min(numResults, 20); // Maximum 20 results
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public boolean isIncludeCitations() {
		return includeCitations;
	}

	public void setIncludeCitations(boolean includeCitations) {
		this.includeCitations = includeCitations;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
