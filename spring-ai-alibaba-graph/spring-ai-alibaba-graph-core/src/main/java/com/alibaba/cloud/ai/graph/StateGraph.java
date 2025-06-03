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

import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeCondition;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;
import com.alibaba.cloud.ai.graph.internal.node.SubStateGraphNode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.gson.GsonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.lang.String.format;

/**
 * Represents a state graph with nodes and edges.
 */
public class StateGraph {

	/**
	 * Enum representing various error messages related to graph state.
	 */
	public enum Errors {

		/**
		 * The Invalid node identifier.
		 */
		invalidNodeIdentifier("END is not a valid node id!"),
		/**
		 * The Invalid edge identifier.
		 */
		invalidEdgeIdentifier("END is not a valid edge sourceId!"),
		/**
		 * The Duplicate node error.
		 */
		duplicateNodeError("node with id: %s already exist!"),
		/**
		 * The Duplicate edge error.
		 */
		duplicateEdgeError("edge with id: %s already exist!"),
		/**
		 * The Duplicate conditional edge error.
		 */
		duplicateConditionalEdgeError("conditional edge from '%s' already exist!"),
		/**
		 * The Edge mapping is empty.
		 */
		edgeMappingIsEmpty("edge mapping is empty!"),
		/**
		 * The Missing entry point.
		 */
		missingEntryPoint("missing Entry Point"),
		/**
		 * The Entry point not exist.
		 */
		entryPointNotExist("entryPoint: %s doesn't exist!"),
		/**
		 * The Finish point not exist.
		 */
		finishPointNotExist("finishPoint: %s doesn't exist!"),
		/**
		 * The Missing node referenced by edge.
		 */
		missingNodeReferencedByEdge("edge sourceId '%s' refers to undefined node!"),
		/**
		 * The Missing node in edge mapping.
		 */
		missingNodeInEdgeMapping("edge mapping for sourceId: %s contains a not existent nodeId %s!"),
		/**
		 * The Invalid edge target.
		 */
		invalidEdgeTarget("edge sourceId: %s has an initialized target value!"),
		/**
		 * The Duplicate edge target error.
		 */
		duplicateEdgeTargetError("edge [%s] has duplicate targets %s!"),
		/**
		 * The Unsupported conditional edge on parallel node.
		 */
		unsupportedConditionalEdgeOnParallelNode(
				"parallel node doesn't support conditional branch, but on [%s] a conditional branch on %s have been found!"),
		/**
		 * The Illegal multiple targets on parallel node.
		 */
		illegalMultipleTargetsOnParallelNode("parallel node [%s] must have only one target, but %s have been found!"),
		/**
		 * The Interruption node not exist.
		 */
		interruptionNodeNotExist("node '%s' configured as interruption doesn't exist!");

		private final String errorMessage;

		Errors(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		/**
		 * Creates a new GraphStateException with the formatted error message.
		 * @param args the arguments to format the error message
		 * @return a new GraphStateException
		 */
		public GraphStateException exception(Object... args) {
			return new GraphStateException(format(errorMessage, (Object[]) args));
		}

	}

	/**
	 * Enum representing various error messages related to graph runner.
	 */
	enum RunnableErrors {

		/**
		 * The Missing node in edge mapping.
		 */
		missingNodeInEdgeMapping("cannot find edge mapping for id: '%s' in conditional edge with sourceId: '%s' "),
		/**
		 * The Missing node.
		 */
		missingNode("node with id: '%s' doesn't exist!"),
		/**
		 * The Missing edge.
		 */
		missingEdge("edge with sourceId: '%s' doesn't exist!"),
		/**
		 * Execution error runnable errors.
		 */
		executionError("%s");

		private final String errorMessage;

		RunnableErrors(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		/**
		 * Creates a new GraphRunnerException with the formatted error message.
		 * @param args the arguments to format the error message
		 * @return a new GraphRunnerException
		 */
		GraphRunnerException exception(String... args) {
			return new GraphRunnerException(format(errorMessage, (Object[]) args));
		}

	}

	/**
	 * The constant END.
	 */
	public static String END = "__END__";

	/**
	 * The constant START.
	 */
	public static String START = "__START__";

	/**
	 * The Nodes.
	 */
	final Nodes nodes = new Nodes();

