/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.hook.unknowntool;

/**
 * Metadata keys used to mark unknown tool responses and drive guard behavior.
 */
public final class UnknownToolGuardConstants {

	public static final String UNKNOWN_TOOL_ERROR_TYPE = "unknown_tool";

	public static final String ERROR_TYPE_METADATA_KEY = "errorType";

	public static final String UNKNOWN_TOOL_RESPONSE_METADATA_KEY = "unknownToolResponse";

	public static final String REQUESTED_TOOL_NAMES_METADATA_KEY = "requestedToolNames";

	public static final String AVAILABLE_TOOL_NAMES_METADATA_KEY = "availableToolNames";

	public static final String UNKNOWN_TOOL_COUNT_METADATA_KEY = "unknownToolCount";

	public static final String ALL_TOOL_CALLS_UNKNOWN_METADATA_KEY = "allToolCallsUnknown";

	private UnknownToolGuardConstants() {
	}

}

