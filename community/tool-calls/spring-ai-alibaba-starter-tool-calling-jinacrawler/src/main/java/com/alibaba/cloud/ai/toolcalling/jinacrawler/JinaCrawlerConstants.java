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
package com.alibaba.cloud.ai.toolcalling.jinacrawler;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

public final class JinaCrawlerConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".jinacrawler";

	public static final String TOOL_NAME = "jinaCrawler";

	public static final String BASE_URL = "https://r.jina.ai/";

	public static final String API_KEY_ENV = "JINA_API_KEY";

	/**
	 * Jina Reader Request headers.
	 */
	public static final String X_LOCALE = "X-Locale";

	public static final String X_NO_CACHE = "X-No-Cache";

	public static final String X_PROXY_URL = "X-Proxy-Url";

	public static final String X_REMOVE_SELECTOR = "X-Remove-Selector";

	public static final String X_RETAIN_IMAGES = "X-Retain-Images";

	public static final String X_SET_COOKIE = "X-Set-Cookie";

	public static final String X_TARGET_SELECTOR = "X-Target-Selector";

	public static final String X_WAIT_FOR_SELECTOR = "X-Wait-For-Selector";

	public static final String X_WITH_GENERATED_ALT = "X-With-Generated-Alt";

	public static final String X_WITH_IFRAME = "X-With-Iframe";

	public static final String X_WITH_IMAGES_SUMMARY = "X-With-Images-Summary";

	public static final String X_WITH_LINKS_SUMMARY = "X-With-Links-Summary";

	public static final String X_WITH_SHADOW_DOM = "X-With-Shadow-Dom";

}
