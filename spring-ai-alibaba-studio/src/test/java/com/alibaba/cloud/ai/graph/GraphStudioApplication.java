/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under
 * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
// package com.alibaba.cloud.ai.graph;
//
// import java.util.Map;
//
// import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
// import com.alibaba.cloud.ai.graph.action.EdgeAction;
// import com.alibaba.cloud.ai.graph.prebuilt.MessagesState;
// import com.alibaba.cloud.ai.graph.prebuilt.MessagesStateGraph;
// import com.alibaba.cloud.ai.graph.serializer.AgentState;
// import org.checkerframework.checker.units.qual.C;
//
// import org.springframework.boot.SpringApplication;
// import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.ComponentScan;
// import org.springframework.context.annotation.PropertySource;
//
// import static com.alibaba.cloud.ai.graph.StateGraph.END;
// import static com.alibaba.cloud.ai.graph.StateGraph.START;
// import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
// import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
//
// @SpringBootApplication
// @ComponentScan("com.alibaba.cloud.ai")
// public class GraphStudioApplication {
//
// public static void main(String[] args) {
// SpringApplication.run(GraphStudioApplication.class, args);
// }
//
// @Bean
// public StateGraph stateGraph() throws GraphStateException {
// return sampleFlow();
// }
//
// private StateGraph<AgentState> sampleFlow() throws GraphStateException {
//
// final EdgeAction<AgentState> conditionalAge = new EdgeAction<>() {
// int steps = 0;
//
// @Override
// public String apply(AgentState state) {
// if (++steps == 2) {
// steps = 0;
// return "end";
// }
// return "next";
// }
// };
//
// return new StateGraph<>(AgentState::new).addNode("agent", node_async((state) -> {
// System.out.println("agent ");
// System.out.println(state);
// if (state.value("action_response").isPresent()) {
// return Map.of("agent_summary", "This is just a DEMO summary");
// }
// return Map.of("agent_response", "This is an Agent DEMO response");
// })).addNode("action", node_async(state -> {
// System.out.print("action: ");
// System.out.println(state);
// return Map.of("action_response", "This is an Action DEMO response");
// }))
// .addEdge(START, "agent")
// .addEdge("action", "agent")
// .addConditionalEdges("agent", edge_async(conditionalAge), Map.of("next", "action",
// "end", END));
//
// }
//
// private StateGraph<MessagesState<String>> withStateSubgraphSample() throws
// GraphStateException {
// var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
// .addNode("B2", _makeNode("B2"))
// .addNode("C", _makeNode("subgraph(C)"))
// .addEdge(START, "B1")
// .addEdge("B1", "B2")
// .addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
// .addEdge("C", END);
//
// var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
// .addSubgraph("B", workflowChild)
// .addNode("C", _makeNode("C"))
// .addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
// .addEdge("A", "B")
// .addEdge("B", "C")
// .addEdge("C", END)
// // .compile(compileConfig)
// ;
//
// return workflowParent;
//
// }
//
// private StateGraph withCompiledSubgraphSample() throws GraphStateException {
// var workflowChild = new MessagesStateGraph<String>().addNode("B1", _makeNode("B1"))
// .addNode("B2", _makeNode("B2"))
// .addNode("C", _makeNode("subgraph(C)"))
// .addEdge(START, "B1")
// .addEdge("B1", "B2")
// .addConditionalEdges("B2", edge_async(state -> "c"), Map.of(END, END, "c", "C"))
// .addEdge("C", END)
// .compile();
//
// var workflowParent = new MessagesStateGraph<String>().addNode("A", _makeNode("A"))
// .addSubgraph("B", workflowChild)
// .addNode("C", _makeNode("C"))
// .addConditionalEdges(START, edge_async(state -> "a"), Map.of("a", "A", "b", "B"))
// .addEdge("A", "B")
// .addEdge("B", "C")
// .addEdge("C", END)
// // .compile(compileConfig)
// ;
//
// return workflowParent;
// }
//
// private AsyncNodeAction<MessagesState<String>> _makeNode(String id) {
// return node_async(state -> Map.of("messages", id));
// }
//
// }
