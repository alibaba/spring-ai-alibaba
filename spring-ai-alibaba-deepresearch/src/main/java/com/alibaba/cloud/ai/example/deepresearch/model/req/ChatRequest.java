/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.model.req;

import com.alibaba.cloud.ai.toolcalling.searches.SearchEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * @author yingzi
 * @since 2025/5/27 15:52
 */

public record ChatRequest(

		/**
		 * Session ID. Default: "__default__", indicating the use of the default session
		 */
		@JsonProperty(value = "session_id", defaultValue = "__default__") String sessionId,

		/**
		 * Thread ID, used to identify the uniqueness of the current conversation
		 */
		@JsonProperty(value = "thread_id", defaultValue = "") String threadId,
		/**
		 * Maximum planning iteration count, used to control the maximum number of steps for processing requests
		 * Default value is 1, indicating at least one complete execution of the process
		 */
		@JsonProperty(value = "max_plan_iterations", defaultValue = "1") Integer maxPlanIterations,
		/**
		 * Maximum step count, used to control the number of steps per request
		 * Defaults to 3, indicating a maximum of 3 steps will be executed
		 */
		@JsonProperty(value = "max_step_num", defaultValue = "3") Integer maxStepNum,
		/**
		 * Whether to automatically accept the plan, used to control whether to automatically accept the generated plan
		 * Default value is true, indicating that the plan is automatically accepted
		 */
		@JsonProperty(value = "auto_accepted_plan", defaultValue = "true") Boolean autoAcceptPlan,
		/**
		 * Interrupt feedback, used to control the feedback message after an interruption
		 */
		@JsonProperty(value = "interrupt_feedback") String interruptFeedback,
		/**
		 * Whether to enable background check, used to control if the background check is enabled
		 * The default value is true, meaning the background check is enabled
		 */
		@JsonProperty(value = "enable_deepresearch", defaultValue = "true") Boolean enableDeepResearch,
		/**
		 * MCP setting
		 */
		@JsonProperty(value = "mcp_settings") Map<String, Object> mcpSettings,

		@JsonProperty(value = "query", defaultValue = "草莓蛋糕怎么做呀") String query,

		/**
		 * Search engine. Default: Tavily
		 */
		@JsonProperty(value = "search_engine", defaultValue = "tavily") SearchEnum searchEngine,

		/**
		 * Whether to enable website filtering. Default: true
		 */
		@JsonProperty(value = "enable_search_filter", defaultValue = "true") Boolean enableSearchFilter,

		/**
		 * Optimize Query Count. Default: 3
		 */
		@JsonProperty(value = "optimize_query_num", defaultValue = "3") Integer optimizeQueryNum,

		/**
		 * Whether the user is allowed to upload files. Default: false
		 */
		@JsonProperty(value = "user_upload_file", defaultValue = "false") Boolean isUploadFile) {
}
