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

package com.alibaba.cloud.ai.studio.core.workflow.processor.impl;

import com.alibaba.cloud.ai.studio.runtime.domain.agent.AgentResponse;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.ChatMessage;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.MessageRole;
import com.alibaba.cloud.ai.studio.runtime.domain.chat.Usage;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Edge;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.Node;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeResult;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeStatusEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ModelConfig;
import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.ShortTermMemory;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.config.CommonConfig;
import com.alibaba.cloud.ai.studio.core.base.manager.ModelExecuteManager;
import com.alibaba.cloud.ai.studio.core.base.manager.RedisManager;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowContext;
import com.alibaba.cloud.ai.studio.core.utils.common.VariableUtils;
import com.alibaba.cloud.ai.studio.core.workflow.WorkflowInnerService;
import com.alibaba.cloud.ai.studio.core.workflow.processor.AbstractExecuteProcessor;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Question Classification Node Processor
 * <p>
 * This class is responsible for handling the execution of classification nodes in the
 * workflow. It provides functionality for: 1. Text classification using LLM models 2.
 * Support for both efficient and advanced classification modes 3. Multi-branch decision
 * making based on classification results 4. Customizable classification conditions and
 * examples 5. Integration with short-term memory for context-aware classification 6.
 * Decision reasoning and thought process tracking 7. Default branch handling for
 * unclassified cases 8. Result deduplication and validation
 *
 * @version 1.0.0-M1
 */
@Component("ClassifierExecuteProcessor")
public class ClassifierExecuteProcessor extends AbstractExecuteProcessor {

	// Pattern for extracting decision results
	private final static Pattern DECISION_PATTERN = Pattern.compile("<Decision>：(.*?)\\s|<Decision>：(.*)",
			Pattern.DOTALL);

	// Pattern for extracting thought process
	private final static Pattern THOUGHT_PATTERN = Pattern.compile("(?<=<Thinking>：).*?(?=<Decision>)", Pattern.DOTALL);

	private static final Logger log = LoggerFactory.getLogger(ClassifierExecuteProcessor.class);

	private final ModelExecuteManager modelExecuteManager;

	public ClassifierExecuteProcessor(RedisManager redisManager, WorkflowInnerService workflowInnerService,
			ChatMemory conversationChatMemory, CommonConfig commonConfig, ModelExecuteManager modelExecuteManager) {
		super(redisManager, workflowInnerService, conversationChatMemory, commonConfig);
		this.modelExecuteManager = modelExecuteManager;
	}

	@Override
	public String getNodeType() {
		return NodeTypeEnum.CLASSIFIER.getCode();
	}

	@Override
	public String getNodeDescription() {
		return NodeTypeEnum.CLASSIFIER.getDesc();
	}

	/**
	 * Executes the classification operation
	 * @param graph The workflow graph
	 * @param node The current node to be executed
	 * @param context The workflow context
	 * @return NodeResult containing the classification results and branch information
	 */
	@Override
	public NodeResult innerExecute(DirectedAcyclicGraph<String, Edge> graph, Node node, WorkflowContext context) {

		NodeResult nodeResult = initNodeResultAndRefreshContext(node, context);

		// 获取决策、thought及用量，设置输入变量
		DecisionAndThoughtAndUsage decisionAndThoughtAndUsage = constructDecisionAndThought(node, context, nodeResult);
		TargetIdAndActualDecision targetIdAndActualDecision = fetchTargetIdAndActualDecision(graph, node,
				decisionAndThoughtAndUsage, context);
		targetIdAndActualDecision.setTargetIds(removeDuplicates(targetIdAndActualDecision.getTargetIds()));
		targetIdAndActualDecision.setActualDecision(removeDuplicates(targetIdAndActualDecision.getActualDecision()));
		targetIdAndActualDecision.setConditionId(removeDuplicates(targetIdAndActualDecision.getConditionId()));
		// 构建结束
		// 设置输出变量
		Map<String, Object> subjectAndThoughObj = new HashMap<>();
		subjectAndThoughObj.put("subject", targetIdAndActualDecision.getActualDecision().get(0));
		subjectAndThoughObj.put("thought", decisionAndThoughtAndUsage.getThought());
		nodeResult.setOutput(JsonUtils.toJson(subjectAndThoughObj));
		if (decisionAndThoughtAndUsage.getUsage() != null) {
			nodeResult.setUsages(Lists.newArrayList(decisionAndThoughtAndUsage.getUsage()));
		}
		nodeResult.setMultiBranch(true);
		NodeResult.MultiBranchReference branchReference = new NodeResult.MultiBranchReference();
		branchReference.setConditionId(targetIdAndActualDecision.getConditionId().toString());
		branchReference.setTargetIds(targetIdAndActualDecision.getTargetIds());
		nodeResult.setMultiBranchResults(Lists.newArrayList(branchReference));

		return nodeResult;
	}

