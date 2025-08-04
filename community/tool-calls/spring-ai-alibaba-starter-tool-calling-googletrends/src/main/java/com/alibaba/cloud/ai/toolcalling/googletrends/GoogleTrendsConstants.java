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

package com.alibaba.cloud.ai.toolcalling.googletrends;

import java.util.concurrent.ThreadLocalRandom;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.DEFAULT_USER_AGENTS;
import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

public final class GoogleTrendsConstants {

	public static final String CONFIG_PREFIX = TOOL_CALLING_CONFIG_PREFIX + ".googletrends";

	public static final String TOOL_NAME = "googleTrendsSearch";

	public static final String USER_AGENT_VALUE = DEFAULT_USER_AGENTS[ThreadLocalRandom.current()
		.nextInt(DEFAULT_USER_AGENTS.length)];

	public static final String BASE_URL = "https://serpapi.com/search.json";

	public static final String API_KEY_ENV = "SERPAPI_KEY";

}
