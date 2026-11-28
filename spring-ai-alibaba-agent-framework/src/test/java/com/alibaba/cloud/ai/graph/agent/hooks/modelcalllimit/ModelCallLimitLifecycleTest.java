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
package com.alibaba.cloud.ai.graph.agent.hooks.modelcalllimit;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitExceededException;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelCallLimitLifecycleTest {

	@Test
	void accumulatesOnlyWithinTheSameCheckpointThread() throws Exception {
		var calls = new AtomicInteger();
		var hook = ModelCallLimitHook.builder().threadLimit(2).runLimit(1)
			.exitBehavior(ModelCallLimitHook.ExitBehavior.ERROR).build();
		var agent = ReactAgent.builder().name("model-budget").model(fakeModel(calls))
			.hooks(List.of(hook)).saver(new MemorySaver()).build();

		assertEquals("ok", agent.call("first", config("thread-a")).getText());
		assertEquals("ok", agent.call("second", config("thread-a")).getText());
		assertEquals("ok", agent.call("independent", config("thread-b")).getText());
		var exhausted = assertThrows(ModelCallLimitExceededException.class,
			() -> agent.call("third", config("thread-a")));
		assertEquals(2, exhausted.getThreadCount());
		assertEquals(0, exhausted.getRunCount());
		assertEquals(3, calls.get());
	}

	@Test
	void permitsIndependentInvocationsWhenNoSaverIsConfigured() throws Exception {
		var calls = new AtomicInteger();
		var hook = ModelCallLimitHook.builder().threadLimit(1).build();
		var agent = ReactAgent.builder().name("stateless").model(fakeModel(calls))
			.hooks(List.of(hook)).build();

		assertEquals("ok", agent.call("one", config("thread-a")).getText());
		assertEquals("ok", agent.call("two", config("thread-a")).getText());
		assertEquals(2, calls.get());
	}

	@Test
	void endsBeforeCallingTheModelWhenTheCheckpointThreadIsExhausted() throws Exception {
		var calls = new AtomicInteger();
		var hook = ModelCallLimitHook.builder().threadLimit(1).build();
		var agent = ReactAgent.builder().name("model-budget-end").model(fakeModel(calls))
			.hooks(List.of(hook)).saver(new MemorySaver()).build();

		assertEquals("ok", agent.call("first", config("thread-a")).getText());
		assertEquals("Model call limits exceeded: thread limit (1/1)",
			agent.call("second", config("thread-a")).getText());
		assertEquals(1, calls.get());
	}

	private static RunnableConfig config(String thread) {
		return RunnableConfig.builder().threadId(thread).addMetadata("_stream_", false).build();
	}

	private static ChatModel fakeModel(AtomicInteger calls) {
		return prompt -> {
			calls.incrementAndGet();
			return new ChatResponse(List.of(new Generation(new AssistantMessage("ok"))));
		};
	}

}