	/**
	 * Retrieves target IDs and actual decisions based on classification results
	 * @param graph The workflow graph
	 * @param node The current node
	 * @param decisionAndThoughtAndUsage The classification results and thought process
	 * @return TargetIdAndActualDecision containing target IDs and decisions
	 */
	private TargetIdAndActualDecision fetchTargetIdAndActualDecision(DirectedAcyclicGraph<String, Edge> graph,
			Node node, DecisionAndThoughtAndUsage decisionAndThoughtAndUsage, WorkflowContext context) {
		TargetIdAndActualDecision result = new TargetIdAndActualDecision();
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		List<Condition> conditions = config.getConditions();
		// 默认分支
		Optional<Condition> defaultOptional = conditions.stream()
			.filter(condition -> condition.getId().equals("default"))
			.findAny();
		Condition defaultCondition = defaultOptional.get();

		List<String> decisions = decisionAndThoughtAndUsage.getDecisions();

		if (!decisions.isEmpty() && checkDecision(decisions)) {
			for (String decision : decisions) {
				if (Integer.parseInt(decision) == -1) {
					result.getConditionId().add(defaultCondition.getId());
					List<String> targetIds = graph.outgoingEdgesOf(node.getId())
						.stream()
						.filter(edge -> edge.getSourceHandle().equals(node.getId() + "_default"))
						.map(Edge::getTarget)
						.collect(Collectors.toList());
					result.getTargetIds().addAll(targetIds);
					result.getActualDecision().add("Other");
				}
				else {
					Condition condition = conditions.get(Integer.parseInt(decision));
					result.getConditionId().add(condition.getId());
					List<String> targetIds = graph.outgoingEdgesOf(node.getId())
						.stream()
						.filter(edge -> edge.getSourceHandle().equals(node.getId() + "_" + condition.getId()))
						.map(Edge::getTarget)
						.collect(Collectors.toList());
					result.getTargetIds().addAll(targetIds);
					result.getActualDecision().add(replaceTemplateContent(condition.getSubject(), context));
				}
			}
			return result;
		}
		else {
			result.getConditionId().add(defaultCondition.getId());
			List<String> targetIds = graph.outgoingEdgesOf(node.getId())
				.stream()
				.filter(edge -> edge.getSourceHandle().equals(node.getId() + "_default"))
				.map(Edge::getTarget)
				.collect(Collectors.toList());
			result.getTargetIds().addAll(targetIds);
			result.getActualDecision().add("Other");
			return result;
		}
	}

