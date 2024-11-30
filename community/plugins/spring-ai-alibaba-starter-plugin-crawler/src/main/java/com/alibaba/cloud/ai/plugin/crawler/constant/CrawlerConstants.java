/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.crawler.constant;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public interface CrawlerConstants {

	String JINA_BASE_URL = "https://r.jina.ai/";

	String FIRECRAWL_BASE_URL = "https://api.firecrawl.dev";

	String CONFIG_PREFIX = "spring.ai.alibaba.plugin.crawler";

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

}
