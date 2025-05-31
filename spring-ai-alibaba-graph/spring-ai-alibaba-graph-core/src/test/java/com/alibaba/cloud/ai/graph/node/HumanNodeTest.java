package com.alibaba.cloud.ai.graph.node;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HumanNodeTest {

	@Test
	void testCustomInterruptMessage() throws GraphStateException {
		StateGraph stateGraph = new StateGraph("humanNodeTest", OverAllState::new);
		String interruptMessage = "This is a custom Interrupt message";
		HumanNode humanNode = new HumanNode("always", null, interruptMessage);
		stateGraph.addNode("humanNode", AsyncNodeAction.node_async(humanNode));
		stateGraph.addEdge(StateGraph.START, "humanNode").addEdge("humanNode", StateGraph.END);
		CompiledGraph compile = stateGraph.compile();
		OverAllState overAllState = compile.invoke(Map.of("input", "test")).orElseThrow();
		assertEquals(interruptMessage, overAllState.interruptMessage());
	}

}
