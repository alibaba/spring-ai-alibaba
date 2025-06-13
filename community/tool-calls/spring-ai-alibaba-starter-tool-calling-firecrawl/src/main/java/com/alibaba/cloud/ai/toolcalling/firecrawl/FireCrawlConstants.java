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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

public final class FireCrawlConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".firecrawl";

	public static final String TOOL_NAME = "firecrawl";

	public static final String BASE_URL = "https://api.firecrawl.dev/v1/";

	public static final String API_KEY_ENV = "FIRECRAWL_API_KEY";

	/**
	 * Headers
	 */
	static final String REMOVE_BASE64_IMAGES = "removeBase64Images";

	static final String SKIP_TLS_VERIFICATION = "skipTlsVerification";

	static final String MOBILE = "mobile";

	static final String WAIT_FOR = "waitFor";

	static final String URL = "url";

	static final String FORMATS = "formats";

	static final String ONLY_MAIN_CONTENT = "onlyMainContent";

	static final String INCLUDE_TAGS = "includeTags";

	static final String EXCLUDE_TAGS = "excludeTags";

}
