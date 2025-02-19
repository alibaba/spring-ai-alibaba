package com.alibaba.cloud.ai.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.constant.SaverConstant;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import lombok.NonNull;
import com.alibaba.cloud.ai.graph.action.AsyncEdgeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.action.AsyncNodeActionWithConfig;
import com.alibaba.cloud.ai.graph.internal.edge.Edge;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeCondition;
import com.alibaba.cloud.ai.graph.internal.edge.EdgeValue;
import com.alibaba.cloud.ai.graph.internal.node.Node;
import com.alibaba.cloud.ai.graph.internal.node.SubCompiledGraphNode;
import com.alibaba.cloud.ai.graph.internal.node.SubStateGraphNode;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.ObjectStreamStateSerializer;
import com.alibaba.cloud.ai.graph.state.AgentState;
import com.alibaba.cloud.ai.graph.state.AgentStateFactory;
import com.alibaba.cloud.ai.graph.state.Channel;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;

/**
 * Represents a state graph with nodes and edges.
 *
 * @param <State> the type of the state associated with the graph
 */
public class StateGraph<State extends AgentState> {

	/**
	 * Enum representing various error messages related to graph state.
	 */
	public enum Errors {

		invalidNodeIdentifier("END is not a valid node id!"),
		invalidEdgeIdentifier("END is not a valid edge sourceId!"),
		duplicateNodeError("node with id: %s already exist!"), duplicateEdgeError("edge with id: %s already exist!"),
		duplicateConditionalEdgeError("conditional edge from '%s' already exist!"),
		edgeMappingIsEmpty("edge mapping is empty!"), missingEntryPoint("missing Entry Point"),
		entryPointNotExist("entryPoint: %s doesn't exist!"), finishPointNotExist("finishPoint: %s doesn't exist!"),
		missingNodeReferencedByEdge("edge sourceId '%s' refers to undefined node!"),
		missingNodeInEdgeMapping("edge mapping for sourceId: %s contains a not existent nodeId %s!"),
		invalidEdgeTarget("edge sourceId: %s has an initialized target value!"),
		duplicateEdgeTargetError("edge [%s] has duplicate targets %s!"),
		unsupportedConditionalEdgeOnParallelNode(
				"parallel node doesn't support conditional branch, but on [%s] a conditional branch on %s have been found!"),
		illegalMultipleTargetsOnParallelNode("parallel node [%s] must have only one target, but %s have been found!"),
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

		missingNodeInEdgeMapping("cannot find edge mapping for id: '%s' in conditional edge with sourceId: '%s' "),
		missingNode("node with id: '%s' doesn't exist!"), missingEdge("edge with sourceId: '%s' doesn't exist!"),
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

	public static String END = "__END__";

	public static String START = "__START__";

	final Nodes<State> nodes = new Nodes<>();

	final Edges<State> edges = new Edges<>();

	private EdgeValue<State> entryPoint;

	@Deprecated(forRemoval = true)
	private String finishPoint;

	private final Map<String, Channel<?>> channels;

	private final StateSerializer<State> stateSerializer;

	/**
	 * @param channels the state's schema of the graph
	 * @param stateSerializer the serializer to serialize the state
	 */
	public StateGraph(Map<String, Channel<?>> channels, StateSerializer<State> stateSerializer) {
		this.channels = channels;
		this.stateSerializer = stateSerializer;
	}

	/**
	 * Constructs a new StateGraph with the specified serializer.
	 * @param stateSerializer the serializer to serialize the state
	 */
	public StateGraph(@NonNull StateSerializer<State> stateSerializer) {
		this(Map.of(), stateSerializer);

	}

	/**
	 * Constructs a new StateGraph with the specified state factory.
	 * @param stateFactory the factory to create agent states
	 */
	public StateGraph(AgentStateFactory<State> stateFactory) {
		this(Map.of(), stateFactory);

	}

