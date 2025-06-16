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

import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.Errors;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeCondition;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;
import com.alibaba.cloud.ai.graph.internal.node.SubStateGraphNode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.PlainTextStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.JacksonStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Represents a state graph with nodes and edges.
 */
public class StateGraph {

	/**
	 * Constant representing the END of the graph.
	 */
	public static final String END = "__END__";

	/**
	 * Constant representing the START of the graph.
	 */
	public static final String START = "__START__";

	/**
	 * Constant representing the ERROR of the graph.
	 */
	public static final String ERROR = "__ERROR__";

	/**
	 * Collection of nodes in the graph.
	 */
	final Nodes nodes = new Nodes();

	/**
	 * Collection of edges in the graph.
	 */
	final Edges edges = new Edges();

	/**
	 * Factory for creating overall state instances.
	 */
	private OverAllStateFactory overAllStateFactory;

	/**
	 * Factory for providing key strategies.
	 */
	private KeyStrategyFactory keyStrategyFactory;

	/**
	 * Name of the graph.
	 */
	private String name;

	/**
	 * Serializer for the state.
	 */
	private final PlainTextStateSerializer stateSerializer;

	/**
	 * Jackson-based serializer for state.
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
	 * Constructs a StateGraph with the specified name, key strategy factory, and state
	 * serializer.
	 * @param name the name of the graph
	 * @param keyStrategyFactory the factory for providing key strategies
	 * @param stateSerializer the plain text state serializer to use
	 */
	public StateGraph(String name, KeyStrategyFactory keyStrategyFactory, PlainTextStateSerializer stateSerializer) {
		this.name = name;
		this.keyStrategyFactory = keyStrategyFactory;
		this.stateSerializer = stateSerializer;
	}

	public StateGraph(KeyStrategyFactory keyStrategyFactory, PlainTextStateSerializer stateSerializer) {
		this.keyStrategyFactory = keyStrategyFactory;
		this.stateSerializer = stateSerializer;
	}

	/**
	 * Constructs a StateGraph with the given key strategy factory and name.
	 * @param keyStrategyFactory the factory for providing key strategies
	 * @param name the name of the graph
	 */
	public StateGraph(String name, KeyStrategyFactory keyStrategyFactory) {
		this.keyStrategyFactory = keyStrategyFactory;
		this.name = name;
		this.stateSerializer = new JacksonSerializer();
	}

	/**
	 * Constructs a StateGraph with the provided key strategy factory.
	 * @param keyStrategyFactory the factory for providing key strategies
	 */
	public StateGraph(KeyStrategyFactory keyStrategyFactory) {
		this.keyStrategyFactory = keyStrategyFactory;
		this.stateSerializer = new JacksonSerializer();
	}

	/**
	 * Deprecated constructor that initializes a StateGraph with the specified name,
	 * overall state factory, and state serializer.
	 * @param name the name of the graph
	 * @param overAllStateFactory the factory for creating overall state instances
	 * @param plainTextStateSerializer the plain text state serializer to use
	 */
	@Deprecated
	public StateGraph(String name, OverAllStateFactory overAllStateFactory,
			PlainTextStateSerializer plainTextStateSerializer) {
		this.name = name;
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = plainTextStateSerializer;
	}

	/**
	 * Deprecated constructor that initializes a StateGraph with the specified name and
	 * overall state factory.
	 * @param name the name of the graph
	 * @param overAllStateFactory the factory for creating overall state instances
	 */
	@Deprecated
	public StateGraph(String name, OverAllStateFactory overAllStateFactory) {
		this.name = name;
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = new JacksonSerializer();
	}

	/**
	 * Deprecated constructor that initializes a StateGraph with the provided overall
	 * state factory.
	 * @param overAllStateFactory the factory for creating overall state instances
	 */
	@Deprecated
	public StateGraph(OverAllStateFactory overAllStateFactory) {
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = new JacksonSerializer();
	}

	/**
	 * Deprecated constructor that initializes a StateGraph with the provided overall
	 * state factory and state serializer.
	 * @param overAllStateFactory the factory for creating overall state instances
	 * @param plainTextStateSerializer the plain text state serializer to use
	 */
	@Deprecated
	public StateGraph(OverAllStateFactory overAllStateFactory, PlainTextStateSerializer plainTextStateSerializer) {
		this.overAllStateFactory = overAllStateFactory;
		this.stateSerializer = plainTextStateSerializer;
	}

