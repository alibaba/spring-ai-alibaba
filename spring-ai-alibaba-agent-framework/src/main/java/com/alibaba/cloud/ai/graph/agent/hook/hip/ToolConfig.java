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

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class ToolConfig {
	private final String description;

	private final List<FeedbackResult> allowedDecisions;

	private final Predicate<ToolCallRequest> when;

	public ToolConfig() {
		this(new Builder());
	}

	private ToolConfig(Builder builder) {
		this.description = builder.description;
		this.allowedDecisions = builder.allowedDecisions;
		this.when = builder.when;
	}

	public static Builder builder() {
		return new Builder();
	}

	String getDescription() {
		return description;
	}

	List<FeedbackResult> getAllowedDecisions() {
		return allowedDecisions;
	}

	boolean requiresReview(ToolCallRequest request) {
		return when.test(request);
	}

	public static class Builder {
		private String description;

		private List<FeedbackResult> allowedDecisions = ToolFeedback.DEFAULT_ALLOWED_DECISIONS;

		private Predicate<ToolCallRequest> when = request -> true;

		public Builder description(String description) {
			this.description = description;
			return this;
		}

		/** Sets the decisions a reviewer can submit for this tool. */
		public Builder allowedDecisions(FeedbackResult... decisions) {
			var allowed = List.of(decisions);
			if (allowed.isEmpty() || allowed.stream().distinct().count() != allowed.size()) {
				throw new IllegalArgumentException("Allowed decisions must be non-empty and unique");
			}
			this.allowedDecisions = allowed;
			return this;
		}

		/** Limits review to calls that satisfy the predicate. Tool metadata can independently require confirmation. */
		public Builder when(Predicate<ToolCallRequest> when) {
			this.when = Objects.requireNonNull(when);
			return this;
		}

		public ToolConfig build() {
			return new ToolConfig(this);
		}
	}

}