	/**
	 * @param channels the state's schema of the graph
	 * @param stateFactory the factory to create agent states
	 */
	public StateGraph(Map<String, Channel<?>> channels, AgentStateFactory<State> stateFactory) {
		this(channels, new ObjectStreamStateSerializer<>(stateFactory));
	}

	public StateSerializer<State> getStateSerializer() {
		return stateSerializer;
	}

	public final AgentStateFactory<State> getStateFactory() {
		return stateSerializer.stateFactory();
	}

	public Map<String, Channel<?>> getChannels() {
		return unmodifiableMap(channels);
	}

	@Deprecated(forRemoval = true)
	public EdgeValue<State> getEntryPoint() {
		return edges.edgeBySourceId(START).map(Edge::target).orElse(null);
	}

	@Deprecated(forRemoval = true)
	public String getFinishPoint() {
		return finishPoint;
	}

	/**
	 * Sets the entry point of the graph.
	 * @param entryPoint the nodeId of the graph's entry-point
	 * @deprecated use addEdge(START, nodeId)
	 */
	@Deprecated(forRemoval = true)
	public void setEntryPoint(String entryPoint) {
		try {
			addEdge(START, entryPoint);
		}
		catch (GraphStateException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets a conditional entry point of the graph.
	 * @param condition the edge condition
	 * @param mappings the edge mappings
	 * @throws GraphStateException if the edge mappings is null or empty
	 * @deprecated use addConditionalEdge(START, consition, mappings)
	 */
	@Deprecated(forRemoval = true)
	public void setConditionalEntryPoint(AsyncEdgeAction<State> condition, Map<String, String> mappings)
			throws GraphStateException {
		addConditionalEdges(START, condition, mappings);
	}

	/**
	 * Sets the identifier of the node that represents the end of the graph execution.
	 * @param finishPoint the identifier of the finish point node
	 * @deprecated use use addEdge(nodeId, END)
	 */
	@Deprecated
	public void setFinishPoint(String finishPoint) {
		this.finishPoint = finishPoint;
	}

	/**
	 * /** Adds a node to the graph.
	 * @param id the identifier of the node
	 * @param action the action to be performed by the node
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph<State> addNode(String id, AsyncNodeAction<State> action) throws GraphStateException {
		return addNode(id, AsyncNodeActionWithConfig.of(action));
	}

	/**
	 * @param id the identifier of the node
	 * @param actionWithConfig the action to be performed by the node
	 * @return this
	 * @throws GraphStateException if the node identifier is invalid or the node already
	 * exists
	 */
	public StateGraph<State> addNode(String id, AsyncNodeActionWithConfig<State> actionWithConfig)
			throws GraphStateException {
		if (Objects.equals(id, END)) {
			throw Errors.invalidNodeIdentifier.exception(END);
		}
		Node<State> node = new Node<>(id, (config) -> actionWithConfig);

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
	public StateGraph<State> addSubgraph(String id, CompiledGraph<State> subGraph) throws GraphStateException {
		if (Objects.equals(id, END)) {
			throw Errors.invalidNodeIdentifier.exception(END);
		}

		var node = new SubCompiledGraphNode<>(id, subGraph);

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
	public StateGraph<State> addSubgraph(String id, StateGraph<State> subGraph) throws GraphStateException {
		if (Objects.equals(id, END)) {
			throw Errors.invalidNodeIdentifier.exception(END);
		}

		subGraph.validateGraph();

		var node = new SubStateGraphNode<>(id, subGraph);

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
	 * @throws GraphStateException if the edge identifier is invalid or the edge already
	 * exists
	 */
	public StateGraph<State> addEdge(String sourceId, String targetId) throws GraphStateException {
		if (Objects.equals(sourceId, END)) {
			throw Errors.invalidEdgeIdentifier.exception(END);
		}

		// if (Objects.equals(sourceId, START)) {
		// this.entryPoint = new EdgeValue<>(targetId);
		// return this;
		// }

		var newEdge = new Edge<>(sourceId, new EdgeValue<State>(targetId));

		int index = edges.elements.indexOf(newEdge);
		if (index >= 0) {
			var newTargets = new ArrayList<>(edges.elements.get(index).targets());
			newTargets.add(newEdge.target());
			edges.elements.set(index, new Edge<>(sourceId, newTargets));
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
	 * @throws GraphStateException if the edge identifier is invalid, the mappings are
	 * empty, or the edge already exists
	 */
	public StateGraph<State> addConditionalEdges(String sourceId, AsyncEdgeAction<State> condition,
			Map<String, String> mappings) throws GraphStateException {
		if (Objects.equals(sourceId, END)) {
			throw Errors.invalidEdgeIdentifier.exception(END);
		}
		if (mappings == null || mappings.isEmpty()) {
			throw Errors.edgeMappingIsEmpty.exception(sourceId);
		}

		// if (Objects.equals(sourceId, START)) {
		// this.entryPoint = new EdgeValue<>(new EdgeCondition<>(condition, mappings));
		// return this;
		// }

		var newEdge = new Edge<>(sourceId, new EdgeValue<>(new EdgeCondition<>(condition, mappings)));

		if (edges.elements.contains(newEdge)) {
			throw Errors.duplicateConditionalEdgeError.exception(sourceId);
		}
		else {
			edges.elements.add(newEdge);
		}
		return this;
	}

	void validateGraph() throws GraphStateException {
		var edgeStart = edges.edgeBySourceId(START).orElseThrow(Errors.missingEntryPoint::exception);

		edgeStart.validate(nodes);

		for (Edge<State> edge : edges.elements) {
			edge.validate(nodes);
		}

	}

	/**
	 * Compiles the state graph into a compiled graph.
	 * @param config the compile configuration
	 * @return a compiled graph
	 * @throws GraphStateException if there are errors related to the graph state
	 */
	public CompiledGraph<State> compile(CompileConfig config) throws GraphStateException {
		Objects.requireNonNull(config, "config cannot be null");

		validateGraph();

		return new CompiledGraph<>(this, config);
	}

	/**
	 * Compiles the state graph into a compiled graph.
	 * @return a compiled graph
	 * @throws GraphStateException if there are errors related to the graph state
	 */
	public CompiledGraph<State> compile() throws GraphStateException {
		SaverConfig saverConfig = SaverConfig.builder().register(SaverConstant.MEMORY, new MemorySaver()).build();
		return compile(CompileConfig.builder().saverConfig(saverConfig).build());
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

	public static class Nodes<State extends AgentState> {

		public final Set<Node<State>> elements;

		public Nodes(Collection<Node<State>> elements) {
			this.elements = new LinkedHashSet<>(elements);
		}

		public Nodes() {
			this.elements = new LinkedHashSet<>();
		}

		public boolean anyMatchById(String id) {
			return elements.stream().anyMatch(n -> Objects.equals(n.id(), id));
		}

		public List<SubStateGraphNode<State>> onlySubStateGraphNodes() {
			return elements.stream()
				.filter(n -> n instanceof SubStateGraphNode<State>)
				.map(n -> (SubStateGraphNode<State>) n)
				.toList();
		}

		public List<Node<State>> exceptSubStateGraphNodes() {
			return elements.stream().filter(n -> !(n instanceof SubStateGraphNode<State>)).toList();
		}

	}

	public static class Edges<State extends AgentState> {

		public final List<Edge<State>> elements;

		public Edges(Collection<Edge<State>> elements) {
			this.elements = new LinkedList<>(elements);
		}

		public Edges() {
			this.elements = new LinkedList<>();
		}

		public Optional<Edge<State>> edgeBySourceId(String sourceId) {
			return elements.stream().filter(e -> Objects.equals(e.sourceId(), sourceId)).findFirst();
		}

		public List<Edge<State>> edgesByTargetId(String targetId) {
			return elements.stream().filter(e -> e.anyMatchByTargetId(targetId)).toList();
		}

	}

}
