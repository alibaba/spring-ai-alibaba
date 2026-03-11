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
package com.alibaba.cloud.ai.graph.agent.hook.hip;

public final class HitlMetadataKeys {

	private HitlMetadataKeys() {
	}

	public static final String HITL_APPROVAL_TOOL_NAMES_KEY = "HITL_APPROVAL_TOOL_NAMES";
	public static final String HITL_APPROVAL_TOOL_CALL_IDS_KEY = "HITL_APPROVAL_TOOL_CALL_IDS";
	public static final String HITL_PENDING_TOOL_CALL_IDS_KEY = "_HITL_PENDING_TOOL_CALL_IDS_";
	public static final String HITL_PENDING_TOOL_FEEDBACKS_KEY = "_HITL_PENDING_TOOL_FEEDBACKS_";
	public static final String TOOL_OUTPUT_ENVELOPES_METADATA_KEY = "_TOOL_OUTPUT_ENVELOPES_";
}
