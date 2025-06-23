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
		 * 线程 ID，用于标识当前对话的唯一性。 默认值为 "__default__"，表示使用默认线程。
		 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,
		/**
		 * 最大计划迭代次数，用于控制处理请求的最大步骤数。 默认值为 1，表示至少执行一次完整流程。
		 */
		@JsonProperty(value = "max_plan_iterations", defaultValue = "1") Integer maxPlanIterations,
		/**
		 * 最大步骤数，用于控制单次请求的步骤数。 默认值为 3，表示最多执行 3 步。
		 */
		@JsonProperty(value = "max_step_num", defaultValue = "3") Integer maxStepNum,
		/**
		 * 是否自动接受计划，用于控制是否自动接受生成的计划。 默认值为 true，表示自动接受计划。
		 */
		@JsonProperty(value = "auto_accepted_plan", defaultValue = "true") Boolean autoAcceptPlan,
		/**
		 * 中断反馈，用于控制中断后的反馈信息。
		 */
		@JsonProperty(value = "interrupt_feedback") String interruptFeedback,
		/**
		 * 是否启用背景调查，用于控制是否启用背景调查 默认值为 true，表示启用背景调查
		 */
		@JsonProperty(value = "enable_background_investigation",
				defaultValue = "true") Boolean enableBackgroundInvestigation,
		/**
		 * MCP 设置
		 */
		@JsonProperty(value = "mcp_settings") Map<String, Object> mcpSettings,

		@JsonProperty(value = "query", defaultValue = "草莓蛋糕怎么做呀") String query,

		/**
		 * 搜索引擎，默认为Tavily
		 */
		@JsonProperty(value = "search_engine", defaultValue = "tavily") SearchEnum searchEngine) {
}
