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
package com.alibaba.cloud.ai.toolcalling.crawler;

/**
 * Crawler constants define. In Jina Reader, parameters are passed through the request
 * header. In Firecrawl, parameters are passed through the request body.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public interface CrawlerConstants {

	String JINA_BASE_URL = "https://r.jina.ai/";

	String FIRECRAWL_BASE_URL = "https://api.firecrawl.dev/v1/";

	String CONFIG_PREFIX = "spring.ai.alibaba.toolcalling.crawler.";

	/**
	 * Jina Reader Request headers.
	 */
	interface JinaHeaders {

		String X_LOCALE = "X-Locale";

		String X_NO_CACHE = "X-No-Cache";

		String X_PROXY_URL = "X-Proxy-Url";

		String X_REMOVE_SELECTOR = "X-Remove-Selector";

		String X_RETAIN_IMAGES = "X-Retain-Images";

		String X_SET_COOKIE = "X-Set-Cookie";

		String X_TARGET_SELECTOR = "X-Target-Selector";

		String X_WAIT_FOR_SELECTOR = "X-Wait-For-Selector";

		String X_WITH_GENERATED_ALT = "X-With-Generated-Alt";

		String X_WITH_IFRAME = "X-With-Iframe";

		String X_WITH_IMAGES_SUMMARY = "X-With-Images-Summary";

		String X_WITH_LINKS_SUMMARY = "X-With-Links-Summary";

		String X_WITH_SHADOW_DOM = "X-With-Shadow-Dom";

	}

	/**
	 * Firecrawl Request body keys. Reference:
	 * <a href="https://docs.firecrawl.dev/api-reference/endpoint/scrape">...</a>
	 * LangChain: <a href=
	 * "https://python.langchain.com/docs/integrations/document_loaders/firecrawl/#modes">...</a>
	 */
	interface FirecrawlMode {

		/**
		 * Scrape single url and return the markdown.
		 */
		String SCRAPE = "scrape";

		/**
		 * Maps the URL and returns a list of semantically related pages.
		 */
		String MAP = "map";

		/**
		 * Crawl the url and all accessible sub pages and return the markdown for each
		 * one.
		 */
		String CRAWL = "crawl";

	}

	interface FirecrawlFormats {

		String MARKDOWN = "markdown";

		String RAW_HTML = "rawHtml";

		String HTML = "html";

		String LINKS = "links";

	}

	interface FirecrawlRequestBodyKey {

		String REMOVE_BASE64_IMAGES = "removeBase64Images";

		String SKIP_TLS_VERIFICATION = "skipTlsVerification";

		String MOBILE = "mobile";

		String WAIT_FOR = "waitFor";

		String URL = "url";

		String FORMATS = "formats";

		String ONLY_MAIN_CONTENT = "onlyMainContent";

		String INCLUDE_TAGS = "includeTags";

		String EXCLUDE_TAGS = "excludeTags";

	}

}
