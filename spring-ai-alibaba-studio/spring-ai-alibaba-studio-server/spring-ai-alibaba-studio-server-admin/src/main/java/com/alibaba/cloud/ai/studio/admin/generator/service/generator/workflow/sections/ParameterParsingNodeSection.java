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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ParameterParsingNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.stereotype.Component;

@Component
public class ParameterParsingNodeSection implements NodeSection<ParameterParsingNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.PARAMETER_PARSING.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		ParameterParsingNodeData nodeData = ((ParameterParsingNodeData) node.getData());
		return String.format("""
				// -- ParameterParsingNode [%s] --
				stateGraph.addNode("%s", AsyncNodeAction.node_async(
				    createParameterParsingAction(chatModel, %s, %s, %s, %s, %s, %s, %s, %s, "%s")
				));

				""", node.getId(), varName, ObjectToCodeUtil.toCode(nodeData.getChatModeName()),
				ObjectToCodeUtil.toCode(nodeData.getModeParams()),
				ObjectToCodeUtil.toCode(nodeData.getInputSelector().getNameInCode()),
				ObjectToCodeUtil.toCode(nodeData.getParameters()), ObjectToCodeUtil.toCode(nodeData.getSuccessKey()),
				ObjectToCodeUtil.toCode(nodeData.getDataKey()), ObjectToCodeUtil.toCode(nodeData.getReasonKey()),
				ObjectToCodeUtil.toCode(nodeData.getInstruction()), varName);
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY, STUDIO ->
				"""
						private NodeAction createParameterParsingAction(
						        ChatModel chatModel,
						        String chatModelName, Map<String, Number> modeParams,
						        String inputKey, List<ParameterParsingNode.Param> parameters,
						        String successKey, String dataKey, String reasonKey, String instruction, String outputKeyPrefix) {
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
						    ParameterParsingNode node = ParameterParsingNode.builder()
						                              .inputText("")
						                              .inputTextKey(inputKey)
						                              .chatClient(chatClient)
						                              .parameters(parameters)
						                              .successKey(successKey)
						                              .dataKey(dataKey)
						                              .reasonKey(reasonKey)
						                              .instruction(instruction)
						                              .build();

						                      // unpack answer
						                      return state -> {
						                          Map<String, Object> res = node.apply(state);
						                          if(!(Boolean) res.get(successKey)) {
						                              return res;
						                          }
						                          Map<String, Object> finalRes = new HashMap<>(res);
						                          Map<String, Object> data = (Map<String, Object>) finalRes.remove(dataKey);
						                          finalRes.putAll(data.entrySet()
						                                    .stream()
						                                    .filter(e -> e.getValue() != null)
						                                    .map(e ->
						                                            Map.entry(outputKeyPrefix + "_" + e.getKey(), e.getValue()))
						                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
						                            );
						                          return finalRes;
						                      };
						}
						""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.ParameterParsingNode",
				"com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions", "java.util.Optional",
				"java.util.stream.Collectors");
	}

}
