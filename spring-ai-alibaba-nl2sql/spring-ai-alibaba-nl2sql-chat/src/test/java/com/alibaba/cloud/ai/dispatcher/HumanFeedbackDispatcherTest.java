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
}
