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

package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.QuestionClassifierNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;

import org.springframework.stereotype.Component;

@Component
public class QuestionClassifierNodeSection implements NodeSection<QuestionClassifierNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.QUESTION_CLASSIFIER.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		QuestionClassifierNodeData nodeData = (QuestionClassifierNodeData) node.getData();
		return String.format("""
				// —— QuestionClassifierNode [%s] ——
				stateGraph.addNode("%s", AsyncNodeAction.node_async(
				    createQuestionClassifierAction(chatModel, %s, %s, "%s", "%s", %s, %s)
				));

				""", node.getId(), varName, ObjectToCodeUtil.toCode(nodeData.getChatModeName()),
				ObjectToCodeUtil.toCode(nodeData.getModeParams()), nodeData.getInputSelector().getNameInCode(),
				nodeData.getOutputKey(),
				ObjectToCodeUtil.toCode(nodeData.getClasses()
					.stream()
					.collect(Collectors.toUnmodifiableMap(QuestionClassifierNodeData.ClassConfig::id,
							QuestionClassifierNodeData.ClassConfig::classTemplate, (a, b) -> b))),
				ObjectToCodeUtil.toCode(List.of(nodeData.getPromptTemplate())));
	}

	@Override
	public String renderEdges(QuestionClassifierNodeData nodeData, List<Edge> edges) {
		Map<String, String> classIdToName = nodeData.getClassIdToName();
		// 规定edge的sourceHandle为caseId，前面的转化需要符合这条规则
		String edgeCode = String.format("""
				state -> {
				    String result = state.value("%s").orElseThrow().toString();
				    %s
				    throw new RuntimeException("invalid output");
				}
				""", nodeData.getOutputKey(),
				nodeData.getClasses()
					.stream()
					.map(QuestionClassifierNodeData.ClassConfig::id)
					.map(id -> String.format("""
							if("%s".equals(result)) {
							    return "%s";
							}
							""", id, classIdToName.getOrDefault(id, id)))
					.collect(Collectors.joining("\n")));

		Map<String, String> caseToTarget = edges.stream()
			.collect(Collectors.toUnmodifiableMap(
					e -> classIdToName.getOrDefault(e.getSourceHandle(), e.getSourceHandle()), Edge::getTarget));

		return String.format("""
				// render QuestionNode [%s]'s edge
				stateGraph.addConditionalEdges("%s", AsyncEdgeAction.edge_async(%s), %s);

				""", nodeData.getVarName(), nodeData.getVarName(), edgeCode, ObjectToCodeUtil.toCode(caseToTarget));
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO ->
				"""
						private NodeAction createQuestionClassifierAction(
						        ChatModel chatModel,
						        String chatModelName, Map<String, Number> modeParams,
						        String inputKey, String outputKey,
						        Map<String, String> categories, List<String> instructions) {
						    // build ChatClient
						    var chatOptionsBuilder = DashScopeChatOptions.builder().withModel(chatModelName);
						    Optional.ofNullable(modeParams.get("temperature"))
						            .ifPresent(val -> chatOptionsBuilder.withTemperature(val.doubleValue()));
						    Optional.ofNullable(modeParams.get("seed")).ifPresent(val -> chatOptionsBuilder.withSeed(val.intValue()));
						    Optional.ofNullable(modeParams.get("top_p")).ifPresent(val -> chatOptionsBuilder.withTopP(val.doubleValue()));
						    Optional.ofNullable(modeParams.get("top_k")).ifPresent(val -> chatOptionsBuilder.withTopK(val.intValue()));
						    Optional.ofNullable(modeParams.get("max_tokens"))
						            .ifPresent(val -> chatOptionsBuilder.withMaxToken(val.intValue()));
						    Optional.ofNullable(modeParams.get("repetition_penalty"))
						            .ifPresent(val -> chatOptionsBuilder.withRepetitionPenalty(val.doubleValue()));
						    final ChatClient chatClient = ChatClient.builder(chatModel).defaultOptions(chatOptionsBuilder.build()).build();

						    // build Node
						    return QuestionClassifierNode.builder()
						            .chatClient(chatClient)
						            .inputTextKey(inputKey)
						            .outputKey(outputKey)
						            .categories(categories)
						            .classificationInstructions(instructions)
						            .build();
						}
						""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.QuestionClassifierNode",
				"org.springframework.beans.factory.annotation.Autowired",
				"com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions", "java.util.Optional");
	}

}
