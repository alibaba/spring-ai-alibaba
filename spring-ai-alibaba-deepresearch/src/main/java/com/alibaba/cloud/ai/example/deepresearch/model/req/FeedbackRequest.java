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
		 * session_id. Defaults to "__default__", indicating the default session is used.
		 */
		@JsonProperty(value = "session_id", defaultValue = "__default__") String sessionId,

		/**
		 * Unique identifier for the current conversation thread
		 */
		@JsonProperty(value = "thread_id", defaultValue = "") String threadId,

		/**
		 *  Determines whether to accept the Planner's proposal. true: accept, false: regenerate. Default: true.
		 */
		@JsonProperty(value = "feed_back", defaultValue = "true") Boolean feedBack,

		/**
		 * User feedback content provides additional contextual information for regenerating the Planner's plan.
		 */
		@JsonProperty(value = "feed_back_content", defaultValue = "") String feedBackContent) {
}
