/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.workflow.processor;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.CommonParam;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.ValueFromEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.RetryConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ShortTermMemory;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.TryCatchConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowConfig;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.APPCODE_CONVERSATION_ID_TEMPLATE;
import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_NODE_SELF_SHORT_MEMORY_TEMPLATE;
import static com.alibaba.cloud.ai.studio.core.base.constants.CacheConstants.WORKFLOW_SESSION_VARIABLE_KEY_TEMPLATE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.MEMORY_TYPE_CUSTOM;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.MEMORY_TYPE_SELF;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.NODE_BRANCH_DEFAULT;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.NODE_BRANCH_FAIL;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.NODE_CONFIG_RETRY;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.NODE_CONFIG_SHORT_MEMORY;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.NODE_CONFIG_TRY_CATCH;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.PARAM_TYPE_ARRAY_OBJECT_LOWER_CASE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.PARAM_TYPE_BOOLEAN_LOWER_CASE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.PARAM_TYPE_NUMBER_LOWER_CASE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.PARAM_TYPE_OBJECT_LOWER_CASE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.PARAM_TYPE_STRING_LOWER_CASE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.SYS_QUERY_KEY;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.TRY_CATCH_STRATEGY_DEFAULT_VALUE;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.TRY_CATCH_STRATEGY_FAIL_BRANCH;
import static com.alibaba.cloud.ai.studio.core.workflow.constants.WorkflowConstants.TRY_CATCH_STRATEGY_NOOP;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.FAIL;
import static com.alibaba.cloud.ai.studio.core.utils.LogUtils.SUCCESS;

/**
 * Abstract base class for workflow node execution processors.
 * <p>
 * This class provides core functionality for: 1. Node execution flow control (pre-check,
 * execution, retry, exception handling) 2. Variable processing (input/output parameter
 * mapping, template replacement) 3. Short-term memory management (session memory, node
 * memory) 4. Node result handling (status updates, result caching)
 * <p>
 * The processor follows a template method pattern where concrete implementations need to
 * provide specific execution logic through innerExecute().
 */
@Slf4j
@Component
public abstract class AbstractExecuteProcessor implements ExecuteProcessor {

	protected final RedisManager redisManager;

	protected final WorkflowInnerService workflowInnerService;

	protected final ChatMemory conversationChatMemory;

	protected final CommonConfig commonConfig;

	protected static final String INPUT_DECORATE_PARAM_KEY = "input";

	protected static final String OUTPUT_DECORATE_PARAM_KEY = "output";

	protected static final String REASONING_DECORATE_PARAM_KEY = "reasoning_content";