	/**
	 * Validates if the decision results are valid numeric values
	 * @param decision List of decision results to validate
	 * @return true if all decisions are valid numbers, false otherwise
	 */
	public boolean checkDecision(List<String> decision) {
		for (String item : decision) {
			if (!NumberUtils.isCreatable(item)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes duplicate values from a list
	 * @param list The list to deduplicate
	 * @return A new list containing unique values
	 */
	public List<String> removeDuplicates(List<String> list) {
		// 使用HashSet去重
		Set<String> set = new HashSet<>(list);
		// 将set转换回list
		List<String> uniqueList = new ArrayList<>(set);
		return uniqueList;
	}

	/**
	 * Container for target IDs and actual decisions
	 */
	@Data
	private static class TargetIdAndActualDecision {

		private List<String> targetIds = new ArrayList<>();

		private List<String> actualDecision = new ArrayList<>();

		private List<String> conditionId = new ArrayList<>();

	}

	/**
	 * Constructs the decision results and thought process
	 * @param node The current node
	 * @param context The workflow context
	 * @param nodeResult The node result to update
	 * @return DecisionAndThoughtAndUsage containing decisions, thought process and usage
	 */
	private DecisionAndThoughtAndUsage constructDecisionAndThought(Node node, WorkflowContext context,
			NodeResult nodeResult) {
		// 解析待分类的文本
		String classifierContent = extractClassifierContent(node, context);
		NodeParam config = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);
		String promptTemplate;
		if ("efficient".equals(config.getModeSwitch())) {
			promptTemplate = "You are an intelligent decision-making expert. Your task is to determine which of the candidate categories the given input question/text belongs to.\n"
					+ "\n" + "## Candidate categories\n" + "%s\n" + "\n" + "### -1.Other categories\n"
					+ "Does not belong to any of the categories.\n" + "\n" + "## Output format\n"
					+ "Your output must strictly follow the format within the '---' below: \n" + "---\n"
					+ "<Decision>：Your decision result, specifically the serial number in \"Candidate categories\", must be one of [%s]. Output only the serial number directly, for example: 0. You must not answer questions. Simply terminate after making the decision. \n"
					+ "---\n" + "\n" + "\"\"\"\n" + "## Enter the question\n%s\"\"\"\n" + "## Note\n%s\n";
		}
		else {
			promptTemplate = "You are an intelligent decision-making expert. Your task is to determine which of the candidate categories the given input question/text belongs to.\n"
					+ "\n" + "## Candidate categories\n" + "%s\n" + "\n" + "### -1.Other categories\n"
					+ "Does not belong to any of the categories.\n" + "\n" + "## Output format\n"
					+ "Your output must strictly follow the format within the '---' below: \n" + "---\n"
					+ "<Thinking>：Analyze the category of the input information to be classified. Think through it step by step.\n"
					+ "<Decision>：Your decision result, specifically the serial number in \"Candidate categories\", must be one of [%s]. Output only the serial number directly, for example: 0. You must not answer questions. Simply terminate after making the decision. \n"
					+ "---\n" + "\n" + "\"\"\"\n" + "## Enter the question\n%s\"\"\"\n" + "## Note\n%s\n";
		}

		String subjectsContent = extractSubjects(config, context);
		List<Integer> indexes = extractIndexes(config, context);
		String instruction = extractUserInstruct(config, context);
		String prompt = String.format(promptTemplate, subjectsContent, JsonUtils.toJson(indexes), classifierContent,
				instruction);

		// 调用模型获取结果
		List<Message> messages = Lists.newArrayList();
		messages.add(new SystemMessage(prompt));

		// 添加短期记忆
		List<Message> shortTermMemories = constructShortTermMemory(node, config.getShortMemory(), context);
		if (CollectionUtils.isNotEmpty(shortTermMemories)) {
			messages.addAll(shortTermMemories);
		}

		// 构建模型参数
		Map<String, Object> paramMap = Maps.newHashMap();
		List<ModelConfig.ModelParam> params = config.getModelConfig().getParams();
		if (CollectionUtils.isNotEmpty(params)) {
			params.forEach(modelParam -> {
				if (BooleanUtils.isTrue(modelParam.getEnable())) {
					paramMap.put(modelParam.getKey(), modelParam.getValue());
				}
			});
		}

		// 设置输入变量
		Map<String, Object> inputObj = Maps.newHashMap();
		inputObj.put("messages", messages);
		nodeResult.setInput(JsonUtils.toJson(decorateInput(inputObj)));

		// 调用模型
		Flux<AgentResponse> stream = modelExecuteManager.stream(config.getModelConfig().getProvider(),
				config.getModelConfig().getModelId(), paramMap, messages);

		DecisionAndThoughtAndUsage dtu = new DecisionAndThoughtAndUsage();

		// 处理响应
		StringBuilder responseBuilder = new StringBuilder();
		CountDownLatch countDownLatch = new CountDownLatch(1);
		Usage usage = new Usage();
		stream.subscribe(response -> {
			if (response.getMessage() != null && response.getMessage().getContent() != null) {
				responseBuilder.append(response.getMessage().getContent());
			}
			if (response.getUsage() != null) {
				usage.setPromptTokens(response.getUsage().getPromptTokens());
				usage.setCompletionTokens(response.getUsage().getCompletionTokens());
				usage.setTotalTokens(response.getUsage().getTotalTokens());
			}
		}, error -> {
			nodeResult.setNodeStatus(NodeStatusEnum.FAIL.getCode());
			nodeResult.setErrorInfo("模型调用失败：" + error.getMessage());
			countDownLatch.countDown();
		}, () -> {
			String content = responseBuilder.toString();
			log.info("log used for query classify response:{} , requestId:{}", content, context.getRequestId());

			if ("efficient".equals(config.getModeSwitch())) {
				// 快速模式 - 直接提取决策结果
				dtu.setDecisions(Lists.newArrayList(content.trim()));
				dtu.setThought("");
			}
			else {
				// 效果模式 - 提取思考过程和决策结果
				Matcher decisionMatcher = DECISION_PATTERN.matcher(content);
				Matcher thoughtMatcher = THOUGHT_PATTERN.matcher(content);
				List<String> decisions = new ArrayList<>();
				String thought = "";

				if (decisionMatcher.find()) {
					String decisionContent = decisionMatcher.group(1) != null ? decisionMatcher.group(1)
							: decisionMatcher.group(2);
					if (decisionContent.contains("[")) {
						List<String> list = JsonUtils.fromJsonToList(decisionContent, String.class);
						decisions.addAll(list);
					}
					else {
						decisions.add(decisionContent);
					}
				}
				if (thoughtMatcher.find()) {
					thought = thoughtMatcher.group(0);
				}

				dtu.setDecisions(decisions);
				dtu.setThought(thought);
			}
			dtu.setUsage(usage);
			dtu.setOriginContent(content);
			countDownLatch.countDown();
		});
		try {
			countDownLatch.await(5, TimeUnit.MINUTES);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		return dtu;
	}

	/**
	 * Extracts the content to be classified from the node
	 * @param node The current node
	 * @param context The workflow context
	 * @return The content to be classified
	 */
	private String extractClassifierContent(Node node, WorkflowContext context) {
		Map<String, Object> inputParamsMap = constructInputParamsMap(node, context);
		Object o = inputParamsMap.get(INPUT_DECORATE_PARAM_KEY);
		if (o == null) {
			return null;
		}
		else if (o instanceof Map || o instanceof List) {
			return JsonUtils.toJson(o);
		}
		else {
			return "" + o;
		}
	}

	/**
	 * Extracts subject information from the configuration
	 * @param config The node configuration
	 * @param context The workflow context
	 * @return Formatted subject information
	 */
	private String extractSubjects(NodeParam config, WorkflowContext context) {
		List<Condition> conditions = config.getConditions();
		if (CollectionUtils.isEmpty(conditions)) {
			return "";
		}
		int counter = 0;
		StringBuilder subjectStringBuilder = new StringBuilder();
		for (int i = 0; i < conditions.size(); i++) {
			String conditionId = conditions.get(i).getId();
			if ("default".equals(conditionId)) {
				conditions.get(i).setOrder(-1);
			}
			else {
				counter++;
				conditions.get(i).setOrder(counter);
				subjectStringBuilder.append("### " + conditions.get(i).getOrder() + "."
						+ replaceTemplateContent(conditions.get(i).getSubject(), context) + "\n");
			}
		}
		return subjectStringBuilder.toString();
	}

	/**
	 * Extracts index information from the configuration
	 * @param config The node configuration
	 * @param context The workflow context
	 * @return List of valid indexes
	 */
	private List<Integer> extractIndexes(NodeParam config, WorkflowContext context) {
		List<Condition> conditions = config.getConditions();
		if (CollectionUtils.isEmpty(conditions)) {
			return Lists.newArrayList();
		}
		List<Integer> indexes = Lists.newArrayList();
		int counter = 0;
		for (int i = 0; i < conditions.size(); i++) {
			String conditionId = conditions.get(i).getId();
			if ("default".equals(conditionId)) {
				continue;
			}
			counter++;
			indexes.add(counter);
		}
		return indexes;
	}

	/**
	 * Extracts user instructions from the configuration
	 * @param config The node configuration
	 * @param context The workflow context
	 * @return User instructions for classification
	 */
	private String extractUserInstruct(NodeParam config, WorkflowContext context) {
		String instruction = config.getInstruction();
		if (StringUtils.isBlank(instruction)) {
			return "暂无";
		}
		return replaceTemplateContent(instruction, context);
	}

	/**
	 * Configuration parameters for the classifier node
	 */
	@Data
	public static class NodeParam {

		@JsonProperty("model_config")
		private ModelConfig modelConfig;

		private String instruction;

		private List<Condition> conditions;

		// 模式设置，advanced:效果模式 efficient:快速模式
		@JsonProperty("mode_switch")
		private String modeSwitch = "advanced";

		@JsonProperty("short_memory")
		private ShortTermMemory shortMemory;

	}

	/**
	 * Represents a classification condition
	 */
	@Data
	public static class Condition {

		private String id;

		private String subject;

		private String examples;

		private Integer order;

	}

	/**
	 * Container for decision results, thought process and usage information
	 */
	@Data
	private static class DecisionAndThoughtAndUsage {

		private List<String> decisions;

		private String thought;

		@JsonProperty("origin_content")
		private String originContent;

		private Usage usage;

	}

	/**
	 * Validates the node parameters including conditions and model configuration
	 * @param graph The workflow graph
	 * @param node The node to validate
	 * @return CheckNodeParamResult containing validation results
	 */
	@Override
	public CheckNodeParamResult checkNodeParam(DirectedAcyclicGraph<String, Edge> graph, Node node) {
		CheckNodeParamResult result = super.checkNodeParam(graph, node);
		CheckNodeParamResult inputParamsResult = checkInputParams(node.getConfig().getInputParams());
		if (!inputParamsResult.isSuccess()) {
			result.setSuccess(false);
			result.getErrorInfos().addAll(inputParamsResult.getErrorInfos());
		}
		NodeParam nodeParam = JsonUtils.fromMap(node.getConfig().getNodeParam(), NodeParam.class);

		ModelConfig modelConfig = nodeParam.getModelConfig();
		if (modelConfig == null || StringUtils.isBlank(modelConfig.getModelId())) {
			result.setSuccess(false);
			result.getErrorInfos().add("[modelConfig] is missing");
		}
		StringBuilder stringBuilder = new StringBuilder();
		List<Condition> conditions = nodeParam.getConditions();
		if (CollectionUtils.isEmpty(conditions)) {
			result.setSuccess(false);
			stringBuilder.append("[conditions] is missing;\n");
		}
		else {
			for (int i = 0; i < conditions.size(); i++) {
				Condition condition = conditions.get(i);
				if (!"default".equals(condition.getId())
						&& (condition.getSubject() == null || condition.getSubject().isEmpty())) {
					result.setSuccess(false);
					stringBuilder.append("[conditions.").append(i).append(".subject] is missing;\n");
				}
			}
		}

		String errorInfo = stringBuilder.toString();
		if (StringUtils.isNotBlank(errorInfo)) {
			result.setSuccess(false);
			result.getErrorInfos().add(errorInfo);
		}
		return result;
	}

	/**
	 * Handles short-term memory for the current round of classification
	 * @param node The current node
	 * @param round The current round number
	 * @param context The workflow context
	 * @param nodeResult The result of the node execution
	 */
	@Override
	public void handleCurrentRound4SelfShortTermMemory(Node node, Integer round, WorkflowContext context,
			NodeResult nodeResult) {
		List<ChatMessage> chatMessages = Lists.newArrayList();
		chatMessages.add(new ChatMessage(MessageRole.USER,
				VariableUtils.getValueFromContext(node.getConfig().getInputParams().get(0), context)));
		Map<Object, Object> outputMap = JsonUtils.fromJsonToMap(nodeResult.getOutput());
		chatMessages
			.add(new ChatMessage(MessageRole.ASSISTANT, "Matched classification is " + outputMap.get("subject")));
		NodeResult.ShortMemory shortMemory = new NodeResult.ShortMemory();
		shortMemory.setRound(round);
		shortMemory.setCurrentSelfChatMessages(chatMessages);
		nodeResult.setShortMemory(shortMemory);
	}

}
