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

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static org.junit.jupiter.api.Assertions.*;

class HumanFeedbackNodeTest {

	private HumanFeedbackNode node;

	private OverAllState state;

	@BeforeEach
	void setUp() {
		node = new HumanFeedbackNode();
		state = new OverAllState();
		state.registerKeyAndStrategy(PLAN_REPAIR_COUNT, new ReplaceStrategy());
		state.registerKeyAndStrategy(PLAN_CURRENT_STEP, new ReplaceStrategy());
		state.registerKeyAndStrategy(HUMAN_REVIEW_ENABLED, new ReplaceStrategy());
		state.registerKeyAndStrategy(PLAN_VALIDATION_ERROR, new ReplaceStrategy());
	}

	@Test
	void testApproveFlow() throws Exception {
		state.withHumanFeedback(new OverAllState.HumanFeedback(Map.of("feed_back", true), null));

		Map<String, Object> result = node.apply(state);
		assertEquals(PLAN_EXECUTOR_NODE, result.get("human_next_node"));
		assertEquals(false, result.get(HUMAN_REVIEW_ENABLED));
		assertFalse(result.containsKey(PLAN_VALIDATION_ERROR));
	}

	@Test
	void testRejectFlowWithContent() throws Exception {
		state.updateState(Map.of(PLAN_REPAIR_COUNT, 0));
		state.withHumanFeedback(
				new OverAllState.HumanFeedback(Map.of("feed_back", false, "feed_back_content", "需要补充过滤条件"), null));

		Map<String, Object> result = node.apply(state);
		assertEquals(PLANNER_NODE, result.get("human_next_node"));
		assertEquals(1, result.get(PLAN_REPAIR_COUNT));
		assertEquals(1, result.get(PLAN_CURRENT_STEP));
		assertEquals(true, result.get(HUMAN_REVIEW_ENABLED));
		assertEquals("需要补充过滤条件", result.get(PLAN_VALIDATION_ERROR));
	}

	@Test
	void testRejectFlowWithoutContent() throws Exception {
		state.updateState(Map.of(PLAN_REPAIR_COUNT, 2));
		state.withHumanFeedback(new OverAllState.HumanFeedback(Map.of("feed_back", false), null));

		Map<String, Object> result = node.apply(state);
		assertEquals(PLANNER_NODE, result.get("human_next_node"));
		assertEquals(3, result.get(PLAN_REPAIR_COUNT));
		assertEquals(1, result.get(PLAN_CURRENT_STEP));
		assertEquals(true, result.get(HUMAN_REVIEW_ENABLED));
		assertEquals("Plan rejected by user", result.get(PLAN_VALIDATION_ERROR));
	}

	@Test
	void testWaitForFeedback() throws Exception {
		Map<String, Object> result = node.apply(state);
		assertEquals("WAIT_FOR_FEEDBACK", result.get("human_next_node"));
	}

	@Test
	void testMaxRepairExceeded() throws Exception {
		state.updateState(Map.of(PLAN_REPAIR_COUNT, 3));
		Map<String, Object> result = node.apply(state);
		assertEquals("END", result.get("human_next_node"));
	}

	@Test
	void testRejectFlowClearsPlanNextNode() throws Exception {
		state.updateState(Map.of(PLAN_REPAIR_COUNT, 0));
		state.withHumanFeedback(
				new OverAllState.HumanFeedback(Map.of("feed_back", false, "feed_back_content", "再次修正"), null));

		Map<String, Object> result = node.apply(state);
		assertEquals(PLANNER_NODE, result.get("human_next_node"));
		assertEquals(1, result.get(PLAN_REPAIR_COUNT));
		assertEquals(1, result.get(PLAN_CURRENT_STEP));
		assertEquals(true, result.get(HUMAN_REVIEW_ENABLED));
		assertEquals("", result.getOrDefault(PLAN_NEXT_NODE, ""));
	}

}
