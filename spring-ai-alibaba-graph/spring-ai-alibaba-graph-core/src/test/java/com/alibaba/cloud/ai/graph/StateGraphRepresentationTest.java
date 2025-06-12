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

package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.utils.EdgeMappings;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig.node_async;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StateGraphRepresentationTest {

	// Dummy node action that returns an empty map as result
	CompletableFuture<Map<String, Object>> dummyNodeAction(OverAllState state) {
		return CompletableFuture.completedFuture(Map.of());
	}

	// Dummy condition function that always returns an empty string
	CompletableFuture<String> dummyCondition(OverAllState state) {
		return CompletableFuture.completedFuture("");
	}

	private OverAllStateFactory overAllStateFactory = () -> new OverAllState();

	private KeyStrategyFactory keyStrategyFactory = () -> {
		HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
		return new HashMap<>();
	};

	/**
	 * Test a simple graph structure with three nodes and sequential edges. Verifies the
	 * PlantUML representation of the graph structure.
	 */
	@Test
	public void testSimpleGraph() throws Exception {

		StateGraph workflow = new StateGraph(keyStrategyFactory).addNode("agent_3", this::dummyNodeAction)
			.addNode("agent_1", this::dummyNodeAction)
			.addNode("agent_2", this::dummyNodeAction)
			.addEdge(START, "agent_1")
			.addEdge("agent_2", END)
			.addEdge("agent_1", "agent_3")
			.addEdge("agent_3", "agent_2");

		CompiledGraph app = workflow.compile();

		GraphRepresentation result = app.getGraph(GraphRepresentation.Type.PLANTUML);
		assertEquals(GraphRepresentation.Type.PLANTUML, result.type());

		assertEquals("""
				@startuml Graph_Diagram
				skinparam usecaseFontSize 14
				skinparam usecaseStereotypeFontSize 12
				skinparam hexagonFontSize 14
				skinparam hexagonStereotypeFontSize 12
				title "Graph Diagram"
				footer

				powered by spring-ai-alibaba
				end footer
				circle start<<input>> as __START__
				circle stop as __END__
				usecase "agent_3"<<Node>>
				usecase "agent_1"<<Node>>
				usecase "agent_2"<<Node>>
				"__START__" -down-> "agent_1"
				"agent_2" -down-> "__END__"
				"agent_1" -down-> "agent_3"
				"agent_3" -down-> "agent_2"
				@enduml
				""", result.content());
	}

	/**
	 * Test a correction process graph with conditional edges. Verifies both PlantUML and
	 * Mermaid representations.
	 */
	@Test
	public void testCorrectionProcessGraph() throws Exception {

		var workflow = new StateGraph(keyStrategyFactory).addNode("evaluate_result", this::dummyNodeAction)
			.addNode("agent_review", this::dummyNodeAction)
			.addEdge("agent_review", "evaluate_result")
			.addConditionalEdges("evaluate_result", this::dummyCondition,
					EdgeMappings.builder().toEND("OK").toEND("UNKNOWN").to("agent_review", "ERROR").build())
			.addEdge(START, "evaluate_result");

		var result = workflow.getGraph(GraphRepresentation.Type.PLANTUML, "Correction process");

		assertEquals(GraphRepresentation.Type.PLANTUML, result.type());

		assertEquals("""
				@startuml Correction_process
				skinparam usecaseFontSize 14
				skinparam usecaseStereotypeFontSize 12
				skinparam hexagonFontSize 14
				skinparam hexagonStereotypeFontSize 12
				title "Correction process"
				footer

				powered by spring-ai-alibaba
				end footer
				circle start<<input>> as __START__
				circle stop as __END__
				usecase "evaluate_result"<<Node>>
				usecase "agent_review"<<Node>>
				hexagon "check state" as condition1<<Condition>>
				"__START__" -down-> "evaluate_result"
				"agent_review" -down-> "evaluate_result"
				"evaluate_result" .down.> "condition1"
				"condition1" .down.> "__END__": "OK"
				'"evaluate_result" .down.> "__END__": "OK"
				"condition1" .down.> "__END__": "UNKNOWN"
				'"evaluate_result" .down.> "__END__": "UNKNOWN"
				"condition1" .down.> "agent_review": "ERROR"
				'"evaluate_result" .down.> "agent_review": "ERROR"
				@enduml
				""", result.content());
	}

	/**
	 * Test an agent executor graph with conditional routing. Verifies the PlantUML
	 * representation of the graph.
	 */
	@Test
	public void GenerateAgentExecutorGraph() throws Exception {
		StateGraph workflow = new StateGraph(keyStrategyFactory).addNode("agent", this::dummyNodeAction)
			.addNode("action", this::dummyNodeAction)
			.addEdge(START, "agent")
			.addConditionalEdges("agent", this::dummyCondition,
					EdgeMappings.builder().to("action", "continue").toEND("end").build())
			.addEdge("action", "agent");

		CompiledGraph app = workflow.compile();

		GraphRepresentation result = app.getGraph(GraphRepresentation.Type.PLANTUML);
		assertEquals(GraphRepresentation.Type.PLANTUML, result.type());

		assertEquals("""
				@startuml Graph_Diagram
				skinparam usecaseFontSize 14
				skinparam usecaseStereotypeFontSize 12
				skinparam hexagonFontSize 14
				skinparam hexagonStereotypeFontSize 12
				title "Graph Diagram"
				footer

				powered by spring-ai-alibaba
				end footer
				circle start<<input>> as __START__
				circle stop as __END__
				usecase "agent"<<Node>>
				usecase "action"<<Node>>
				hexagon "check state" as condition1<<Condition>>
				"__START__" -down-> "agent"
				"agent" .down.> "condition1"
				"condition1" .down.> "action": "continue"
				'"agent" .down.> "action": "continue"
				"condition1" .down.> "__END__": "end"
				'"agent" .down.> "__END__": "end"
				"action" -down-> "agent"
				@enduml
				""", result.content());
	}

	/**
	 * Test a complex graph for image-to-diagram conversion. Verifies both PlantUML and
	 * Mermaid representations.
	 */
	@Test
	public void GenerateImageToDiagramGraph() throws Exception {
		StateGraph workflow = new StateGraph(keyStrategyFactory).addNode("agent_describer", this::dummyNodeAction)
			.addNode("agent_sequence_plantuml", this::dummyNodeAction)
			.addNode("agent_generic_plantuml", this::dummyNodeAction)
			.addConditionalEdges("agent_describer", this::dummyCondition,
					EdgeMappings.builder()
						.to("agent_generic_plantuml", "generic")
						.to("agent_sequence_plantuml", "sequence")
						.build())
			.addNode("evaluate_result", this::dummyNodeAction)
			.addEdge("agent_sequence_plantuml", "evaluate_result")
			.addEdge("agent_generic_plantuml", "evaluate_result")
			.addEdge(START, "agent_describer")
			.addEdge("evaluate_result", END);

		CompiledGraph app = workflow.compile();

		GraphRepresentation result = app.getGraph(GraphRepresentation.Type.PLANTUML);
		assertEquals(GraphRepresentation.Type.PLANTUML, result.type());

		assertEquals("""
				@startuml Graph_Diagram
				skinparam usecaseFontSize 14
				skinparam usecaseStereotypeFontSize 12
				skinparam hexagonFontSize 14
				skinparam hexagonStereotypeFontSize 12
				title "Graph Diagram"
				footer

				powered by spring-ai-alibaba
				end footer
				circle start<<input>> as __START__
				circle stop as __END__
				usecase "agent_describer"<<Node>>
				usecase "agent_sequence_plantuml"<<Node>>
				usecase "agent_generic_plantuml"<<Node>>
				usecase "evaluate_result"<<Node>>
				hexagon "check state" as condition1<<Condition>>
				"__START__" -down-> "agent_describer"
				"agent_describer" .down.> "condition1"
				"condition1" .down.> "agent_generic_plantuml": "generic"
				'"agent_describer" .down.> "agent_generic_plantuml": "generic"
				"condition1" .down.> "agent_sequence_plantuml": "sequence"
				'"agent_describer" .down.> "agent_sequence_plantuml": "sequence"
				"agent_sequence_plantuml" -down-> "evaluate_result"
				"agent_generic_plantuml" -down-> "evaluate_result"
				"evaluate_result" -down-> "__END__"
				@enduml
				""", result.content());

		result = app.getGraph(GraphRepresentation.Type.MERMAID, "Graph Diagram", false);
		assertEquals(GraphRepresentation.Type.MERMAID, result.type());

		assertEquals("""
				---
				title: Graph Diagram
				---
				flowchart TD
				\t__START__((start))
				\t__END__((stop))
				\tagent_describer("agent_describer")
				\tagent_sequence_plantuml("agent_sequence_plantuml")
				\tagent_generic_plantuml("agent_generic_plantuml")
				\tevaluate_result("evaluate_result")
				\t%%	condition1{"check state"}
				\t__START__:::__START__ --> agent_describer:::agent_describer
				\t%%	agent_describer:::agent_describer -.-> condition1:::condition1
				\t%%	condition1:::condition1 -.->|generic| agent_generic_plantuml:::agent_generic_plantuml
				\tagent_describer:::agent_describer -.->|generic| agent_generic_plantuml:::agent_generic_plantuml
				\t%%	condition1:::condition1 -.->|sequence| agent_sequence_plantuml:::agent_sequence_plantuml
				\tagent_describer:::agent_describer -.->|sequence| agent_sequence_plantuml:::agent_sequence_plantuml
				\tagent_sequence_plantuml:::agent_sequence_plantuml --> evaluate_result:::evaluate_result
				\tagent_generic_plantuml:::agent_generic_plantuml --> evaluate_result:::evaluate_result
				\tevaluate_result:::evaluate_result --> __END__:::__END__

				\tclassDef ___START__ fill:black,stroke-width:1px,font-size:xx-small;
				\tclassDef ___END__ fill:black,stroke-width:1px,font-size:xx-small;
				""", result.content());
	}

	/**
	 * Creates a test node with a simple message output
	 * @param id Node identifier
	 * @return AsyncNodeActionWithConfig that returns a map with the node ID as message
	 */
	private AsyncNodeActionWithConfig makeNode(String id) {
		return node_async((t, config) -> Map.of("messages", id));
	}

	/**
	 * Test a graph with parallel branches. Verifies both PlantUML and Mermaid
	 * representations.
	 */
	@Test
	void testWithParallelBranch() throws Exception {

		var workflow = new StateGraph(keyStrategyFactory).addNode("A", makeNode("A"))
			.addNode("A1", makeNode("A1"))
			.addNode("A2", makeNode("A2"))
			.addNode("A3", makeNode("A3"))
			.addNode("B", makeNode("B"))
			.addNode("C", makeNode("C"))
			.addEdge("A", "A1")
			.addEdge("A", "A2")
			.addEdge("A", "A3")
			.addEdge("A1", "B")
			.addEdge("A2", "B")
			.addEdge("A3", "B")
			.addEdge("B", "C")
			.addEdge(START, "A")
			.addEdge("C", END);

		var result = workflow.getGraph(GraphRepresentation.Type.PLANTUML, "testWithParallelBranch");

		assertEquals("""
				@startuml testWithParallelBranch
				skinparam usecaseFontSize 14
				skinparam usecaseStereotypeFontSize 12
				skinparam hexagonFontSize 14
				skinparam hexagonStereotypeFontSize 12
				title "testWithParallelBranch"
				footer

				powered by spring-ai-alibaba
				end footer
				circle start<<input>> as __START__
				circle stop as __END__
				usecase "A"<<Node>>
				usecase "A1"<<Node>>
				usecase "A2"<<Node>>
				usecase "A3"<<Node>>
				usecase "B"<<Node>>
				usecase "C"<<Node>>
				"__START__" -down-> "A"
				"A" -down-> "A1"
				"A" -down-> "A2"
				"A" -down-> "A3"
				"A1" -down-> "B"
				"A2" -down-> "B"
				"A3" -down-> "B"
				"B" -down-> "C"
				"C" -down-> "__END__"
				@enduml
				""", result.content());

		result = workflow.getGraph(GraphRepresentation.Type.MERMAID, "testWithParallelBranch", false);

		assertEquals("""
				---
				title: testWithParallelBranch
				---
				flowchart TD
					__START__((start))
					__END__((stop))
					A("A")
					A1("A1")
					A2("A2")
					A3("A3")
					B("B")
					C("C")
					__START__:::__START__ --> A:::A
					A:::A --> A1:::A1
					A:::A --> A2:::A2
					A:::A --> A3:::A3
					A1:::A1 --> B:::B
					A2:::A2 --> B:::B
					A3:::A3 --> B:::B
					B:::B --> C:::C
					C:::C --> __END__:::__END__

					classDef ___START__ fill:black,stroke-width:1px,font-size:xx-small;
					classDef ___END__ fill:black,stroke-width:1px,font-size:xx-small;
				""", result.content());
	}

	/**
	 * Test a graph with parallel branches starting from the initial node. Verifies both
	 * PlantUML and Mermaid representations.
	 */
	@Test
	void testWithParallelBranchOnStart() throws Exception {

		var workflow = new StateGraph(() -> {
			HashMap<String, KeyStrategy> objectObjectHashMap = new HashMap<>();
			objectObjectHashMap.put("messages", new AppendStrategy());
			return objectObjectHashMap;
		}).addNode("A1", makeNode("A1"))
			.addNode("A2", makeNode("A2"))
			.addNode("A3", makeNode("A3"))
			.addNode("B", makeNode("B"))
			.addNode("C", makeNode("C"))
			.addEdge("A1", "B")
			.addEdge("A2", "B")
			.addEdge("A3", "B")
			.addEdge("B", "C")
			.addEdge(START, "A1")
			.addEdge(START, "A2")
			.addEdge(START, "A3")
			.addEdge("C", END);

		var result = workflow.compile().getGraph(GraphRepresentation.Type.PLANTUML, "testWithParallelBranchOnStart");

		assertEquals("""
				@startuml testWithParallelBranchOnStart
				skinparam usecaseFontSize 14
				skinparam usecaseStereotypeFontSize 12
				skinparam hexagonFontSize 14
				skinparam hexagonStereotypeFontSize 12
				title "testWithParallelBranchOnStart"
				footer

				powered by spring-ai-alibaba
				end footer
				circle start<<input>> as __START__
				circle stop as __END__
				usecase "A1"<<Node>>
				usecase "A2"<<Node>>
				usecase "A3"<<Node>>
				usecase "B"<<Node>>
				usecase "C"<<Node>>
				"__START__" -down-> "A1"
				"__START__" -down-> "A2"
				"__START__" -down-> "A3"
				"A1" -down-> "B"
				"A2" -down-> "B"
				"A3" -down-> "B"
				"B" -down-> "C"
				"C" -down-> "__END__"
				@enduml
				""", result.content());

		result = workflow.getGraph(GraphRepresentation.Type.MERMAID, "testWithParallelBranchOnStart", false);
		System.out.println(result.content());
		assertEquals("""
				---
				title: testWithParallelBranchOnStart
				---
				flowchart TD
					__START__((start))
					__END__((stop))
					A1("A1")
					A2("A2")
					A3("A3")
					B("B")
					C("C")
					__START__:::__START__ --> A1:::A1
					__START__:::__START__ --> A2:::A2
					__START__:::__START__ --> A3:::A3
					A1:::A1 --> B:::B
					A2:::A2 --> B:::B
					A3:::A3 --> B:::B
					B:::B --> C:::C
					C:::C --> __END__:::__END__

					classDef ___START__ fill:black,stroke-width:1px,font-size:xx-small;
					classDef ___END__ fill:black,stroke-width:1px,font-size:xx-small;
				""", result.content());
	}

}