	/**
	 * Default constructor that initializes a StateGraph with a Gson-based state
	 * serializer.
	 */
	public StateGraph() {
		this.stateSerializer = new JacksonSerializer();
		this.keyStrategyFactory = HashMap::new;
	}

	/**
	 * Gets the name of the graph.
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the state serializer used by this graph.
	 * @return the state serializer
	 */
	public StateSerializer<OverAllState> getStateSerializer() {
		return stateSerializer;
	}

	/**
	 * Gets the state factory associated with this graph's state serializer.
	 * @return the state factory
	 */
	public final AgentStateFactory<OverAllState> getStateFactory() {
		return stateSerializer.stateFactory();
	}

	/**
	 * Gets the overall state factory.
	 * @return the overall state factory
	 */
	@Deprecated
	public final OverAllStateFactory getOverAllStateFactory() {
		return overAllStateFactory;
	}

	/**
	 * Gets the key strategy factory.
	 * @return the key strategy factory
	 */
	public final KeyStrategyFactory getKeyStrategyFactory() {
		return keyStrategyFactory;
	}

	/**
	 * Adds a node to the graph.
	 * @param id the identifier of the node
	 * @param action the asynchronous node action to be performed by the node
	 * @return this state graph instance
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, AsyncNodeAction action) throws GraphStateException {
		return addNode(id, AsyncNodeActionWithConfig.of(action));
	}

	/**
	 * Adds a node to the graph with the specified action and configuration.
	 * @param id the identifier of the node
	 * @param actionWithConfig the action to be performed by the node
	 * @return this state graph instance
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph addNode(String id, AsyncNodeActionWithConfig actionWithConfig) throws GraphStateException {
		Node node = new Node(id, (config) -> actionWithConfig);
		return addNode(id, node);
	}

	/**
	 * Adds a node to the graph with the specified identifier and node instance.
	 * @param id the identifier of the node
	 * @param node the node to be added
	 * @return this state graph instance
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
	 * identifier. This implies that the subgraph shares the same state with the parent
	 * graph.
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
	 * identifier. This implies that the subgraph will share the same state with the
	 * parent graph and will be compiled when the parent is compiled.
	 * @param id the identifier of the node representing the subgraph
	 * @param subGraph the subgraph to be added; it will be compiled during compilation of
	 * the parent
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
	 * Adds an edge to the graph between the specified source and target nodes.
	 * @param sourceId the identifier of the source node
	 * @param targetId the identifier of the target node
	 * @return this state graph instance
	 * @throws GraphStateException if the edge identifier is invalid or the edge already
	 * exists
	 */
	public StateGraph addEdge(String sourceId, String targetId) throws GraphStateException {
		if (Objects.equals(sourceId, END)) {
			throw Errors.invalidEdgeIdentifier.exception(END);
		}

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
	 * Adds conditional edges to the graph based on the provided condition and mappings.
	 * @param sourceId the identifier of the source node
	 * @param condition the command action used to determine the target node
	 * @param mappings the mappings of conditions to target nodes
	 * @return this state graph instance
	 * @throws GraphStateException if the edge identifier is invalid, the mappings are
	 * empty, or the edge already exists
	 */
	public StateGraph addConditionalEdges(String sourceId, AsyncCommandAction condition, Map<String, String> mappings)
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
	 * Adds conditional edges to the graph based on the provided edge action condition and
	 * mappings.
	 * @param sourceId the identifier of the source node
	 * @param condition the edge action used to determine the target node
	 * @param mappings the mappings of conditions to target nodes
	 * @return this state graph instance
	 * @throws GraphStateException if the edge identifier is invalid, the mappings are
	 * empty, or the edge already exists
	 */
	public StateGraph addConditionalEdges(String sourceId, AsyncEdgeAction condition, Map<String, String> mappings)
			throws GraphStateException {
		return addConditionalEdges(sourceId, AsyncCommandAction.of(condition), mappings);
	}

	/**
	 * Validates the structure of the graph ensuring all connections are valid.
	 * @throws GraphStateException if there are errors related to the graph state
	 */
	void validateGraph() throws GraphStateException {
		var edgeStart = edges.edgeBySourceId(START).orElseThrow(Errors.missingEntryPoint::exception);

		edgeStart.validate(nodes);

		for (Edge edge : edges.elements) {
			edge.validate(nodes);
		}
	}

	/**
	 * Compiles the state graph into a compiled graph using the provided configuration.
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
	 * Compiles the state graph into a compiled graph using a default configuration with
	 * memory saver.
	 * @return a compiled graph
	 * @throws GraphStateException if there are errors related to the graph state
	 */
	public CompiledGraph compile() throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
		return compile(CompileConfig.builder().saverConfig(saverConfig).build());
	}