	/**
	 * The Edges.
	 */
	final Edges edges = new Edges();

	private OverAllStateFactory overAllStateFactory;

	private String name;

	private final PlainTextStateSerializer stateSerializer;

	/**
	 * The type Jackson serializer.
	 */
	static class JacksonSerializer extends JacksonStateSerializer {

		/**
		 * Instantiates a new Jackson serializer.
		 */
		public JacksonSerializer() {
			super(OverAllState::new);
		}

		/**
		 * Gets object mapper.
		 * @return the object mapper
		 */
		ObjectMapper getObjectMapper() {
			return objectMapper;
		}

	}

	/**
	 * The type Gson serializer.
	 */
	static class GsonSerializer extends GsonStateSerializer {

		/**
		 * Instantiates a new Gson serializer.
		 */
		public GsonSerializer() {
			super(OverAllState::new,
					new GsonBuilder().enableComplexMapKeySerialization()
						.setLenient()
						.registerTypeAdapter(Double.TYPE,
								(JsonDeserializer<Double>) (json, typeOfT, context) -> json.getAsDouble())
						.serializeNulls()
						.create());
		}

		/**
		 * Gets gson.
		 * @return the gson
		 */
		Gson getGson() {
			return gson;
		}

	}

	/**
	 * The type Gson serializer 2.
	 */
	static class GsonSerializer2 extends GsonStateSerializer {

		/**
		 * Instantiates a new Gson serializer 2.
		 * @param stateFactory the state factory
		 */
		public GsonSerializer2(AgentStateFactory<OverAllState> stateFactory) {
			super(stateFactory,
					new GsonBuilder().enableComplexMapKeySerialization()
						.registerTypeAdapter(Double.TYPE,
								(JsonDeserializer<Double>) (json, typeOfT, context) -> json.getAsDouble())
						.setLenient()
						.serializeNulls()
						.create());
		}

		/**
		 * Gets gson.
		 * @return the gson
		 */
		Gson getGson() {
			return gson;
		}

	}

	/**
	 * Instantiates a new State graph.
	 * @param name the name
	 * @param overAllStateFactory the over all state factory
	 * @param plainTextStateSerializer the plain text state serializer
	 */
	public StateGraph(String name, OverAllStateFactory overAllStateFactory,
			PlainTextStateSerializer plainTextStateSerializer) {
		this.name = name;
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = plainTextStateSerializer;
	}

	/**
	 * Instantiates a new State graph.
	 * @param name the name
	 * @param overAllStateFactory the over all state factory
	 */
	public StateGraph(String name, OverAllStateFactory overAllStateFactory) {
		this.name = name;
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = new JacksonSerializer();
	}

	/**
	 * Instantiates a new State graph.
	 * @param overAllStateFactory the over all state factory
	 */
	public StateGraph(OverAllStateFactory overAllStateFactory) {
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = new JacksonSerializer();
	}

	/**
	 * Instantiates a new State graph.
	 * @param overAllStateFactory the over all state factory
	 * @param plainTextStateSerializer the plain text state serializer
	 */
	public StateGraph(OverAllStateFactory overAllStateFactory, PlainTextStateSerializer plainTextStateSerializer) {
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = plainTextStateSerializer;
	}

	/**
	 * Instantiates a new State graph.
	 */
	public StateGraph() {
		this.stateSerializer = new GsonSerializer();
	}

	/**
	 * Gets name.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets state serializer.
	 * @return the state serializer
	 */
	public StateSerializer<OverAllState> getStateSerializer() {
		return stateSerializer;
	}

	/**
	 * Gets state factory.
	 * @return the state factory
	 */
	public final AgentStateFactory<OverAllState> getStateFactory() {
		return stateSerializer.stateFactory();
	}

	/**
	 * Gets over all state factory.
	 * @return the over all state factory
	 */
	public final OverAllStateFactory getOverAllStateFactory() {
		return overAllStateFactory;
	}

