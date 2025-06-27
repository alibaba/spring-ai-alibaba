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
package com.alibaba.cloud.ai.toolcalling.jsonprocessor;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;

public final class JsonProcessorConstants {

	public static final String CONFIG_PREFIX = CommonToolCallConstants.TOOL_CALLING_CONFIG_PREFIX + ".jsonprocessor";

	public static final String INSERT_TOOL_NAME = "jsonInsertPropertyField";

	public static final String PARSE_TOOL_NAME = "jsonParseProperty";

	public static final String REPLACE_TOOL_NAME = "jsonReplacePropertyFiledValue";

	public static final String REMOVE_TOOL_NAME = "jsonRemovePropertyField";

}
