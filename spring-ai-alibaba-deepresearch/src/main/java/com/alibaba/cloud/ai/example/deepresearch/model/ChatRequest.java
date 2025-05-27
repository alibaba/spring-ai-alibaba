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

package com.alibaba.cloud.ai.example.deepresearch.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * @author yingzi
 * @date 2025/5/27 15:52
 */

public record ChatRequest(

		/**
		 * 历史消息聊天列表，包含用户和系统
		 */
		@JsonProperty(value = "messages") List<ChatMessage> messages,

		/**
		 * 线程 ID，用于标识当前对话的唯一性。 默认值为 "__default__"，表示使用默认线程。
		 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,
		/**
		 * 最大计划迭代次数，用于控制处理请求的最大步骤数。 默认值为 1，表示至少执行一次完整流程。
		 */
		@JsonProperty(value = "max_plan_iterations", defaultValue = "1") int maxPlanIterations,
		/**
		 * 最大步骤数，用于控制单次请求的步骤数。 默认值为 3，表示最多执行 3 步。
		 */
		@JsonProperty(value = "max_step_num", defaultValue = "3") int maxStepNum,
		/**
		 * 是否自动接受计划，用于控制是否自动接受生成的计划。 默认值为 true，表示自动接受计划。
		 */
		@JsonProperty(value = "auto_accept_plan", defaultValue = "false") boolean autoAcceptPlan,
		/**
		 * 中断反馈，用于控制中断后的反馈信息。
		 */
		@JsonProperty(value = "interrupt_feedback") String interruptFeedback,
		/**
		 * 是否启用背景调查，用于控制是否启用背景调查 默认值为 true，表示启用背景调查
		 */
		@JsonProperty(value = "enable_background_investigation",
				defaultValue = "true") boolean enableBackgroundInvestigation,
		/**
		 * 是否调试模式，用于控制是否开启调试模式。 默认值为 false，表示关闭调试模式。
		 */
		@JsonProperty(value = "debug", defaultValue = "false") boolean debug,
		/**
		 * MCP 设置
		 */
		@JsonProperty(value = "mcp_settings") Map<String, Object> mcpSettings) {
}
