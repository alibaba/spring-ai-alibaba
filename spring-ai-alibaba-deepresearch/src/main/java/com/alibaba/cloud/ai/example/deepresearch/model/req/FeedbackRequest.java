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

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author yingzi
 * @since 2025/6/10
 */

public record FeedbackRequest(
		/**
		 * 线程 ID，用于标识当前对话的唯一性。 默认值为 "__default__"，表示使用默认线程
		 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId,

		/**
		 * 是否接受Planner的计划，true为接受，false为重新生成
		 */
		@JsonProperty(value = "feed_back", defaultValue = "true") Boolean feedBack,

		/**
		 * 用户反馈内容，重新生成Planner计划是给予额外的上下文信息
		 */
		@JsonProperty(value = "feed_back_content", defaultValue = "") String feedBackContent) {
}
