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
package com.alibaba.cloud.ai.graph.agent.flow.agent.loop;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrayLoopStrategyTest {

	@Test
	void shouldDispatchArrayElementsInBatches() {
		ArrayLoopStrategy strategy = LoopMode.array(2, messages -> List.of("one", "two", "three", "four", "five"));
		Map<String, Object> state = new HashMap<>(strategy.loopInit(stateWithMessage("ignored")));

		Map<String, Object> first = strategy.loopDispatch(new OverAllState(state));
		assertDispatch(first, strategy, 2, "[\"one\",\"two\"]");
		state.putAll(first);

		Map<String, Object> second = strategy.loopDispatch(new OverAllState(state));
		assertDispatch(second, strategy, 4, "[\"three\",\"four\"]");
		state.putAll(second);

		Map<String, Object> last = strategy.loopDispatch(new OverAllState(state));
		assertDispatch(last, strategy, 5, "[\"five\"]");
		state.putAll(last);

		Map<String, Object> completed = strategy.loopDispatch(new OverAllState(state));
		assertFalse((Boolean) completed.get(strategy.loopFlagKey()));
	}

	@Test
	void defaultArrayModeShouldKeepSingleElementMessages() {
		ArrayLoopStrategy strategy = LoopMode.array(messages -> List.of("one", "two"));
		Map<String, Object> state = new HashMap<>(strategy.loopInit(stateWithMessage("ignored")));

		Map<String, Object> first = strategy.loopDispatch(new OverAllState(state));

		assertDispatch(first, strategy, 1, "one");
	}

	@Test
	void shouldRejectNonPositiveBatchSize() {
		assertThrows(IllegalArgumentException.class, () -> LoopMode.array(0));
		assertThrows(IllegalArgumentException.class, () -> LoopMode.array(-1, messages -> List.of()));
	}

	private static OverAllState stateWithMessage(String text) {
		return new OverAllState(Map.of(LoopStrategy.MESSAGE_KEY, List.of(new UserMessage(text))));
	}

	private static void assertDispatch(Map<String, Object> update, ArrayLoopStrategy strategy, int expectedIndex,
			String expectedMessage) {
		assertTrue((Boolean) update.get(strategy.loopFlagKey()));
		assertEquals(expectedIndex, update.get(strategy.loopCountKey()));
		Message message = (Message) update.get(LoopStrategy.MESSAGE_KEY);
		assertEquals(expectedMessage, message.getText());
	}

}
