/*
 * Copyright 2024 - 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dashscope.metadata;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi.TokenUsage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.util.Assert;

/**
 * {@link Usage} implementation for {@literal DashScopeAI}.
 *
 * @author Ken
 */
public class DashScopeAiUsage implements Usage {

	public static DashScopeAiUsage from(TokenUsage usage) {
		return new DashScopeAiUsage(usage);
	}

	private final TokenUsage usage;

	protected DashScopeAiUsage(TokenUsage usage) {
		Assert.notNull(usage, "Dashscope Usage must not be null");
		this.usage = usage;
	}

	protected TokenUsage getUsage() {
		return this.usage;
	}

	@Override
	public Long getPromptTokens() {
		return getUsage().inputTokens().longValue();
	}

	@Override
	public Long getGenerationTokens() {
		return getUsage().outputTokens().longValue();
	}

	@Override
	public Long getTotalTokens() {
		return getUsage().totalTokens().longValue();
	}

	@Override
	public String toString() {
		return getUsage().toString();
	}

}
