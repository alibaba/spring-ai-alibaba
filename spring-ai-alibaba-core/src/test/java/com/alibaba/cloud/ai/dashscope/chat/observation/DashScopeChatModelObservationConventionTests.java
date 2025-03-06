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
package com.alibaba.cloud.ai.dashscope.chat.observation;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.observation.ChatModelObservationContext;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeChatModelObservationConvention. Tests cover observation name,
 * stop sequences handling, and key value generation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeChatModelObservationConventionTests {

	private DashScopeChatModelObservationConvention convention;

	private ChatModelObservationContext context;

	@BeforeEach
	void setUp() {
		// Initialize the convention and create a basic context
		convention = new DashScopeChatModelObservationConvention();

		// Create a basic prompt with a user message
		Prompt prompt = new Prompt(List.of(new UserMessage("Test message")));

		// Create context with DashScope specific options
		context = ChatModelObservationContext.builder()
			.prompt(prompt)
			.provider("dashscope")
			.requestOptions(DashScopeChatOptions.builder().withModel("qwen-turbo").build())
			.build();
	}

	@Test
	void testGetName() {
		// Test that the convention returns the correct name
		assertThat(convention.getName()).isEqualTo(DashScopeChatModelObservationConvention.DEFAULT_NAME)
			.isEqualTo("gen_ai.client.operation");
	}

	@Test
	void testRequestStopSequencesWithEmptyStop() {
		// Test handling of empty stop sequences
		KeyValues keyValues = KeyValues.empty();
		KeyValues result = convention.requestStopSequences(keyValues, context);

		// Should return the original keyValues when no stop sequences are present
		assertThat(result).isEqualTo(keyValues);
	}

	// 添加一个简单的 ChatOptions 实现类用于测试
	private static class TestChatOptions implements ChatOptions {

		@Override
		public ChatOptions copy() {
			return new TestChatOptions();
		}

		@Override
		public String getModel() {
			return "";
		}

		@Override
		public Double getFrequencyPenalty() {
			return 0.0;
		}

		@Override
		public Integer getMaxTokens() {
			return 0;
		}

		@Override
		public Double getPresencePenalty() {
			return 0.0;
		}

		@Override
		public List<String> getStopSequences() {
			return List.of();
		}

		@Override
		public Double getTemperature() {
			return 0.0;
		}

		@Override
		public Integer getTopK() {
			return 0;
		}

		@Override
		public Double getTopP() {
			return 0.0;
		}

	}

}
