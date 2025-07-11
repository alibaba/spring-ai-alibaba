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
package com.alibaba.cloud.ai.toolcalling.bravesearch;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

public class BraveSearchConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".bravesearch";

	public static final String TOOL_NAME = "braveSearch";

	public static final String API_KEY_ENV = "BRAVE_SEARCH_API_KEY";

	public static final String BASE_URL = "https://api.search.brave.com/res/v1/web/search";

}