	/**
	 * /** Adds a node to the graph.
	 * @param id the identifier of the node
	 * @param action the action to be performed by the node
	 * @return the state graph
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, AsyncNodeAction action) throws GraphStateException {
		return addNode(id, AsyncNodeActionWithConfig.of(action));
	}

	/**
	 * Add node state graph.
	 * @param id the identifier of the node
	 * @param actionWithConfig the action to be performed by the node
	 * @return this state graph
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, AsyncNodeActionWithConfig actionWithConfig) throws GraphStateException {
		Node node = new Node(id, (config) -> actionWithConfig);
		return addNode(id, node);
	}

	/**
	 * Add node state graph.
	 * @param id the identifier of the node
	 * @param node the node to be added
	 * @return this state graph
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, Node node) throws GraphStateException {
		if (Objects.equals(node.id(), END)) {
			throw Errors.invalidNodeIdentifier.exception(END);
		}
		if (!Objects.equals(node.id(), id)) {
			throw Errors.invalidNodeIdentifier.exception(node.id(), id);
		}

		if (nodes.elements.contains(node)) {
			throw Errors.duplicateNodeError.exception(id);
		}

		nodes.elements.add(node);
		return this;
	}

	/**
	 * Adds a subgraph to the state graph by creating a node with the specified
	 * identifier. This implies that Subgraph share the same state with parent graph
	 * @param id the identifier of the node representing the subgraph
	 * @param subGraph the compiled subgraph to be added
	 * @return this state graph instance
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, CompiledGraph subGraph) throws GraphStateException {
		if (Objects.equals(id, END)) {
			throw Errors.invalidNodeIdentifier.exception(END);
		}

		var node = new SubCompiledGraphNode(id, subGraph);

		if (nodes.elements.contains(node)) {
			throw Errors.duplicateNodeError.exception(id);
		}

		nodes.elements.add(node);
		return this;

	}

	/**
	 * Adds a subgraph to the state graph by creating a node with the specified
	 * identifier. This implies that Subgraph share the same state with parent graph
	 * @param id the identifier of the node representing the subgraph
	 * @param subGraph the subgraph to be added. it will be compiled on compilation of the
	 * parent
	 * @return this state graph instance
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, StateGraph subGraph) throws GraphStateException {
		if (Objects.equals(id, END)) {
			throw Errors.invalidNodeIdentifier.exception(END);
		}

		subGraph.validateGraph();

		var node = new SubStateGraphNode(id, subGraph);

		if (nodes.elements.contains(node)) {
			throw Errors.duplicateNodeError.exception(id);
		}

		nodes.elements.add(node);
		return this;
	}

	/**
	 * Adds an edge to the graph.
	 * @param sourceId the identifier of the source node
	 * @param targetId the identifier of the target node
	 * @return the state graph
	 * @throws GraphStateException if the edge identifier is invalid or the edge already
	 * exists
	 */
	public StateGraph addEdge(String sourceId, String targetId) throws GraphStateException {
		if (Objects.equals(sourceId, END)) {
			throw Errors.invalidEdgeIdentifier.exception(END);
		}

		// if (Objects.equals(sourceId, START)) {
		// this.entryPoint = new EdgeValue<>(targetId);
		// return this;
		// }

		var newEdge = new Edge(sourceId, new EdgeValue(targetId));

		int index = edges.elements.indexOf(newEdge);
		if (index >= 0) {
			var newTargets = new ArrayList<>(edges.elements.get(index).targets());
			newTargets.add(newEdge.target());
			edges.elements.set(index, new Edge(sourceId, newTargets));
		}
		else {
			edges.elements.add(newEdge);
		}

		return this;
	}

	/**
	 * Adds conditional edges to the graph.
	 * @param sourceId the identifier of the source node
	 * @param condition the condition to determine the target node
	 * @param mappings the mappings of conditions to target nodes
	 * @return the state graph
	 * @throws GraphStateException if the edge identifier is invalid, the mappings are
	 * empty, or the edge already exists
	 */
	public StateGraph addConditionalEdges(String sourceId, AsyncEdgeAction condition, Map<String, String> mappings)
			throws GraphStateException {
		if (Objects.equals(sourceId, END)) {
			throw Errors.invalidEdgeIdentifier.exception(END);
		}
		if (mappings == null || mappings.isEmpty()) {
			throw Errors.edgeMappingIsEmpty.exception(sourceId);
		}

		var newEdge = new Edge(sourceId, new EdgeValue(new EdgeCondition(condition, mappings)));

		if (edges.elements.contains(newEdge)) {
			throw Errors.duplicateConditionalEdgeError.exception(sourceId);
		}
		else {
			edges.elements.add(newEdge);
		}
		return this;
	}

