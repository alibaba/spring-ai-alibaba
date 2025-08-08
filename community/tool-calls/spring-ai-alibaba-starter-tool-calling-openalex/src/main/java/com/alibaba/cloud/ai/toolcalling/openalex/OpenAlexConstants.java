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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

public class OpenAlexConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".openalex";

	public static final String BASE_URL = "https://api.openalex.org";

	public static final String TOOL_NAME = "openAlex";

	public static final String USER_AGENT = "Spring AI Alibaba Tool Calling / OpenAlex (mailto:your-email@example.com)";

	// API endpoints
	public static final String WORKS_ENDPOINT = "/works";

	public static final String AUTHORS_ENDPOINT = "/authors";

	public static final String SOURCES_ENDPOINT = "/sources";

	public static final String INSTITUTIONS_ENDPOINT = "/institutions";

	public static final String TOPICS_ENDPOINT = "/topics";

	public static final String PUBLISHERS_ENDPOINT = "/publishers";

	public static final String FUNDERS_ENDPOINT = "/funders";

}
