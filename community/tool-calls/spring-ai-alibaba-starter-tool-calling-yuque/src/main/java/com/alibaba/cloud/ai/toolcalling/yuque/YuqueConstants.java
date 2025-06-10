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
package com.alibaba.cloud.ai.toolcalling.yuque;

import static com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX;

public final class YuqueConstants {

	public static final String CONFIG_PREFIX = TOOL_CALLING_CONFIG_PREFIX + ".yuque";

	public static final String TOKEN_ENV = "YUQUE_TOKEN";

	public static final String CREATE_DOC_TOOL_NAME = "createYuqueDoc";

	public static final String CREATE_BOOK_TOOL_NAME = "createYuqueBook";

	public static final String UPDATE_DOC_TOOL_NAME = "updateDocService";

	public static final String DELETE_DOC_TOOL_NAME = "deleteDocService";

}