	/**
	 * Validate graph.
	 * @throws GraphStateException the graph state exception
	 */
	void validateGraph() throws GraphStateException {
		var edgeStart = edges.edgeBySourceId(START).orElseThrow(Errors.missingEntryPoint::exception);

		edgeStart.validate(nodes);

		for (Edge edge : edges.elements) {
			edge.validate(nodes);
		}

	}

	/**
	 * Compiles the state graph into a compiled graph.
	 * @param config the compile configuration
	 * @return a compiled graph
	 * @throws GraphStateException if there are errors related to the graph state
	 */
	public CompiledGraph compile(CompileConfig config) throws GraphStateException {
		Objects.requireNonNull(config, "config cannot be null");

		validateGraph();

		return new CompiledGraph(this, config);
	}

	/**
	 * Compiles the state graph into a compiled graph.
	 * @return a compiled graph
	 * @throws GraphStateException if there are errors related to the graph state
	 */
	public CompiledGraph compile() throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
		return compile(CompileConfig.builder()
			.plainTextStateSerializer(new JacksonSerializer())
			.saverConfig(saverConfig)
			.build());
	}

	/**
	 * Generates a drawable graph representation of the state graph.
	 * @param type the type of graph representation to generate
	 * @param title the title of the graph
	 * @param printConditionalEdges whether to print conditional edges
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title, boolean printConditionalEdges) {

		String content = type.generator.generate(nodes, edges, title, printConditionalEdges);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Generates a drawable graph representation of the state graph.
	 * @param type the type of graph representation to generate
	 * @param title the title of the graph
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title) {

		String content = type.generator.generate(nodes, edges, title, true);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Gets graph.
	 * @param type the type
	 * @return the graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type) {

		String content = type.generator.generate(nodes, edges, name, true);

		return new GraphRepresentation(type, content);
	}

	/**
	 * The type Nodes.
	 */
	public static class Nodes {

		/**
		 * The Elements.
		 */
		public final Set<Node> elements;

		/**
		 * Instantiates a new Nodes.
		 * @param elements the elements
		 */
		public Nodes(Collection<Node> elements) {
			this.elements = new LinkedHashSet<>(elements);
		}

		/**
		 * Instantiates a new Nodes.
		 */
		public Nodes() {
			this.elements = new LinkedHashSet<>();
		}

		/**
		 * Any match by id boolean.
		 * @param id the id
		 * @return the boolean
		 */
		public boolean anyMatchById(String id) {
			return elements.stream().anyMatch(n -> Objects.equals(n.id(), id));
		}

		/**
		 * Only sub state graph nodes list.
		 * @return the list
		 */
		public List<SubStateGraphNode> onlySubStateGraphNodes() {
			return elements.stream()
				.filter(n -> n instanceof SubStateGraphNode)
				.map(n -> (SubStateGraphNode) n)
				.toList();
		}

		/**
		 * Except sub state graph nodes list.
		 * @return the list
		 */
		public List<Node> exceptSubStateGraphNodes() {
			return elements.stream().filter(n -> !(n instanceof SubStateGraphNode)).toList();
		}

	}

	/**
	 * The type Edges.
	 */
	public static class Edges {

		/**
		 * The Elements.
		 */
		public final List<Edge> elements;

		/**
		 * Instantiates a new Edges.
		 * @param elements the elements
		 */
		public Edges(Collection<Edge> elements) {
			this.elements = new LinkedList<>(elements);
		}

		/**
		 * Instantiates a new Edges.
		 */
		public Edges() {
			this.elements = new LinkedList<>();
		}

		/**
		 * Edge by source id optional.
		 * @param sourceId the source id
		 * @return the optional
		 */
		public Optional<Edge> edgeBySourceId(String sourceId) {
			return elements.stream().filter(e -> Objects.equals(e.sourceId(), sourceId)).findFirst();
		}

		/**
		 * Edges by target id list.
		 * @param targetId the target id
		 * @return the list
		 */
		public List<Edge> edgesByTargetId(String targetId) {
			return elements.stream().filter(e -> e.anyMatchByTargetId(targetId)).toList();
		}

	}

}
