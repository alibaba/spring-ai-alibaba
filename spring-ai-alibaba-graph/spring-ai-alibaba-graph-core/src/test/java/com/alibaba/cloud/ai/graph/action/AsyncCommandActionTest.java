package com.alibaba.cloud.ai.graph.action;

import static org.junit.jupiter.api.Assertions.*;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphRepresentation;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AsyncCommandActionTest {

	@Test
	void updateStateTest() throws GraphStateException {
		StateGraph graph = new StateGraph(() -> {
			HashMap<String, KeyStrategy> stringKeyStrategyHashMap = new HashMap<>();
			stringKeyStrategyHashMap.put("messages", new ReplaceStrategy());
			return stringKeyStrategyHashMap;
		});

		NodeAction mkNode = s -> {
			assertEquals("node1", s.value("messages", ""));
			return Map.of();
		};
		graph.addNode("node1", AsyncNodeAction.node_async(mkNode));

		CommandAction commandAction = (state, config) -> {
			return new Command("node1", Map.of("messages", "node1"));
		};
		graph.addConditionalEdges(StateGraph.START, AsyncCommandAction.node_async(commandAction),
				Map.of("node1", "node1"));

		graph.addEdge("node1", StateGraph.END);

		CompiledGraph compile = graph.compile();
		System.out.println(compile.getGraph(GraphRepresentation.Type.PLANTUML).content());

		System.out.println(compile.invoke(Map.of()).orElseThrow());
	}

}