	public AbstractExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig) {
		this.redisManager = redisManager;
		this.workflowInnerService = workflowInnerService;
		this.conversationChatMemory = conversationChatMemory;
		this.commonConfig = commonConfig;
	}

	/**
	 * Main execution method for workflow nodes. Handles the complete execution lifecycle
	 * including pre-checks, execution, retry, exception handling, variable processing and
	 * result handling.
	 * @param graph The workflow graph
	 * @param node The node to execute
	 * @param context The workflow context
	 */
	@Override
	public void execute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		long start = System.currentTimeMillis();
		try {
			// Pre-check
			preCheck(graph, node, context);
			NodeResult nodeResult;
			try {
				nodeResult = innerExecute(graph, node, context);
			}
			catch (BizException e) {
				nodeResult = NodeResult.error(node, e.getError());
			}
			catch (Exception e) {
				nodeResult = NodeResult.error(node, e.getMessage());
			}
			// Handle retry on failure
			nodeResult = handleRetry(graph, node, context, nodeResult);
			// Handle exception handling
			nodeResult = handleTryCatch(graph, node, context, nodeResult);
			handleVariables(graph, node, context, nodeResult);
			handleNodeResult(graph, node, context, nodeResult, start);
			// Handle node's short-term memory at child node dimension
			handleSelfShortTermMemory(node, context, nodeResult);
		}
		catch (Exception e) {
			// Node execution error, save execution result and record error information
			NodeResult errorNodeResult = NodeResult.error(node, e.getMessage());
			errorNodeResult.setInput(JsonUtils.toJson(constructInputParamsMap(node, context)));
			handleNodeResult(graph, node, context, errorNodeResult, start);
		}
	}

	/**
	 * Pre-execution validation checks. Verifies: 1. Workflow is not stopped 2. Node has
	 * valid successors (unless it's an end node)
	 * @param graph The workflow graph
	 * @param node The node to validate
	 * @param context The workflow context
	 * @throws BizException if validation fails
	 */
	public void preCheck(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {
		// boolean b = workflowInnerService.checkValidFlag(context);
		if (context.getTaskStatus().equals(NodeStatusEnum.STOP.getCode())) {
			throw new BizException(ErrorCode.WORKFLOW_RUN_CANCEL.toError("Manually terminated"));
		}
		if ((!node.getId().startsWith("End_") && !node.getId().startsWith("IteratorEnd_")
				&& !node.getId().startsWith("ParallelEnd_"))
				&& CollectionUtils.isEmpty(graph.outgoingEdgesOf(node.getId()))) {
			throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID
				.toError("the current node has no successor node, and it cannot function properly."));
		}
	}

	/**
	 * Core execution logic to be implemented by concrete processors.
	 * @param graph The workflow graph
	 * @param node The node to execute
	 * @param context The workflow context
	 * @return NodeResult containing execution results
	 */
	public abstract NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node,
			WorkflowContext context);

	/**
	 * Processes node variables and updates context. Maps input/output parameters and
	 * updates variable cache.
	 * @param graph The workflow graph
	 * @param node The executed node
	 * @param context The workflow context
	 * @param nodeResult The execution result
	 */
	public void handleVariables(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())
				|| nodeResult.getNodeStatus().equals(NodeStatusEnum.EXECUTING.getCode())) {
			// Output node content does not need to record variables
			if (node.getType().equals(NodeTypeEnum.OUTPUT.getCode())) {
				return;
			}
			// Only process the current variable cache if successful
			String outputJsonString = nodeResult.getOutput();
			if (StringUtils.isBlank(outputJsonString)) {
				return;
			}
			Map<String, Object> map = JsonUtils.fromJsonToMap(outputJsonString);
			if (MapUtils.isNotEmpty(map)) {
				context.getVariablesMap().put(node.getId(), map);
			}
		}
	}

	/**
	 * Handles retry logic for failed node executions. Implements configurable retry with
	 * interval and max attempts.
	 * @param graph The workflow graph
	 * @param node The node to retry
	 * @param context The workflow context
	 * @param nodeResult The initial execution result
	 * @return Updated NodeResult after retry attempts
	 */
	public NodeResult handleRetry(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		try {
			if (!commonConfig.getRetrySupportNodeTypeSet().contains(node.getType())) {
				return nodeResult;
			}
			if (nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
				Map<String, Object> configMap = node.getConfig().getNodeParam();
				if (configMap != null && configMap.containsKey(NODE_CONFIG_RETRY)) {
					RetryConfig retryConfig = JsonUtils.fromJson(JsonUtils.toJson(configMap.get(NODE_CONFIG_RETRY)),
							RetryConfig.class);
					if (retryConfig != null && BooleanUtils.isTrue(retryConfig.getRetryEnabled())) {
						// interval default 1000ms
						int retryInterval = retryConfig.getRetryInterval() == null ? 1000
								: retryConfig.getRetryInterval();
						int maxRetries = retryConfig.getMaxRetries() == null ? 3 : retryConfig.getMaxRetries();
						for (int i = 0; i < maxRetries; i++) {
							try {
								Thread.sleep(retryInterval);
							}
							catch (InterruptedException e) {
								break;
							}
							NodeResult tmpNodeResult;
							try {
								tmpNodeResult = innerExecute(graph, node, context);
							}
							catch (Exception e) {
								tmpNodeResult = NodeResult.error(node, e.getMessage());
							}

							NodeResult.Retry retry = new NodeResult.Retry();
							retry.setHappened(true);
							retry.setRetryTimes(i + 1);
							tmpNodeResult.setRetry(retry);
							if (tmpNodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())
									&& (i + 1) == maxRetries) {
								return tmpNodeResult;
							}
							else if (!tmpNodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
								return tmpNodeResult;
							}
						}
					}
				}
			}
			return nodeResult;
		}
		catch (Exception e) {
			log.error("AbstractExecuteProcessor RequestId:{}", context.getRequestId(), e);
			return nodeResult;
		}
	}

	/**
	 * Handles exception handling logic for node executions. Supports different strategies
	 * like default value and fail branch.
	 * @param graph The workflow graph
	 * @param node The node with exception handling
	 * @param context The workflow context
	 * @param nodeResult The execution result
	 * @return Updated NodeResult after exception handling
	 */
	public NodeResult handleTryCatch(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult) {
		try {
			if (!commonConfig.getTryCatchSupportNodeTypeSet().contains(node.getType())) {
				return nodeResult;
			}
			String strategy = TRY_CATCH_STRATEGY_NOOP;
			boolean needTryCatch = false;
			TryCatchConfig tryCatchConfig = null;
			Map<String, Object> configMap = node.getConfig().getNodeParam();
			if (configMap != null && configMap.containsKey(NODE_CONFIG_TRY_CATCH)) {
				tryCatchConfig = JsonUtils.fromJson(JsonUtils.toJson(configMap.get(NODE_CONFIG_TRY_CATCH)),
						TryCatchConfig.class);
				if (tryCatchConfig != null) {
					strategy = tryCatchConfig.getStrategy() == null ? TRY_CATCH_STRATEGY_NOOP
							: tryCatchConfig.getStrategy();
					needTryCatch = !strategy.equals(TRY_CATCH_STRATEGY_NOOP);
				}
			}
			if (nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
				if (needTryCatch) {
					if (TRY_CATCH_STRATEGY_DEFAULT_VALUE.equals(strategy)) {
						nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
						nodeResult.setError(null);
						NodeResult.TryCatch tryCatch = new NodeResult.TryCatch();
						tryCatch.setHappened(true);
						tryCatch.setStrategy(TRY_CATCH_STRATEGY_DEFAULT_VALUE);
						nodeResult.setTryCatch(tryCatch);
						nodeResult.setOutput(
								JsonUtils.toJson(construct4DefaultValueStrategy(tryCatchConfig.getDefaultValues())));
					}
					else if (TRY_CATCH_STRATEGY_FAIL_BRANCH.equals(strategy)) {
						nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
						nodeResult.setErrorInfo(null);
						nodeResult.setError(null);
						NodeResult.TryCatch tryCatch = new NodeResult.TryCatch();
						tryCatch.setHappened(true);
						tryCatch.setStrategy(TRY_CATCH_STRATEGY_FAIL_BRANCH);
						nodeResult.setTryCatch(tryCatch);
						nodeResult.setMultiBranch(true);
						NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
						branchReference.setConditionId(NODE_BRANCH_FAIL);
						Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
						List<String> failTargetIds = edges.stream().filter(edge -> {
							if (edge.getSourceHandle().equals(node.getId() + "_" + NODE_BRANCH_FAIL)) {
								return true;
							}
							return false;
						}).map(Edge::getTarget).collect(Collectors.toList());
						branchReference.setTargetIds(failTargetIds);
						nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));
					}
				}
			}
			else {
				if (needTryCatch && strategy.equals(TRY_CATCH_STRATEGY_FAIL_BRANCH)) {
					nodeResult.setMultiBranch(true);
					NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
					branchReference.setConditionId(NODE_BRANCH_DEFAULT);
					Set<Edge> edges = graph.outgoingEdgesOf(node.getId());
					List<String> successTargetIds = edges.stream()
						.filter(edge -> edge.getSourceHandle().equals(node.getId()))
						.map(Edge::getTarget)
						.collect(Collectors.toList());
					branchReference.setTargetIds(successTargetIds);
					nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));
				}
			}
			context.setTaskStatus(NodeStatusEnum.EXECUTING.getCode());
			return nodeResult;
		}
		catch (Exception e) {
			log.error("AbstractExecuteProcessor RequestId:{}", context.getRequestId(), e);
			return nodeResult;
		}
	}

	private Map<String, Object> construct4DefaultValueStrategy(List<CommonParam> defaultValues) {
		Map<String, Object> resultMap = Maps.newHashMap();
		if (CollectionUtils.isEmpty(defaultValues)) {
			return resultMap;
		}
		// Default configuration only supports top-level configuration, not nested
		// structures
		defaultValues.stream().forEach(defaultValue -> {
			String key = defaultValue.getKey();
			String type = defaultValue.getType();
			Object value = defaultValue.getValue();
			try {
				resultMap.put(key, VariableUtils.convertValueByType(key, type, value));
			}
			catch (Exception e) {
				resultMap.put(key, null);
			}
		});
		return resultMap;
	}

	private void handleSelfShortTermMemory(Node node, WorkflowContext context, NodeResult nodeResult) {
		if (!nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
			return;
		}
		Map<String, Object> nodeParamMap = node.getConfig().getNodeParam();
		if (nodeParamMap == null) {
			return;
		}
		ShortTermMemory shortMemory = JsonUtils.fromJson(JsonUtils.toJson(nodeParamMap.get(NODE_CONFIG_SHORT_MEMORY)),
				ShortTermMemory.class);
		if (shortMemory == null || BooleanUtils.isNotTrue(shortMemory.getEnabled())) {
			return;
		}
		String type = shortMemory.getType() == null ? MEMORY_TYPE_CUSTOM : shortMemory.getType();
		if (MEMORY_TYPE_SELF.equals(type)) {
			Integer round = shortMemory.getRound() == null ? 5 : shortMemory.getRound();
			handleCurrentRound4SelfShortTermMemory(node, round, context, nodeResult);
		}
	}

	public void handleCurrentRound4SelfShortTermMemory(Node node, Integer round, WorkflowContext context,
			NodeResult nodeResult) {

	}

	/**
	 * Processes and updates node execution results. Updates node status, execution time,
	 * and handles end node completion.
	 * @param graph The workflow graph
	 * @param node The executed node
	 * @param context The workflow context
	 * @param nodeResult The execution result
	 * @param startTime Execution start timestamp
	 */
	public void handleNodeResult(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context,
			NodeResult nodeResult, long startTime) {
		if (nodeResult == null) {
			return;
		}

		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
			// If any task fails, set the task status to failed
			context.setTaskStatus(NodeStatusEnum.FAIL.getCode());
			HashMap<String, Object> errorMap = new HashMap();
			errorMap.put("errorInfo", nodeResult.getErrorInfo());
			errorMap.put("nodeId", node.getId());
			errorMap.put("nodeName", node.getName());
			context.setErrorInfo(JsonUtils.toJson(errorMap));
			context.setError(nodeResult.getError());
		}
		nodeResult.setNodeName(node.getName() == null ? node.getId() : node.getName());
		nodeResult.setNodeExecTime((System.currentTimeMillis() - startTime) + "ms");

		// Don't set the node success status within the node to avoid premature task
		// status changes that could cause subsequent nodes to start early and miss
		// variable values
		if (!nodeResult.getNodeStatus().equals(NodeStatusEnum.FAIL.getCode())) {
			nodeResult.setNodeStatus(NodeStatusEnum.SUCCESS.getCode());
		}
		// Set node execution result
		context.getNodeResultMap().put(node.getId(), nodeResult);
		if (nodeResult.getNodeType().equals(NodeTypeEnum.END.getCode())
				&& nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
			// End node and execution successful, set task status to success
			// And set the final task result to the output of the end node
			context.setTaskResult(nodeResult.getOutput());
			// Handle global historical context
			handleSessionShortTermMemory(context, nodeResult);
			// Handle all nodes
			handleNodeSelfShortTermMemory(context);
			// Handle session variable results
			handleSessionVariables(context);
			// End node and execution successful, set task status to success
			context.setTaskStatus(NodeStatusEnum.SUCCESS.getCode());
		}
		if (nodeResult.getNodeStatus().equals(NodeStatusEnum.SUCCESS.getCode())) {
			LogUtils.monitor("WorkFlowProcessor", nodeResult.getNodeType(), startTime, SUCCESS, nodeResult.getInput(),
					nodeResult.getOutput());
		}
		else {
			LogUtils.monitor("WorkFlowProcessor", nodeResult.getNodeType(), startTime, FAIL, nodeResult.getInput(),
					nodeResult.getOutput(), nodeResult.getErrorInfo());
		}

		workflowInnerService.refreshContextCache(context);
	}

	/**
	 * Processes session variables and persists to cache. Variables are stored with 1 hour
	 * expiration.
	 * @param context The workflow context
	 */
	private void handleSessionVariables(WorkflowContext context) {
		Map<String, Object> sessionMap = (Map<String, Object>) context.getVariablesMap().get("session");
		if (MapUtils.isEmpty(sessionMap)) {
			return;
		}
		sessionMap.entrySet().stream().forEach(entry -> {
			// Session persists for one hour
			redisManager.put(String.format(WORKFLOW_SESSION_VARIABLE_KEY_TEMPLATE, context.getAppId(),
					context.getConversationId(), entry.getKey()), entry.getValue(), Duration.ofHours(1));
		});
	}

	/**
	 * Manages session-level short-term memory. Maintains conversation history for the
	 * workflow conversation.
	 * @param context The workflow context
	 * @param nodeResult The final node result
	 */
	private void handleSessionShortTermMemory(WorkflowContext context, NodeResult nodeResult) {
		// Get global context switch
		boolean historySwitch = false;
		WorkflowConfig.GlobalConfig globalConfig = context.getWorkflowConfig().getGlobalConfig();
		if (globalConfig != null && globalConfig.getHistoryConfig() != null
				&& BooleanUtils.isTrue(globalConfig.getHistoryConfig().getHistorySwitch())) {
			historySwitch = true;
		}
		if (historySwitch) {
			String conversationId = String.format(APPCODE_CONVERSATION_ID_TEMPLATE, context.getAppId(),
					context.getConversationId());
			String inputContent = context.getSysMap().get(SYS_QUERY_KEY) == null ? ""
					: (String) context.getSysMap().get(SYS_QUERY_KEY);
			String outputContent = nodeResult.getOutput() == null ? "" : nodeResult.getOutput();
			conversationChatMemory.add(conversationId, buildCurrentRoundMessages(inputContent, outputContent));
		}
	}

	/**
	 * Manages node-level short-term memory. Maintains conversation history for individual
	 * nodes.
	 * @param context The workflow context
	 */
	private void handleNodeSelfShortTermMemory(WorkflowContext context) {
		if (context.getConversationId() == null) {
			return;
		}
		ConcurrentHashMap<String, NodeResult> nodeResultMap = context.getNodeResultMap();
		nodeResultMap.entrySet().stream().forEach(entry -> {
			if (entry == null || entry.getValue() == null || entry.getValue().getShortMemory() == null) {
				return;
			}
			NodeResult.ShortMemory shortMemory = entry.getValue().getShortMemory();
			List<ChatMessage> currentSelfChatMessages = shortMemory.getCurrentSelfChatMessages();
			if (CollectionUtils.isEmpty(currentSelfChatMessages)) {
				return;
			}
			String cacheKey = String.format(WORKFLOW_NODE_SELF_SHORT_MEMORY_TEMPLATE, context.getAppId(),
					context.getConversationId(), entry.getValue().getNodeId());
			List<ChatMessage> historyCache = redisManager.get(cacheKey);
			Deque<ChatMessage> historyMessages;
			if (Objects.isNull(historyCache)) {
				historyMessages = new ArrayDeque<>();
			}
			else {
				historyMessages = new ArrayDeque<>(historyCache);
			}

			for (ChatMessage message : currentSelfChatMessages) {
				historyMessages.offer(message);
				if (historyMessages.size() > shortMemory.getRound() * 2) {
					historyMessages.poll();
				}
			}
			redisManager.put(cacheKey, Lists.newArrayList(historyMessages));
		});
	}

	private List<Message> buildCurrentRoundMessages(String queryText, String answerText) {
		return Lists.newArrayList(new UserMessage(queryText), new AssistantMessage(answerText));
	}

	/**
	 * Constructs input parameter map from node configuration and context.
	 * @param node The node with input parameters
	 * @param context The workflow context
	 * @return Map of input parameter values
	 */
	public Map<String, Object> constructInputParamsMap(Node node, WorkflowContext context) {
		Map<String, Object> map = Maps.newHashMap();
		List<Node.InputParam> inputParams = node.getConfig().getInputParams();
		if (CollectionUtils.isEmpty(inputParams)) {
			return map;
		}
		inputParams.stream().forEach(inputParam -> {
			String valueFrom = inputParam.getValueFrom();
			Object value = inputParam.getValue();
			if (valueFrom.equals(ValueFromEnum.refer.name())) {
				if (value == null) {
					return;
				}
				String expression = VariableUtils.getExpressionFromBracket((String) value);
				if (expression == null) {
					return;
				}
				Object finalValue = VariableUtils.getValueFromContext(inputParam, context);
				if (finalValue == null) {
					return;
				}
				map.put(inputParam.getKey(), finalValue);
			}
			else {
				if (value == null) {
					return;
				}
				map.put(inputParam.getKey(), value);
			}
		});
		return map;
	}

	/**
	 * Replaces template variables in text with actual values from context.
	 * @param originalTemplate The template text
	 * @param context The workflow context
	 * @return Processed text with replaced variables
	 */
	public String replaceTemplateContent(String originalTemplate, WorkflowContext context) {
		String promptContent = originalTemplate;
		Set<String> keys = VariableUtils.identifyVariableSetFromText(promptContent);
		for (String key : keys) {
			Object o = VariableUtils.getValueFromPayload(key, context.getVariablesMap());
			key = key.replaceAll("\\[", "\\\\[").replaceAll("\\]", "\\\\]");
			if (o == null) {
				// If the variable doesn't exist, replace with empty string
				promptContent = promptContent.replaceAll("\\$\\{" + key + "}", "");
			}
			else {
				String replaceContent;
				if (o instanceof Map || o instanceof List) {
					replaceContent = JsonUtils.toJson(o);
				}
				else {
					replaceContent = "" + o;
				}
				promptContent = promptContent.replaceAll("\\$\\{" + key + "}",
						Matcher.quoteReplacement(replaceContent));
			}
		}
		return promptContent;
	}

	/**
	 * Constructs output parameter map from execution result.
	 * @param node The node with output parameters
	 * @param resultObj The execution result object
	 * @param context The workflow context
	 * @return Map of output parameter values
	 */
	public static Map<String, Object> constructOutputParamsMap(Node node, Object resultObj, WorkflowContext context) {
		try {
			Map<String, Object> resultMap = Maps.newHashMap();
			if (resultObj == null || CollectionUtils.isEmpty(node.getConfig().getOutputParams())) {
				return resultMap;
			}
			if (resultObj instanceof Map) {
				Map<String, Object> finalResultObj = new HashMap<>();
				constructOutputs((Map<String, Object>) resultObj, finalResultObj, node.getConfig().getOutputParams());
				return finalResultObj;
			}
			else {
				node.getConfig().getOutputParams().stream().forEach(param -> {
					resultMap.put(param.getKey(), resultObj);
				});
				return resultMap;
			}
		}
		catch (Exception e) {
			throw new BizException(ErrorCode.WORKFLOW_CONFIG_INVALID
				.toError("Output format does not match actual structure, original return is:"
						+ JsonUtils.toJson(resultObj)));
		}
	}

	/**
	 * Decorates output with standard output parameter key.
	 * @param output The output value
	 * @return Decorated output map
	 */
	protected Map<String, Object> decorateOutput(Object output) {
		Map<String, Object> outputObj = new HashMap<>();
		outputObj.put(OUTPUT_DECORATE_PARAM_KEY, output);
		return outputObj;
	}

	/**
	 * Decorates input with standard input parameter key.
	 * @param input The input value
	 * @return Decorated input map
	 */
	protected Map<String, Object> decorateInput(Object input) {
		Map<String, Object> outputObj = new HashMap<>();
		outputObj.put(INPUT_DECORATE_PARAM_KEY, input);
		return outputObj;
	}

	/**
	 * Build output content
	 * @param sourceMap
	 * @param targetMap
	 * @param outputParamsRefs
	 */
	private static void constructOutputs(Map<String, Object> sourceMap, Map<String, Object> targetMap,
			List<Node.OutputParam> outputParamsRefs) {
		Set<String> typeSet = new HashSet<>();
		typeSet.add(PARAM_TYPE_STRING_LOWER_CASE);
		typeSet.add(PARAM_TYPE_NUMBER_LOWER_CASE);
		typeSet.add(PARAM_TYPE_BOOLEAN_LOWER_CASE);
		if (sourceMap == null) {
			return;
		}
		if (CollectionUtils.isEmpty(outputParamsRefs)) {
			return;
		}
		for (Node.OutputParam outputParamsRef : outputParamsRefs) {
			String type = outputParamsRef.getType();
			if (type == null) {
				continue;
			}
			type = type.toLowerCase();
			if (typeSet.contains(type)) {
				if (PARAM_TYPE_STRING_LOWER_CASE.equals(type)) {
					targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
				}
				else if (PARAM_TYPE_NUMBER_LOWER_CASE.equals(type)) {
					targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
				}
				else if (PARAM_TYPE_BOOLEAN_LOWER_CASE.equals(type)) {
					targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
				}
			}
			else if (PARAM_TYPE_ARRAY_OBJECT_LOWER_CASE.equals(type)) {
				// list
				List list;
				if (!targetMap.containsKey(outputParamsRef.getKey())) {
					list = new ArrayList();
					targetMap.put(outputParamsRef.getKey(), list);
				}
				else {
					list = (List) targetMap.get(outputParamsRef.getKey());
				}
				if (!CollectionUtils.isEmpty(outputParamsRef.getProperties())) {
					constructArray((List) sourceMap.get(outputParamsRef.getKey()), list,
							outputParamsRef.getProperties());
				}
				else {
					list.addAll((List) sourceMap.get(outputParamsRef.getKey()));
				}
			}
			else if (type.startsWith("array")) {
				targetMap.put(outputParamsRef.getKey(), sourceMap.get(outputParamsRef.getKey()));
			}
			else if (PARAM_TYPE_OBJECT_LOWER_CASE.equals(type)) {
				Map<String, Object> jsonObject;
				if (!targetMap.containsKey(outputParamsRef.getKey())) {
					jsonObject = new HashMap<>();
					targetMap.put(outputParamsRef.getKey(), jsonObject);
				}
				else {
					jsonObject = (Map<String, Object>) targetMap.get(outputParamsRef.getKey());
				}
				if (!CollectionUtils.isEmpty(outputParamsRef.getProperties())) {
					constructOutputs((Map<String, Object>) sourceMap.get(outputParamsRef.getKey()), jsonObject,
							outputParamsRef.getProperties());
				}
				else {
					// 如果object没有继续的属性，则不解析，直接将所有内容直接放入target对象中
					jsonObject.putAll((Map<String, Object>) sourceMap.get(outputParamsRef.getKey()));
				}
			}
		}
	}

	private static void constructArray(List sourceList, List targetList, List<Node.OutputParam> outputParamsRefs) {
		if (sourceList == null || outputParamsRefs == null) {
			return;
		}
		for (Node.OutputParam outputParamsRef : outputParamsRefs) {
			for (int i = 0; i < sourceList.size(); i++) {
				Object o = sourceList.get(i);
				Set<String> typeSet = new HashSet<>();
				typeSet.add(PARAM_TYPE_STRING_LOWER_CASE);
				typeSet.add(PARAM_TYPE_NUMBER_LOWER_CASE);
				typeSet.add(PARAM_TYPE_BOOLEAN_LOWER_CASE);
				String type = outputParamsRef.getType();
				if (type == null) {
					continue;
				}
				type = type.toLowerCase();
				if (typeSet.contains(type)) {
					if (o instanceof Map) {
						Map<String, Object> oObj = (Map) o;
						if (targetList.size() > i) {
							((Map) targetList.get(i)).put(outputParamsRef.getKey(), oObj.get(outputParamsRef.getKey()));
						}
						else {
							Map<String, Object> newObj = new HashMap<>();
							newObj.put(outputParamsRef.getKey(), oObj.get(outputParamsRef.getKey()));
							targetList.add(newObj);
						}
					}
					else {
						targetList.add(o);
					}
				}
				else if (PARAM_TYPE_OBJECT_LOWER_CASE.equals(type)) {
					Map<String, Object> targetObj = new HashMap<>();
					constructOutputs(((Map<String, Object>) ((Map<String, Object>) o).get(outputParamsRef.getKey())),
							targetObj, outputParamsRef.getProperties());
					if (targetList.size() > i) {
						((Map<String, Object>) targetList.get(i)).put(outputParamsRef.getKey(), targetObj);
					}
					else {
						Map<String, Object> tempObj = new HashMap<>();
						tempObj.put(outputParamsRef.getKey(), targetObj);
						targetList.add(tempObj);
					}
				}
				else if (type.equals(PARAM_TYPE_ARRAY_OBJECT_LOWER_CASE)) {
					List jsonArray = new ArrayList();
					constructArray(((List) ((Map<String, Object>) o).get(outputParamsRef.getKey())), jsonArray,
							outputParamsRef.getProperties());
					if (targetList.size() > i) {
						((Map<String, Object>) targetList.get(i)).put(outputParamsRef.getKey(), jsonArray);
					}
					else {
						Map<String, Object> tempObj = new HashMap<>();
						tempObj.put(outputParamsRef.getKey(), jsonArray);
						targetList.add(tempObj);
					}
				}
				else if (type.startsWith("array")) {
					if (targetList.size() > i) {
						((Map<String, Object>) targetList.get(i)).put(outputParamsRef.getKey(),
								((Map<String, Object>) o).get(outputParamsRef.getKey()));
					}
					else {
						Map<String, Object> newObj = new HashMap<>();
						newObj.put(outputParamsRef.getKey(), ((Map<String, Object>) o).get(outputParamsRef.getKey()));
						targetList.add(newObj);
					}
				}
			}
		}
	}

	/**
	 * Validates node parameters.
	 * @param graph The workflow graph
	 * @param node The node to validate
	 * @return Validation result
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = CheckNodeParamResult.success();
		result.setNodeId(node.getId());
		result.setNodeName(node.getName());
		result.setNodeType(node.getType());
		return result;
	}

	/**
	 * Validates input parameters.
	 * @param inputParams List of input parameters
	 * @return Validation result
	 */
	protected CheckNodeParamResult checkInputParams(List<Node.InputParam> inputParams) {
		CheckNodeParamResult result = CheckNodeParamResult.success();
		if (!CollectionUtils.isEmpty(inputParams)) {
			List<Node.InputParam> errorInputParams = inputParams.stream().filter(inputParam -> {
				if (inputParam.getValue() == null) {
					return true;
				}
				return false;
			}).collect(Collectors.toList());
			if (!CollectionUtils.isEmpty(errorInputParams)) {
				result.setSuccess(false);
				result.setErrorInfos(errorInputParams.stream()
					.map(inputParam -> "input param [" + inputParam.getKey() + "] is empty or is illegal")
					.collect(Collectors.toList()));
			}
		}
		return result;
	}

	/**
	 * Initializes node result and refreshes context.
	 * @param node The node to initialize
	 * @param context The workflow context
	 * @return Initialized node result
	 */
	protected NodeResult initNodeResultAndRefreshContext(Node node, WorkflowContext context) {
		NodeResult nodeResult = new NodeResult();
		nodeResult.setNodeId(node.getId());
		nodeResult.setNodeName(node.getName());
		nodeResult.setNodeType(node.getType());
		nodeResult.setUsages(null);
		nodeResult.setNodeStatus(NodeStatusEnum.EXECUTING.getCode());
		context.getNodeResultMap().put(node.getId(), nodeResult);
		workflowInnerService.refreshContextCache(context);
		return nodeResult;
	}

	/**
	 * Constructs short-term memory messages.
	 * @param node The node with memory configuration
	 * @param shortTermMemory Memory configuration
	 * @param context The workflow context
	 * @return List of memory messages
	 */
	protected List<Message> constructShortTermMemory(Node node, ShortTermMemory shortTermMemory,
			WorkflowContext context) {
		if (shortTermMemory == null || BooleanUtils.isNotTrue(shortTermMemory.getEnabled())) {
			return Lists.newArrayList();
		}
		if (context.getConversationId() == null) {
			return Lists.newArrayList();
		}
		String type = shortTermMemory.getType() == null ? MEMORY_TYPE_CUSTOM : shortTermMemory.getType();
		if (MEMORY_TYPE_SELF.equals(type)) {
			List<ChatMessage> messageList = redisManager.get(String.format(WORKFLOW_NODE_SELF_SHORT_MEMORY_TEMPLATE,
					context.getAppId(), context.getConversationId(), node.getId()));
			if (org.apache.commons.collections.CollectionUtils.isEmpty(messageList)) {
				return Lists.newArrayList();
			}
			return messageList.stream().map(message -> {
				MessageRole role = message.getRole();
				if (MessageRole.USER.getValue().equals(role.getValue())) {
					return new UserMessage((String) message.getContent());
				}
				else if (MessageRole.ASSISTANT.getValue().equals(role.getValue())) {
					return new AssistantMessage((String) message.getContent());
				}
				else if (MessageRole.SYSTEM.getValue().equals(role.getValue())) {
					return new SystemMessage((String) message.getContent());
				}
				else {
					return null;
				}
			}).filter(obj -> obj != null).collect(Collectors.toList());
		}
		else {
			Node.InputParam param = shortTermMemory.getParam();
			if (param == null) {
				return Lists.newArrayList();
			}
			List<Message> resultList = Lists.newArrayList();
			if (param != null) {
				Object shortValue = VariableUtils.getValueFromContext(param, context);
				if (shortValue instanceof List) {
					List shortValues = (List) shortValue;
					for (Object value : shortValues) {
						if (value instanceof Message) {
							resultList.add((Message) value);
						}
						else {
							Map<Object, Object> messageMap = JsonUtils.fromJsonToMap(JsonUtils.toJson(value));
							if (messageMap != null) {
								String role = MapUtils.getString(messageMap, "role");
								String content = MapUtils.getString(messageMap, "content");
								if (MessageRole.USER.getValue().equals(role)) {
									resultList.add(new UserMessage(content));
								}
								else if (MessageRole.ASSISTANT.getValue().equals(role)) {
									resultList.add(new AssistantMessage(content));
								}
								else if (MessageRole.SYSTEM.getValue().equals(role)) {
									resultList.add(new SystemMessage(content));
								}
								else {
									throw new BizException(ErrorCode.INVALID_PARAMS.toError("short term memory",
											"Short term memory schema is invalid"));
								}
							}
						}
					}
				}
			}
			return resultList;
		}
	}

	/**
	 * Converts Spring AI messages to chat messages.
	 * @param messages List of Spring AI messages
	 * @return List of chat messages
	 */
	protected List<ChatMessage> convertToChatMessage(List<Message> messages) {
		return messages.stream().map(message -> {
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setRole(MessageRole.of(message.getMessageType().getValue()));
			chatMessage.setContent(message.getText());
			return chatMessage;
		}).collect(Collectors.toList());
	}

}
