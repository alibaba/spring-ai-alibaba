/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.model;

import java.util.Objects;

import com.alibaba.cloud.ai.graph.agent.context.ContextWindowPolicy;

import org.springframework.ai.chat.model.ChatModel;

/**
 * A model and its optional input budget. A target without a policy inherits the
 * agent's configured budget.
 */
public record ModelTarget(ChatModel model, ContextWindowPolicy contextWindowPolicy) {

	public ModelTarget {
		Objects.requireNonNull(model, "model must not be null");
	}

	public ModelTarget(ChatModel model) {
		this(model, null);
	}

}
