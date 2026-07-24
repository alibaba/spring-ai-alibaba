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
package com.alibaba.cloud.ai.graph.agent;

import com.alibaba.cloud.ai.graph.CompileConfig;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.observation.ToolCallingObservationContext;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AgentToolObservationTest {

	@Test
	void shouldCreateSpringAiObservationForToolExecution() throws Exception {
		assertToolObservation(false);
	}

	@Test
	void shouldUseObservationRegistryFromCompileConfig() throws Exception {
		assertToolObservation(true);
	}

	private static void assertToolObservation(boolean useCompileConfigRegistry) throws Exception {
		ObservationRegistry observationRegistry = ObservationRegistry.create();
		List<ToolCallingObservationContext> stoppedToolObservations = new CopyOnWriteArrayList<>();
		observationRegistry.observationConfig()
				.observationHandler(new ObservationHandler<ToolCallingObservationContext>() {
					@Override
					public void onStop(ToolCallingObservationContext context) {
						stoppedToolObservations.add(context);
					}

					@Override
					public boolean supportsContext(Observation.Context context) {
						return context instanceof ToolCallingObservationContext;
					}
				});
		ToolCallback observedTool = FunctionToolCallback.builder("observed_tool",
				(ObservedRequest request) -> "observed:" + request.value)
			.description("Returns an observed value")
			.inputType(ObservedRequest.class)
			.build();
		Builder agentBuilder = ReactAgent.builder()
			.name("observed_agent")
			.model(new ToolCallingChatModel())
			.tools(observedTool);
		if (useCompileConfigRegistry) {
			agentBuilder.compileConfig(CompileConfig.builder().observationRegistry(observationRegistry).build());
		}
		else {
			agentBuilder.observationRegistry(observationRegistry);
		}
		ReactAgent agent = agentBuilder.build();

		agent.call("invoke the observed tool");

		assertEquals(1, stoppedToolObservations.size());
		ToolCallingObservationContext context = stoppedToolObservations.get(0);
		assertEquals("spring.ai.tool", context.getName());
		assertEquals("tool_call", context.getLowCardinalityKeyValue("spring.ai.kind").getValue());
		assertEquals("observed_tool", context.getToolDefinition().name());
		assertEquals("{\"value\":\"hello\"}", context.getToolCallArguments());
		assertEquals("\"observed:hello\"", context.getToolCallResult());
	}

	private static class ToolCallingChatModel implements ChatModel {

		private final AtomicInteger callCount = new AtomicInteger();

		@Override
		public ChatResponse call(Prompt prompt) {
			AssistantMessage response = callCount.getAndIncrement() == 0
					? AssistantMessage.builder()
						.content("")
						.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "observed_tool",
								"{\"value\":\"hello\"}")))
						.build()
					: new AssistantMessage("done");
			return new ChatResponse(List.of(new Generation(response)));
		}

		@Override
		public Flux<ChatResponse> stream(Prompt prompt) {
			return Flux.just(call(prompt));
		}
	}

	private static class ObservedRequest {

		public String value;
	}

}
