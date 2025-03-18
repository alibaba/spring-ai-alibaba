/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncSendEdgeAction;
import com.alibaba.cloud.ai.graph.action.SendEdgeAction;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.SubGraphTest.*;

public class SendEdgeGraphTest {

	@Test
	public void sendEdgeGraphTest01() throws Exception {
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addConditionalEdges("B1", AsyncSendEdgeAction.edge_async(t -> {
				Send send = new Send();
				send.setGraph(GraphType.PARENT);
				send.setEdge("C");
				return send;
			}))
			.addEdge("B2", END);

		var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addEdge(START, "B")
			.addEdge("B", "A")
			.addEdge("A", "C")
			.addEdge("C", END);

		var app = workflowParent.compile();
		_execute(app, Map.of());
	}

	@Test
	public void sendEdgeGraphTest02() throws Exception {
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addConditionalEdges("B1", AsyncSendEdgeAction.edge_async(t -> {
				Send send = new Send();
				send.setNodeId("B");
				send.setGraph(GraphType.CURRENT);
				send.setEdge("B2");
				return send;
			}))
			.addEdge("B2", END);

		var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addEdge(START, "B")
			.addEdge("B", "A")
			.addEdge("A", "C")
			.addEdge("C", END);

		var app = workflowParent.compile();
		_execute(app, Map.of());
	}

	@Test
	public void sendEdgeGraphTest03() throws Exception {
		var workflowChild = new StateGraph().addNode("B1", _makeNode("B1"))
			.addNode("B2", _makeNode("B2"))
			.addEdge(START, "B1")
			.addConditionalEdges("B1", AsyncSendEdgeAction.edge_async(t -> {
				Send send = new Send();
				send.setGraph(GraphType.PARENT);
				send.setEdge("E");
				return send;
			}))
			.addEdge("B2", END);

		var workflowChild2 = new StateGraph().addNode("C1", _makeNode("C1"))
			.addNode("C2", _makeNode("C2"))
			.addEdge(START, "C1")
			.addConditionalEdges("C1", AsyncSendEdgeAction.edge_async(t -> {
				Send send = new Send();
				send.setNodeId("B");
				send.setGraph(GraphType.CHILD);
				send.setEdge("B2");
				return send;
			}))
			.addEdge("C2", END);

		var workflowParent = new StateGraph(getOverAllState()).addNode("A", _makeNode("A"))
			.addSubgraph("B", workflowChild)
			.addNode("C", _makeNode("C"))
			.addSubgraph("D", workflowChild2)
			.addNode("E", _makeNode("E"))
			.addEdge(START, "A")
			.addEdge("A", "B")
			.addEdge("B", "C")
			.addEdge("C", "E")
			.addEdge("E", "D")
			.addEdge("D", END);

		var app = workflowParent.compile();
		_execute(app, Map.of());
	}

}
