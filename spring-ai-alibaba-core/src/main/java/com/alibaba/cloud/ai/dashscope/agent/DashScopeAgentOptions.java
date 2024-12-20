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
package com.alibaba.cloud.ai.dashscope.agent;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import org.springframework.ai.chat.prompt.ChatOptions;

import java.util.*;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 1.0.0-M2
 */

public class DashScopeAgentOptions implements ChatOptions {

	private String appId;

	private String sessionId;

	private String memoryId;

	private Boolean incrementalOutput;

	private Boolean hasThoughts;

	private JsonNode bizParams;

	@Override
	public String getModel() {
		return null;
	}

	@Override
	public Double getFrequencyPenalty() {
		return null;
	}

	@Override
	public Integer getMaxTokens() {
		return null;
	}

	@Override
	public Double getPresencePenalty() {
		return null;
	}

	@Override
	public List<String> getStopSequences() {
		return null;
	}

	@Override
	public Double getTemperature() {
		return 0d;
	}

	@Override
	public Double getTopP() {
		return 0d;
	}

	@Override
	public Integer getTopK() {
		return 0;
	}

	public String getAppId() {
		return appId;
	}

	public void setAppId(String appId) {
		this.appId = appId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public String getMemoryId() {
		return memoryId;
	}

	public void setMemoryId(String memoryId) {
		this.memoryId = memoryId;
	}

	public Boolean getIncrementalOutput() {
		return incrementalOutput;
	}

	public void setIncrementalOutput(Boolean incrementalOutput) {
		this.incrementalOutput = incrementalOutput;
	}

	public Boolean getHasThoughts() {
		return hasThoughts;
	}

	public void setHasThoughts(Boolean hasThoughts) {
		this.hasThoughts = hasThoughts;
	}

	public JsonNode getBizParams() {
		return bizParams;
	}

	public void setBizParams(JsonNode bizParams) {
		this.bizParams = bizParams;
	}

	@Override
	public ChatOptions copy() {
		return DashScopeAgentOptions.fromOptions(this);
	}

	public static DashScopeAgentOptions fromOptions(DashScopeAgentOptions options) {
		return DashScopeAgentOptions.builder()
			.withAppId(options.getAppId())
			.withSessionId(options.getSessionId())
			.withIncrementalOutput(options.getIncrementalOutput())
			.withHasThoughts(options.getHasThoughts())
			.withBizParams(options.getBizParams())
			.build();
	}

	public static DashScopeAgentOptions.Builder builder() {

		return new DashScopeAgentOptions.Builder();
	}

	public static class Builder {

		protected DashScopeAgentOptions options;

		public Builder() {
			this.options = new DashScopeAgentOptions();
		}

		public Builder(DashScopeAgentOptions options) {
			this.options = options;
		}

		public Builder withAppId(String appId) {
			this.options.setAppId(appId);
			return this;
		}

		public Builder withSessionId(String sessionId) {
			this.options.sessionId = sessionId;
			return this;
		}

		public Builder withMemoryId(String memoryId) {
			this.options.memoryId = memoryId;
			return this;
		}

		public Builder withIncrementalOutput(Boolean incrementalOutput) {
			this.options.incrementalOutput = incrementalOutput;
			return this;
		}

		public Builder withHasThoughts(Boolean hasThoughts) {
			this.options.hasThoughts = hasThoughts;
			return this;
		}

		public Builder withBizParams(JsonNode bizParams) {
			this.options.bizParams = bizParams;
			return this;
		}

		public DashScopeAgentOptions build() {
			return this.options;
		}

	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("DashScopeAgentOptions{");
		sb.append("appId='").append(appId).append('\'');
		sb.append(", sessionId='").append(sessionId).append('\'');
		sb.append(", memoryId='").append(memoryId).append('\'');
		sb.append(", incrementalOutput=").append(incrementalOutput);
		sb.append(", hasThoughts=").append(hasThoughts);
		sb.append(", bizParams=").append(bizParams);
		sb.append('}');
		return sb.toString();
	}

}
