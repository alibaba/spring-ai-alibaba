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
package com.alibaba.cloud.ai.toolcalling.wikipedia;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Makoto
 */
@ConfigurationProperties(prefix = WikipediaConstants.CONFIG_PREFIX)
public class WikipediaProperties extends CommonToolCallProperties {

	/**
	 * Wikipedia API endpoint
	 */
	private String language = "zh";

	/**
	 * Maximum number of search results to return
	 */
	private int limit = 5;

	/**
	 * Maximum length of excerpt to return
	 */
	private int excerptLength = 500;

	public WikipediaProperties() {
		super("https://zh.wikipedia.org/");
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public int getExcerptLength() {
		return excerptLength;
	}

	public void setExcerptLength(int excerptLength) {
		this.excerptLength = excerptLength;
	}

}
