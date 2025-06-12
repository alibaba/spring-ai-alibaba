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
package com.alibaba.cloud.ai.toolcalling.firecrawl;

/**
 * Firecrawl Request body keys. Reference:
 * <a href="https://docs.firecrawl.dev/api-reference/endpoint/scrape">...</a> LangChain:
 * <a href=
 * "https://python.langchain.com/docs/integrations/document_loaders/firecrawl/#modes">...</a>
 */
public enum FireCrawlModeEnum {

	SCRAPE("scrape"), // Scrape single url and return the markdown.
	MAP("map"), // Maps the URL and returns a list of semantically related pages.
	CRAWL("crawl"); // Crawl the url and all accessible sub pages and return the markdown
					// for each one.

	private final String value;

	FireCrawlModeEnum(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	public String toString() {
		return value;
	}

}
