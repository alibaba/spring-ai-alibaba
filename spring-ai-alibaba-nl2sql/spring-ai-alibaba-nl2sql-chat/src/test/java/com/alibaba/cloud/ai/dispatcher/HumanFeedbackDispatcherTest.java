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

package com.alibaba.cloud.ai.dispatcher;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HumanFeedbackDispatcherTest {

	private HumanFeedbackDispatcher dispatcher;

	private OverAllState state;

	@BeforeEach
	void setUp() {
		dispatcher = new HumanFeedbackDispatcher();
		state = new OverAllState();
	}

	@Test
	void testWaitForFeedbackReturnsEND() throws Exception {
		state.updateState(java.util.Map.of("human_next_node", "WAIT_FOR_FEEDBACK"));
		String next = dispatcher.apply(state);
		assertEquals(StateGraph.END, next);
	}

	@Test
	void testNormalRouting() throws Exception {
		state.updateState(java.util.Map.of("human_next_node", "PLANNER_NODE"));
		String next = dispatcher.apply(state);
		assertEquals("PLANNER_NODE", next);
	}

	@Test
	void testDefaultToENDWhenMissingKey() throws Exception {
		String next = dispatcher.apply(state);
		assertEquals(StateGraph.END, next);
	}

	@Test
	void testRoutesToPlannerAfterRejection() throws Exception {
		state.updateState(java.util.Map.of("human_next_node", "PLANNER_NODE"));
		String next = dispatcher.apply(state);
		assertEquals("PLANNER_NODE", next);
	}

}
