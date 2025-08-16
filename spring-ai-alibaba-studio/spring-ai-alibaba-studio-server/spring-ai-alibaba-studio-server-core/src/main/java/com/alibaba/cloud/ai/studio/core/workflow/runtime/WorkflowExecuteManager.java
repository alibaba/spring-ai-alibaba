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
package com.alibaba.cloud.ai.studio.core.workflow.runtime;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.InvokeSourceEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.debug.TaskRunResponse;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.app.ApplicationVersion;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.context.RequestContextHolder;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.RequestContextThreadPoolWrapper;
import com.alibaba.cloud.ai.studio.core.utils.concurrent.ThreadPoolUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.processor.ExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.ClassifierExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.EndExecuteProcessor;
import com.alibaba.cloud.ai.studio.core.workflow.processor.impl.JudgeExecuteProcessor;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.util.SupplierUtil;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.APPCODE_CONVERSATION_ID_TEMPLATE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.SYS_HISTORY_LIST_KEY;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.WORKFLOW_TASK_FINISH_FLAG;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * WorkflowExecuteManager is responsible for managing the execution of workflow graphs. It
 * handles the construction and validation of workflow graphs, execution scheduling of
 * nodes, monitoring of workflow execution status, building of debug configurations, and
 * checking of workflow configurations. The manager ensures proper execution order,
 * handles node dependencies, and maintains execution state.
 *
 * Key features: - Workflow graph construction and validation - Node execution scheduling
 * and monitoring - Execution state management - Debug configuration building - Workflow
 * configuration validation
 *
 * @since 1.0.0.3
 */
@Slf4j
@Component
public class WorkflowExecuteManager {

	private final Map<String, AbstractExecuteProcessor> processorMap;

	private final WorkflowInnerService workflowInnerService;

	private final ChatMemory conversationChatMemory;

	private final CommonConfig commonConfig;

	public WorkflowExecuteManager(@Lazy Map<String, AbstractExecuteProcessor> processorMap,
			WorkflowInnerService workflowInnerService, ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		this.processorMap = processorMap;
		this.workflowInnerService = workflowInnerService;
		this.conversationChatMemory = conversationChatMemory;
		this.commonConfig = commonConfig;
	}

	public Boolean stopTask(String taskId) {
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		WorkflowContext context = workflowInnerService.getContextCache(requestContext.getWorkspaceId(), taskId);
		if (context == null) {
			return false;
		}

		context.setTaskStatus(NodeStatusEnum.STOP.getCode());
		context.setError(ErrorCode.WORKFLOW_RUN_CANCEL.toError("Manually terminated"));

		workflowInnerService.refreshContextCache(context);
		return true;
	}

	public TaskRunResponse runTask(ApplicationVersion appVersion, List<TaskRunParam> inputParams, String conversationId,
			WorkflowContext workflowContext) {
		RequestContext context = RequestContextHolder.getRequestContext();
		if (appVersion == null) {
			LogUtils.monitor("WorkflowExecuteManager", "runTask", System.currentTimeMillis(), FAIL, inputParams,
					"request or appId is null");
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("request or appId is null"));
		}
		// Initialize context
		inputParams.forEach(input -> {
			String key = input.getKey();
			String source = input.getSource();
			if ("sys".equals(source)) {
				workflowContext.getSysMap()
					.put(key, VariableUtils.convertValueByType(input.getKey(), input.getType(), input.getValue()));
			}
			else {
				workflowContext.getUserMap()
					.put(key, VariableUtils.convertValueByType(input.getKey(), input.getType(), input.getValue()));
			}
		});

		conversationId = conversationId == null ? IdGenerator.uuid() : conversationId;