	/**
	 * Generates a drawable graph representation of the state graph.
	 * @param type the type of graph representation to generate
	 * @param title the title of the graph
	 * @param printConditionalEdges whether to include conditional edges in the output
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title, boolean printConditionalEdges) {
		String content = type.generator.generate(nodes, edges, title, printConditionalEdges);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Generates a drawable graph representation of the state graph with conditional edges
	 * included.
	 * @param type the type of graph representation to generate
	 * @param title the title of the graph
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type, String title) {
		String content = type.generator.generate(nodes, edges, title, true);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Generates a drawable graph representation of the state graph using the graph's name
	 * as title.
	 * @param type the type of graph representation to generate
	 * @return a diagram code of the state graph
	 */
	public GraphRepresentation getGraph(GraphRepresentation.Type type) {
		String content = type.generator.generate(nodes, edges, name, true);

		return new GraphRepresentation(type, content);
	}

	/**
	 * Container for nodes in the graph.
	 */
	public static class Nodes {

		/**
		 * The collection of nodes.
		 */
		public final Set<Node> elements;

		/**
		 * Instantiates a new Nodes container with the provided elements.
		 * @param elements the elements to initialize
		 */
		public Nodes(Collection<Node> elements) {
			this.elements = new LinkedHashSet<>(elements);
		}

		/**
		 * Instantiates a new empty Nodes container.
		 */
		public Nodes() {
			this.elements = new LinkedHashSet<>();
		}

		/**
		 * Checks if any node matches the given identifier.
		 * @param id the identifier to match
		 * @return true if a matching node is found, false otherwise
		 */
		public boolean anyMatchById(String id) {
			return elements.stream().anyMatch(n -> Objects.equals(n.id(), id));
		}

		/**
		 * Returns a list of sub-state graph nodes.
		 * @return a list of sub-state graph nodes
		 */
		public List<SubStateGraphNode> onlySubStateGraphNodes() {
			return elements.stream()
				.filter(n -> n instanceof SubStateGraphNode)
				.map(n -> (SubStateGraphNode) n)
				.toList();
		}

		/**
		 * Returns a list of nodes excluding sub-state graph nodes.
		 * @return a list of nodes excluding sub-state graph nodes
		 */
		public List<Node> exceptSubStateGraphNodes() {
			return elements.stream().filter(n -> !(n instanceof SubStateGraphNode)).toList();
		}

	}

	/**
	 * Container for edges in the graph.
	 */
	public static class Edges {

		/**
		 * The collection of edges.
		 */
		public final List<Edge> elements;

		/**
		 * Instantiates a new Edges container with the provided elements.
		 * @param elements the elements to initialize
		 */
		public Edges(Collection<Edge> elements) {
			this.elements = new LinkedList<>(elements);
		}

		/**
		 * Instantiates a new empty Edges container.
		 */
		public Edges() {
			this.elements = new LinkedList<>();
		}

		/**
		 * Retrieves the first edge matching the specified source identifier.
		 * @param sourceId the source identifier to match
		 * @return an optional containing the matched edge, or empty if none found
		 */
		public Optional<Edge> edgeBySourceId(String sourceId) {
			return elements.stream().filter(e -> Objects.equals(e.sourceId(), sourceId)).findFirst();
		}

		/**
		 * Retrieves a list of edges targeting the specified node identifier.
		 * @param targetId the target identifier to match
		 * @return a list of edges targeting the specified identifier
		 */
		public List<Edge> edgesByTargetId(String targetId) {
			return elements.stream().filter(e -> e.anyMatchByTargetId(targetId)).toList();
		}

	}

}
