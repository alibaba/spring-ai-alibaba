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
 * Report Export Request
 *
 * @author sixiyida
 * @since 2025/6/20
 */
public record ExportRequest(
		/**
		 * The export format, accepts "markdown", "md", or "pdf".
		 */
		@JsonProperty(value = "format", defaultValue = "markdown") String format,

		/**
		 * Thread_id, Used to uniquely identify the current conversation.
		 * Defaults to "__default__", indicating the default thread is used.
		 */
		@JsonProperty(value = "thread_id", defaultValue = "__default__") String threadId) {
}
