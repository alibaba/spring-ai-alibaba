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
package com.alibaba.cloud.ai.dashscope.chat;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.model.function.FunctionCallback;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeChatOptions
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeChatOptionsTests {

	private static final String TEST_MODEL = "qwen-turbo";

	private static final Double TEST_TEMPERATURE = 0.7;

	private static final Double TEST_TOP_P = 0.8;

	private static final Integer TEST_TOP_K = 50;

	private static final Integer TEST_SEED = 42;

	private static final Double TEST_REPETITION_PENALTY = 1.1;

	@Test
	void testBuilderAndGetters() {
		// Test building DashScopeChatOptions using builder pattern and verify getters
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.withModel(TEST_MODEL)
			.withTemperature(TEST_TEMPERATURE)
			.withTopP(TEST_TOP_P)
			.withTopK(TEST_TOP_K)
			.withSeed(TEST_SEED)
			.withRepetitionPenalty(TEST_REPETITION_PENALTY)
			.withStream(true)
			.withEnableSearch(true)
			.withIncrementalOutput(true)
			.withVlHighResolutionImages(true)
			.withMultiModel(true)
			.build();

		// Verify all fields are set correctly
		assertThat(options.getModel()).isEqualTo(TEST_MODEL);
		assertThat(options.getTemperature()).isEqualTo(TEST_TEMPERATURE);
		assertThat(options.getTopP()).isEqualTo(TEST_TOP_P);
		assertThat(options.getTopK()).isEqualTo(TEST_TOP_K);
		assertThat(options.getSeed()).isEqualTo(TEST_SEED);
		assertThat(options.getRepetitionPenalty()).isEqualTo(TEST_REPETITION_PENALTY);
		assertThat(options.getStream()).isTrue();
		assertThat(options.getEnableSearch()).isTrue();
		assertThat(options.getIncrementalOutput()).isTrue();
		assertThat(options.getVlHighResolutionImages()).isTrue();
		assertThat(options.getMultiModel()).isTrue();
	}

	@Test
	void testSettersAndGetters() {
		// Test setters and getters
		DashScopeChatOptions options = new DashScopeChatOptions();

		options.setModel(TEST_MODEL);
		options.setTemperature(TEST_TEMPERATURE);
		options.setTopP(TEST_TOP_P);
		options.setTopK(TEST_TOP_K);
		options.setSeed(TEST_SEED);
		options.setRepetitionPenalty(TEST_REPETITION_PENALTY);
		options.setStream(true);
		options.setEnableSearch(true);
		options.setIncrementalOutput(true);
		options.setVlHighResolutionImages(true);
		options.setMultiModel(true);

		// Verify all fields are set correctly
		assertThat(options.getModel()).isEqualTo(TEST_MODEL);
		assertThat(options.getTemperature()).isEqualTo(TEST_TEMPERATURE);
		assertThat(options.getTopP()).isEqualTo(TEST_TOP_P);
		assertThat(options.getTopK()).isEqualTo(TEST_TOP_K);
		assertThat(options.getSeed()).isEqualTo(TEST_SEED);
		assertThat(options.getRepetitionPenalty()).isEqualTo(TEST_REPETITION_PENALTY);
		assertThat(options.getStream()).isTrue();
		assertThat(options.getEnableSearch()).isTrue();
		assertThat(options.getIncrementalOutput()).isTrue();
		assertThat(options.getVlHighResolutionImages()).isTrue();
		assertThat(options.getMultiModel()).isTrue();
	}

	@Test
	void testFunctionCallbacks() {
		// Test function callbacks related methods
		FunctionCallback callback1 = Mockito.mock(FunctionCallback.class);
		FunctionCallback callback2 = Mockito.mock(FunctionCallback.class);
		Mockito.when(callback1.getName()).thenReturn("test1");
		Mockito.when(callback2.getName()).thenReturn("test2");

		List<FunctionCallback> callbacks = Arrays.asList(callback1, callback2);
		Set<String> functions = new HashSet<>(Arrays.asList("test1", "test2"));

		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.withFunctionCallbacks(callbacks)
			.withFunctions(functions)
			.build();

		assertThat(options.getFunctionCallbacks()).containsExactlyElementsOf(callbacks);
		assertThat(options.getFunctions()).containsExactlyInAnyOrderElementsOf(functions);
	}

	@Test
	void testToolsAndToolChoice() {
		// Test tools and tool choice related methods
		DashScopeApi.FunctionTool.Function function = new DashScopeApi.FunctionTool.Function("Test function", "test",
				"{}");
		DashScopeApi.FunctionTool tool = new DashScopeApi.FunctionTool(function);
		List<DashScopeApi.FunctionTool> tools = Collections.singletonList(tool);
		Map<String, String> toolChoice = Map.of("type", "function", "name", "test");

		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.withTools(tools)
			.withToolChoice(toolChoice)
			.build();

		assertThat(options.getTools()).containsExactlyElementsOf(tools);
		assertThat(options.getToolChoice()).isEqualTo(toolChoice);
	}

	@Test
	void testResponseFormat() {
		// Test response format related methods
		DashScopeResponseFormat responseFormat = DashScopeResponseFormat.builder()
			.type(DashScopeResponseFormat.Type.JSON_OBJECT)
			.build();

		DashScopeChatOptions options = DashScopeChatOptions.builder().withResponseFormat(responseFormat).build();

		assertThat(options.getResponseFormat()).isEqualTo(responseFormat);
		assertThat(options.getResponseFormat().getType()).isEqualTo(DashScopeResponseFormat.Type.JSON_OBJECT);
	}

	@Test
	void testCopy() {
		// Test copy method
		DashScopeChatOptions original = DashScopeChatOptions.builder()
			.withModel(TEST_MODEL)
			.withTemperature(TEST_TEMPERATURE)
			.withTopP(TEST_TOP_P)
			.withTopK(TEST_TOP_K)
			.build();

		DashScopeChatOptions copy = (DashScopeChatOptions) original.copy();

		assertThat(copy).usingRecursiveComparison().isEqualTo(original);
		assertThat(copy).isNotSameAs(original);
	}

	@Test
	void testEqualsAndHashCode() {
		// Test equals and hashCode methods
		DashScopeChatOptions options1 = DashScopeChatOptions.builder()
			.withModel(TEST_MODEL)
			.withTemperature(TEST_TEMPERATURE)
			.build();

		DashScopeChatOptions options2 = DashScopeChatOptions.builder()
			.withModel(TEST_MODEL)
			.withTemperature(TEST_TEMPERATURE)
			.build();

		DashScopeChatOptions options3 = DashScopeChatOptions.builder()
			.withModel("different-model")
			.withTemperature(0.5)
			.build();

		assertThat(options1).isEqualTo(options2);
		assertThat(options1.hashCode()).isEqualTo(options2.hashCode());
		assertThat(options1).isNotEqualTo(options3);
		assertThat(options1.hashCode()).isNotEqualTo(options3.hashCode());
	}

	@Test
	void testToString() {
		// Test toString method
		DashScopeChatOptions options = DashScopeChatOptions.builder()
			.withModel(TEST_MODEL)
			.withTemperature(TEST_TEMPERATURE)
			.build();

		String toString = options.toString();

		assertThat(toString).contains("DashScopeChatOptions")
			.contains(TEST_MODEL)
			.contains(TEST_TEMPERATURE.toString());
	}

}
