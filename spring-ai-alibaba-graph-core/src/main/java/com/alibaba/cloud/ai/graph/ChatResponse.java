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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.drew.lang.annotations.Nullable;

public class ChatResponse {

	@Nullable
	private final String data;

	private ChatResponse(ChatResponseBuilder builder) {
		this.data = builder.data;
	}

	public String getData() {
		return data;
	}

	public static ChatResponseBuilder builder() {
		return new ChatResponseBuilder();
	}

	public static class ChatResponseBuilder {

		@Nullable
		private String data;

		public ChatResponseBuilder data(@Nullable String data) {
			this.data = data;
			return this;
		}

		public ChatResponse build() throws GraphStateException {
			return new ChatResponse(this);
		}

	}

}
