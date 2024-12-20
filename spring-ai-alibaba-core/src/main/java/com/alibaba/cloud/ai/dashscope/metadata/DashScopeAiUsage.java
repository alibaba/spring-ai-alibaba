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
		Integer totalTokens = getUsage().totalTokens();
		if (totalTokens != null) {
			return totalTokens.longValue();
		}
		else {
			return getPromptTokens() + getGenerationTokens();
		}
	}

	@Override
	public String toString() {
		return getUsage().toString();
	}

}
