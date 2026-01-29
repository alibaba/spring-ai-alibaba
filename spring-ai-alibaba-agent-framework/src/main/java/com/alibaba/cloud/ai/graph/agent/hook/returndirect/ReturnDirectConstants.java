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
package com.alibaba.cloud.ai.graph.agent.hook.returndirect;

public final class ReturnDirectConstants {

	/**
	 * Metadata key used in ToolResponseMessage to indicate that returnDirect is enabled.
	 * When all tools have returnDirect=true, this key is set in ToolResponseMessage metadata
	 * with value {@link org.springframework.ai.model.tool.ToolExecutionResult#FINISH_REASON}.
	 */
	public static final String FINISH_REASON_METADATA_KEY = "finishReason";

	private ReturnDirectConstants() {}
}