		workflowContext.setAppId(appVersion.getAppId());
		workflowContext.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
		workflowContext.setRequestId(context.getRequestId());
		workflowContext.setWorkspaceId(context.getWorkspaceId());
		workflowContext.setConversationId(conversationId);
		String taskId = execute(appVersion, workflowContext);
		TaskRunResponse response = new TaskRunResponse();
		response.setTaskId(taskId);
		response.setConversationId(conversationId);
		response.setRequestId(context.getRequestId());
		return response;
	}

	/**
	 * Executes a workflow based on the provided application version and context
	 * @param applicationVersion The version of the application containing workflow
	 * configuration
	 * @param context The execution context for the workflow
	 * @return The task ID for tracking the execution
	 * @throws BizException if application version is null
	 */
	public String execute(ApplicationVersion applicationVersion, WorkflowContext context) {
		if (applicationVersion == null) {
			throw new BizException(ErrorCode.APP_NOT_FOUND.toError());
		}
		String config = applicationVersion.getConfig();
		return execute(config, context);
	}

	/**
	 * Executes a workflow using the provided configuration string and context
	 * @param config JSON string containing workflow configuration
	 * @param context The execution context for the workflow
	 * @return The task ID for tracking the execution
	 */
	public String execute(String config, WorkflowContext context) {
		WorkflowConfig appOrchestraConfig = JsonUtils.fromJson(config, WorkflowConfig.class);
		context.setWorkflowConfig(appOrchestraConfig);
		return execute(context);
	}

	/**
	 * Executes a workflow using the provided workflow configuration and context
	 * @param workflowConfig The workflow configuration object
	 * @param context The execution context for the workflow
	 * @return The task ID for tracking the execution
	 */
	public String execute(WorkflowConfig workflowConfig, WorkflowContext context) {
		context.setWorkflowConfig(workflowConfig);
		return execute(context);
	}

	/**
	 * Core execution method that processes the workflow Initializes execution context,
	 * sets up history tracking, and submits execution task
	 * @param context The execution context containing workflow configuration and state
	 * @return The task ID for tracking the execution
	 * @throws BizException if workflow configuration is missing
	 */
	public String execute(WorkflowContext context) {
		if (context.getWorkflowConfig() == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("workflowConfig"));
		}
		String taskId = IdGenerator.uuid();
		context.setTaskId(taskId);

		// Initialize historical context. If user provides sys.history_list, preset
		// context is invalidated; otherwise use cached context
		boolean historySwitch = false;
		Integer historyMaxRound = 5;
		WorkflowConfig.GlobalConfig globalConfig = context.getWorkflowConfig().getGlobalConfig();
		if (globalConfig != null && globalConfig.getHistoryConfig() != null
				&& BooleanUtils.isTrue(globalConfig.getHistoryConfig().getHistorySwitch())) {
			historySwitch = true;
			historyMaxRound = globalConfig.getHistoryConfig().getHistoryMaxRound() == null ? 5
					: globalConfig.getHistoryConfig().getHistoryMaxRound();
		}
		if (historySwitch) {
			String conversationId = String.format(APPCODE_CONVERSATION_ID_TEMPLATE, context.getAppId(),
					context.getConversationId());
			// FIXME, use 'historyMaxRound' to find the top K messages only
			List<Message> messages = conversationChatMemory.get(conversationId);

			List<Message> historyList = (List<Message>) context.getSysMap().get(SYS_HISTORY_LIST_KEY);
			if (CollectionUtils.isEmpty(historyList)) {
				context.getSysMap().put(SYS_HISTORY_LIST_KEY, messages);
			}
		}

		workflowInnerService.refreshContextCache(context);
		ThreadPoolUtils.taskExecutorService.submit(() -> {
			try {
				syncExecute(context.getWorkflowConfig(), context);
			}
			catch (Exception e) {
				log.error("execute error:{}", context.getWorkflowConfig(), e);
			}
		});
		return taskId;
	}

	/**
	 * Synchronously executes the workflow Manages the execution queue, monitors node
	 * execution, and handles task scheduling
	 * @param appOrchestraConfig The workflow configuration
	 * @param context The execution context
	 * @throws InterruptedException if execution is interrupted
	 */
	public void syncExecute(WorkflowConfig appOrchestraConfig, WorkflowContext context) throws InterruptedException {
		context.setStartTime(System.currentTimeMillis());
		context.setWorkflowConfig(appOrchestraConfig);
		DirectedAcyclicGraph<String, Edge> graph = constructGraph(appOrchestraConfig);
		final BlockingQueue<String> taskQueue = new LinkedBlockingQueue<>();
		BlockingQueue<String> nodeMonitorQueue = new LinkedBlockingQueue<>();
		final HashSet<String> taskSet = new HashSet<>();
		ThreadPoolUtils.taskExecutorService.submit(() -> {
			try {
				executeMonitorThread(graph, taskQueue, taskSet, context, nodeMonitorQueue);
			}
			catch (Exception e) {
				log.error("task Schedule execute error:{}", e.getMessage());
			}
		});

		AtomicBoolean needStop = new AtomicBoolean(needStop(graph, context));
		while (!needStop.get()) {
			String nodeId = taskQueue.poll(10, TimeUnit.SECONDS);
			if (nodeId != null && !WORKFLOW_TASK_FINISH_FLAG.equals(nodeId)) {
				ThreadPoolExecutor executor = ((RequestContextThreadPoolWrapper) ThreadPoolUtils.nodeExecutorService)
					.getThreadPoolExecutor();
				if (executor != null) {
					int currentThreads = executor.getActiveCount();
					long taskCount = executor.getTaskCount();
					long completedTaskCount = executor.getCompletedTaskCount();
					log.info("ThreadId:{} taskId:{} activeCount:{} taskCount:{} completedTaskCount:{} ",
							Thread.currentThread().getId(), context.getTaskId(), currentThreads, taskCount,
							completedTaskCount);
				}
				ThreadPoolUtils.nodeExecutorService.submit(() -> {
					try {
						// Execute node work
						executeNodeWork(graph, nodeId, context);
						nodeMonitorQueue.add("nodeExecuteSuccess");
					}
					catch (Exception e) {
						Thread.currentThread().interrupt();
					}
				});
			}
			else if (WORKFLOW_TASK_FINISH_FLAG.equals(nodeId)) {
				break;
			}
			needStop.set(needStop(graph, context));
		}
		// Maintain cache final consistency
		if (context.getTaskStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
			LogUtils.monitor("WorkflowService", "runTask", context.getStartTime(), SUCCESS, appOrchestraConfig,
					context);
		}
		else {
			LogUtils.monitor("WorkflowService", "runTask", context.getStartTime(), FAIL, appOrchestraConfig, context);
		}

		workflowInnerService.refreshContextCache(context);
	}

	/**
	 * Monitors the execution of workflow nodes Continuously checks for executable nodes
	 * and updates the task queue
	 * @param graph The workflow graph
	 * @param taskQueue Queue for pending tasks
	 * @param taskSet Set of tasks that have been queued
	 * @param context The execution context
	 * @param nodeMonitorQueue Queue for node execution status updates
	 */
	private void executeMonitorThread(DirectedAcyclicGraph<String, Edge> graph, BlockingQueue<String> taskQueue,
			HashSet<String> taskSet, WorkflowContext context, BlockingQueue<String> nodeMonitorQueue) {
		AtomicBoolean done = new AtomicBoolean(false);

		long lastRefreshTime = System.currentTimeMillis();

		// 根据invokeSource设置不同的刷新频率
		Integer refreshInterval = getRefreshIntervalByInvokeSource(context.getInvokeSource());

		while (!done.get()) {
			// Find executable nodes
			for (String node : graph.vertexSet()) {
				boolean b = workflowInnerService.canExecute(graph, node, context);
				if (b && !taskSet.contains(node)) {
					log.info("nodeAnalysis canExecute:{}  taskID:{}  result:{}", node, context.getTaskId(), b);
					taskQueue.add(node);
					taskSet.add(node);
				}
			}
			done.set(needStop(graph, context));
			if (!done.get()) {
				try {
					nodeMonitorQueue.poll(100, TimeUnit.MILLISECONDS);

					// 定时刷新context缓存
					long currentTime = System.currentTimeMillis();
					if (refreshInterval != null && currentTime - lastRefreshTime >= refreshInterval) {
						workflowInnerService.refreshContextCache(context);
						lastRefreshTime = currentTime;
					}
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}
		taskQueue.add(WORKFLOW_TASK_FINISH_FLAG);
	}

	/**
	 * 根据invokeSource获取刷新频率
	 * @param invokeSource 调用来源
	 * @return 刷新间隔（毫秒）
	 */
	private Integer getRefreshIntervalByInvokeSource(String invokeSource) {
		Map<Object, Object> workflowRefreshInterval = JsonUtils
			.fromJsonToMap(commonConfig.getWorkflowRefreshInterval());
		Object o = workflowRefreshInterval.get(invokeSource);
		if (o == null) {
			return null;
		}
		return (Integer) o;
	}

	/**
	 * Constructs a directed acyclic graph from workflow configuration
	 * @param appOrchestraConfig The workflow configuration
	 * @return A directed acyclic graph representing the workflow
	 */
	private DirectedAcyclicGraph<String, Edge> constructGraph(WorkflowConfig appOrchestraConfig) {
		// Check if the current graph is a single path, multiple paths are not allowed
		DirectedAcyclicGraph<String, Edge> graph = new DirectedAcyclicGraph<>(null,
				SupplierUtil.createSupplier(Edge.class), false, true);
		appOrchestraConfig.getNodes().forEach(node -> graph.addVertex(node.getId()));
		appOrchestraConfig.getEdges().forEach(edge -> graph.addEdge(edge.getSource(), edge.getTarget(), edge));
		return graph;
	}

	/**
	 * Constructs and validates a workflow graph from nodes and edges Ensures the graph is
	 * a valid DAG with proper connectivity
	 * @param nodes List of workflow nodes
	 * @param edges List of workflow edges
	 * @return A validated directed acyclic graph
	 * @throws BizException if the graph is invalid
	 */
	public DirectedAcyclicGraph<String, Edge> constructGraph(List<Node> nodes, List<Edge> edges) {
		// Create a temporary WorkflowConfig to reuse the validation logic
		WorkflowConfig tempConfig = new WorkflowConfig();
		tempConfig.setNodes(nodes);
		tempConfig.setEdges(edges);
		DirectedAcyclicGraph<String, Edge> graph = constructGraph(tempConfig);

		// Check connectivity
		Set<String> sourceNodes = graph.vertexSet()
			.stream()
			.filter(nodeId -> graph.incomingEdgesOf(nodeId).isEmpty())
			.collect(Collectors.toSet());

		Set<String> sinkNodes = graph.vertexSet()
			.stream()
			.filter(nodeId -> graph.outgoingEdgesOf(nodeId).isEmpty())
			.collect(Collectors.toSet());

		if (sourceNodes.isEmpty()) {
			throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID
				.toError("Workflow must contain at least one start node with in-degree 0"));
		}
		if (sinkNodes.isEmpty()) {
			throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID
				.toError("Workflow must contain at least one end node with out-degree 0"));
		}

		// Check if all nodes are reachable from source nodes
		Set<String> reachableFromSource = new HashSet<>();
		for (String sourceNode : sourceNodes) {
			reachableFromSource.addAll(findReachableNodes(graph, sourceNode));
		}
		if (!reachableFromSource.equals(graph.vertexSet())) {
			throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID
				.toError("There are nodes that cannot be reached from start nodes"));
		}

		// Check if all nodes can reach sink nodes
		Set<String> canReachSink = new HashSet<>();
		for (String sinkNode : sinkNodes) {
			canReachSink.addAll(findReachableNodes(graph, sinkNode, true));
		}
		if (!canReachSink.equals(graph.vertexSet())) {
			throw new BizException(
					ErrorCode.WORKFLOW_CONFIG_INVALID.toError("There are nodes that cannot reach end nodes"));
		}

		return graph;
	}

	/**
	 * Finds all nodes reachable from a given start node
	 * @param graph The workflow graph
	 * @param startNode The starting node
	 * @param reverse Whether to search in reverse direction
	 * @return Set of reachable nodes
	 */
	private static Set<String> findReachableNodes(DirectedAcyclicGraph<String, Edge> graph, String startNode,
			boolean reverse) {
		Set<String> visited = new HashSet<>();
		Set<String> toVisit = new HashSet<>();
		toVisit.add(startNode);

		while (!toVisit.isEmpty()) {
			String current = toVisit.iterator().next();
			toVisit.remove(current);
			visited.add(current);

			Set<Edge> edges = reverse ? graph.incomingEdgesOf(current) : graph.outgoingEdgesOf(current);
			for (Edge edge : edges) {
				String next = reverse ? edge.getSource() : edge.getTarget();
				if (!visited.contains(next)) {
					toVisit.add(next);
				}
			}
		}

		return visited;
	}

	private static Set<String> findReachableNodes(DirectedAcyclicGraph<String, Edge> graph, String startNode) {
		return findReachableNodes(graph, startNode, false);
	}

	/**
	 * Checks if the workflow execution should stop Considers execution status, end node
	 * state, and timeout conditions
	 * @param graph The workflow graph
	 * @param context The execution context
	 * @return true if execution should stop, false otherwise
	 */
	private boolean needStop(DirectedAcyclicGraph<String, Edge> graph, WorkflowContext context) {
		// Manually terminated
		boolean stopFlag = context.getTaskStatus().equals(NodeStatusEnum.STOP.getCode());
		if (stopFlag) {
			return true;
		}
		boolean containFail = context.getTaskStatus().equals(NodeStatusEnum.FAIL.getCode());
		Optional<String> endNodeOptional = graph.vertexSet()
			.stream()
			.filter(node -> node.startsWith("End_") || node.startsWith("IteratorEnd_")
					|| node.startsWith("ParallelEnd_"))
			.findFirst();
		String endNode = null;
		if (endNodeOptional.isPresent()) {
			endNode = endNodeOptional.get();
		}
		// End execution when there are no executable nodes and no nodes currently
		// executing
		NodeResult endNodeResult = context.getNodeResultMap().get(endNode);
		// Calculate execution time in seconds
		long seconds = (System.currentTimeMillis() - context.getStartTime()) / 1000;
		if (seconds > InvokeSourceEnum.valueOf(context.getInvokeSource()).getTimeoutSeconds()) {
			context.setTaskStatus(NodeStatusEnum.FAIL.getCode());
			context.setErrorInfo("timeout");
			workflowInnerService.refreshContextCache(context);
			return true;
		}
		return containFail
				|| (endNodeResult != null && endNodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode()));
	}

	/**
	 * Executes the work for a specific node Handles node state management, execution, and
	 * error handling
	 * @param graph The workflow graph
	 * @param nodeId The ID of the node to execute
	 * @param context The execution context
	 */
	private void executeNodeWork(DirectedAcyclicGraph<String, Edge> graph, String nodeId, WorkflowContext context) {
		try {
			// Lock before execution to prevent race conditions and multiple executions of
			// the same node
			context.getLock().lock();
			NodeResult nodeResult = new NodeResult();
			try {
				// Ensure execution uniqueness
				if (context.getNodeResultMap().containsKey(nodeId)) {
					return;
				}
				nodeResult.setNodeId(nodeId);
				Optional<Node> any = context.getWorkflowConfig()
					.getNodes()
					.stream()
					.filter(node -> node.getId().equals(nodeId))
					.findFirst();
				nodeResult.setNodeType(any.get().getType());
				// Set status to executing
				nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
				context.getNodeResultMap().put(nodeId, nodeResult);
				workflowInnerService.refreshContextCache(context);
			}
			finally {
				context.getLock().unlock();
			}

			log.info("ThreadId:{} requestId:{} taskId:{} executeNodeWork monitor nodeId :{} NodeResultMap:{}",
					Thread.currentThread().getId(), context.getRequestId(), context.getTaskId(), nodeId,
					context.getNodeResultMap());

			// Get all predecessor nodes
			Set<Edge> incomingEdges = graph.incomingEdgesOf(nodeId);
			// If all predecessor nodes are skip, then current node is also skip
			if (!incomingEdges.isEmpty()) {
				boolean skipResult = incomingEdges.stream().allMatch(incomingEdge -> {
					NodeResult result = context.getNodeResultMap().get(incomingEdge.getSource());
					// Predecessor node success, but predecessor node is multi-branch
					// node, then judge multi-branch result in multiBranchResults, not
					// exist, then skip result
					if (result.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode()) && result.isMultiBranch()) {
						List<NodeResult.MultiBranchReference> multiBranchResults = result.getMultiBranchResults();
						if (CollectionUtils.isEmpty(multiBranchResults)) {
							return true;
						}
						else {
							Optional<NodeResult.MultiBranchReference> referenceOptional = multiBranchResults.stream()
								.filter(branchResult -> branchResult.getTargetIds().contains(nodeId))
								.findFirst();
							if (!referenceOptional.isPresent()) {
								return true;
							}
						}
						return false;
					}
					return result.getNodeStatus().equals(NodeStatusEnum.SKIP.getCode());
				});
				if (skipResult) {
					nodeResult.setNodeStatus(NodeStatusEnum.SKIP.getCode());
					nodeResult.setNodeId(nodeId);
					context.getNodeResultMap().put(nodeId, nodeResult);
					context.getExecuteOrderList().add(nodeId);
					workflowInnerService.refreshContextCache(context);
					return;
				}
			}
			// Execute node configuration
			Optional<Node> nodeOptional = context.getWorkflowConfig()
				.getNodes()
				.stream()
				.filter(node -> node.getId().equals(nodeId))
				.findFirst();
			Node node = nodeOptional.get();
			String type = capitalizeFirstLetter(node.getType());
			context.getExecuteOrderList().add(node.getId());
			node.setType(type);
			processorMap.get(type + "ExecuteProcessor").execute(graph, node, context);
		}
		catch (Exception e) {
			log.error("executeNodeWork error:{}", nodeId, e);
			NodeResult nodeResult = new NodeResult();
			nodeResult.setNodeId(nodeId);
			Optional<Node> any = context.getWorkflowConfig()
				.getNodes()
				.stream()
				.filter(node -> node.getId().equals(nodeId))
				.findFirst();
			nodeResult.setNodeType(any.get().getType());
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorInfo(e.getMessage());
			nodeResult.setNodeExecTime((System.currentTimeMillis() - context.getStartTime()) + "ms");
			context.getNodeResultMap().put(nodeId, nodeResult);
			context.setTaskStatus(NodeStatusEnum.FAIL.getCode());
			workflowInnerService.refreshContextCache(context);
			throw new BizException(ErrorCode.WORKFLOW_EXECUTE_ERROR.toError("error node id is: " + nodeId));
		}
	}

	/**
	 * Capitalizes the first letter of a string
	 * @param str The input string
	 * @return The string with first letter capitalized
	 */
	private String capitalizeFirstLetter(String str) {
		if (str == null || str.length() == 0) {
			return str;
		}
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	/**
	 * Constructs a debug configuration for a workflow fragment Adds necessary start and
	 * end nodes for debugging
	 * @param nodes List of workflow nodes
	 * @param edges List of workflow edges
	 * @return A complete workflow configuration for debugging
	 */
	public WorkflowConfig constructDebugConfig4Fragment(List<Node> nodes, List<Edge> edges) {
		Map<String, Node> nodeMap = nodes.stream().collect(Collectors.toMap(Node::getId, Function.identity()));
		DirectedAcyclicGraph<String, Edge> graph = constructGraph(nodes, edges);
		// Find all nodes with in-degree 0
		Set<String> sourceNodes = graph.vertexSet()
			.stream()
			.filter(nodeId -> graph.incomingEdgesOf(nodeId).isEmpty())
			.collect(Collectors.toSet());

		// Add Start type node, connect it to nodes with in-degree 0, and create
		// corresponding edges
		String startNodeId = "Start_1";
		Node startNode = new Node();
		startNode.setId(startNodeId);
		startNode.setType(NodeTypeEnum.START.getCode());
		startNode.setName("Start Node");
		Node.NodeCustomConfig startNodeCustomConfig = new Node.NodeCustomConfig();
		startNodeCustomConfig.setInputParams(Lists.newArrayList());
		startNodeCustomConfig.setOutputParams(Lists.newArrayList());
		startNode.setConfig(startNodeCustomConfig);
		nodes.add(startNode);

		// Create edges from Start node to each node with in-degree 0
		for (String sourceNode : sourceNodes) {
			Edge edge = new Edge();
			edge.setSource(startNodeId);
			edge.setSourceHandle(startNodeId);
			edge.setTarget(sourceNode);
			edge.setTargetHandle(sourceNode);
			edges.add(edge);
		}

		// Find all nodes with out-degree 0
		Set<String> sinkNodes = graph.vertexSet()
			.stream()
			.filter(nodeId -> graph.outgoingEdgesOf(nodeId).isEmpty())
			.collect(Collectors.toSet());

		// Add End type node, connect nodes with out-degree 0 to it, and create
		// corresponding edges
		String endNodeId = "End_1";
		Node endNode = new Node();
		endNode.setId(endNodeId);
		endNode.setType(NodeTypeEnum.END.getCode());
		endNode.setName("End Node");
		Node.NodeCustomConfig endCustomConfig = new Node.NodeCustomConfig();
		EndExecuteProcessor.NodeParam endNodeParam = new EndExecuteProcessor.NodeParam();
		endNodeParam.setOutputType("text");
		endNodeParam.setTextTemplate("Flow debug successfully");
		endCustomConfig.setNodeParam(JsonUtils.fromObjectToMap(endNodeParam));
		endCustomConfig.setOutputParams(Lists.newArrayList());
		endCustomConfig.setInputParams(Lists.newArrayList());
		endNode.setConfig(endCustomConfig);
		nodes.add(endNode);

		// Create edges from each node with out-degree 0 to the End node
		for (String sinkNode : sinkNodes) {
			Node node = nodeMap.get(sinkNode);
			if (node.getType().equals(NodeTypeEnum.CLASSIFIER.getCode())) {
				// classifier
				ClassifierExecuteProcessor.NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(),
						ClassifierExecuteProcessor.NodeParam.class);
				if (CollectionUtils.isNotEmpty(nodeParam.getConditions())) {
					nodeParam.getConditions().stream().forEach(condition -> {
						Edge edge = new Edge();
						edge.setSource(sinkNode);
						edge.setSourceHandle(sinkNode + "_" + condition.getId());
						edge.setTarget(endNodeId);
						edge.setTargetHandle(endNodeId);
						edges.add(edge);
					});
				}
			}
			else if (node.getType().equals(NodeTypeEnum.JUDGE.getCode())) {
				// judge
				JudgeExecuteProcessor.NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(),
						JudgeExecuteProcessor.NodeParam.class);
				List<JudgeExecuteProcessor.Branch> branches = nodeParam.getBranches();
				if (CollectionUtils.isNotEmpty(branches)) {
					branches.stream().forEach(branch -> {
						Edge edge = new Edge();
						edge.setSource(sinkNode);
						edge.setSourceHandle(sinkNode + "_" + branch.getId());
						edge.setTarget(endNodeId);
						edge.setTargetHandle(endNodeId);
						edges.add(edge);
					});
				}
			}
			else {
				Edge edge = new Edge();
				edge.setSource(sinkNode);
				edge.setSourceHandle(sinkNode);
				edge.setTarget(endNodeId);
				edge.setTargetHandle(endNodeId);
				edges.add(edge);
			}
		}

		// Generate a WorkflowConfig based on the final completed nodes and edges
		WorkflowConfig workflowConfig = new WorkflowConfig();
		workflowConfig.setNodes(nodes);
		workflowConfig.setEdges(edges);
		return workflowConfig;
	}

	/**
	 * Constructs debug variables from input map Converts flat key-value pairs into nested
	 * structure
	 * @param inputMap Map of input variables
	 * @return Nested map structure of variables
	 */
	public Map<String, Object> constructDebugVariables(Map<String, Object> inputMap) {
		// Keys in inputMap are expressions like sys.query, LLM_xxx.result,
		// API_ddd.result.a.b.c, etc., and Objects are their values
		// Generate a nested Map structure based on expression hierarchy
		Map<String, Object> result = new HashMap<>();

		for (Map.Entry<String, Object> entry : inputMap.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();

			// Split expression by dot
			String[] parts = key.split("\\.");
			Map<String, Object> currentMap = result;

			// Process all parts except the last one
			for (int i = 0; i < parts.length - 1; i++) {
				String part = parts[i];
				if (!currentMap.containsKey(part)) {
					currentMap.put(part, new HashMap<String, Object>());
				}
				currentMap = (Map<String, Object>) currentMap.get(part);
			}

			// Process the last part
			String lastPart = parts[parts.length - 1];
			currentMap.put(lastPart, value);
		}
		return result;
	}

	/**
	 * Validates the workflow configuration Checks node parameters and overall flow
	 * validity
	 * @param appOrchestraConfig The workflow configuration to check
	 * @return Result of the configuration check
	 */
	public ExecuteProcessor.CheckFlowParamResult checkWorkflowConfig(WorkflowConfig appOrchestraConfig) {
		ExecuteProcessor.CheckFlowParamResult result = new ExecuteProcessor.CheckFlowParamResult();
		result.setSuccess(true);
		DirectedAcyclicGraph<String, Edge> graph = constructGraph(appOrchestraConfig);
		List<Node> nodes = appOrchestraConfig.getNodes();
		nodes.stream().forEach(node -> {
			String type = node.getType();
			ExecuteProcessor.CheckNodeParamResult checkNodeParamResult = processorMap.get(type + "ExecuteProcessor")
				.checkNodeParam(graph, node);
			if (!checkNodeParamResult.isSuccess()) {
				result.setSuccess(false);
				result.getCheckNodeParamResults().add(checkNodeParamResult);
			}
		});
		return result;
	}

}
