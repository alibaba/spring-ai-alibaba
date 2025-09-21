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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.LLMNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.stereotype.Component;

import java.util.List;

// TODO：支持异常分支、支持DashScope平台以外其他模型、Dify的结构化输出
@Component
public class LLMNodeSection implements NodeSection<LLMNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.LLM.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		LLMNodeData nodeData = ((LLMNodeData) node.getData());
		return String.format("""
				// —— LLMNode [%s] ——
				stateGraph.addNode("%s", AsyncNodeAction.node_async(
				    createLLMNodeAction(chatModel, %s, %s, %s, %s, %s, %s, %s, %s, %s)
				));

				""", node.getId(), varName, ObjectToCodeUtil.toCode(nodeData.getChatModeName()),
				ObjectToCodeUtil.toCode(nodeData.getModeParams()),
				ObjectToCodeUtil.toCode(nodeData.getMessageTemplates()),
				ObjectToCodeUtil.toCode(nodeData.getMemoryKey()), ObjectToCodeUtil.toCode(nodeData.getMaxRetryCount()),
				ObjectToCodeUtil.toCode(nodeData.getRetryIntervalMs()),
				ObjectToCodeUtil.toCode(nodeData.getDefaultOutput()),
				ObjectToCodeUtil.toCode(nodeData.getErrorNextNode()),
				ObjectToCodeUtil.toCode(nodeData.getOutputKeyPrefix()));
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {

		return String.format(
				"""
						private record MessageTemplate(String template, List<String> keys, MessageType type) {
						    public Message render(OverAllState state) {
						        Map<String, Object> params = keys.stream()
						            .collect(Collectors.toMap(key -> key, key -> state.value(key, ""), (o1, o2) -> o2));
						        String text = new PromptTemplate(template).render(params);
						        return switch (type) {
						            case USER -> new UserMessage(text);
						            case SYSTEM -> new SystemMessage(text);
						            case TOOL -> throw new UnsupportedOperationException("Tool message not supported");
						            case ASSISTANT -> new AssistantMessage(text);
						        };
						    }
						}

						private NodeAction createLLMNodeAction(ChatModel chatModel,
						        String chatModelName, Map<String, Number> modeParams,
						        List<MessageTemplate> messageTemplates, String memoryKey, Integer maxRetryCount, Integer retryIntervalMs,
						        String defaultOutput, String errorNextNode, String outputKeyPrefix) {
						    // build chatClient with params
						    var chatOptionsBuilder = DashScopeChatOptions.builder().withModel(chatModelName);
						    Optional.ofNullable(modeParams.get("temperature")).ifPresent(val -> chatOptionsBuilder.withTemperature(val.doubleValue()));
						    Optional.ofNullable(modeParams.get("seed")).ifPresent(val -> chatOptionsBuilder.withSeed(val.intValue()));
						    Optional.ofNullable(modeParams.get("top_p")).ifPresent(val -> chatOptionsBuilder.withTopP(val.doubleValue()));
						    Optional.ofNullable(modeParams.get("top_k")).ifPresent(val -> chatOptionsBuilder.withTopK(val.intValue()));
						    Optional.ofNullable(modeParams.get("max_tokens")).ifPresent(val -> chatOptionsBuilder.withMaxToken(val.intValue()));
						    Optional.ofNullable(modeParams.get("repetition_penalty")).ifPresent(val -> chatOptionsBuilder.withRepetitionPenalty(val.doubleValue()));
						    final ChatClient chatClient = ChatClient.builder(chatModel).defaultOptions(chatOptionsBuilder.build()).build();

						    String nextNodeKey = "next_node";

						    return state -> {
						        String memories;
						        if (memoryKey == null || state.value(memoryKey).isEmpty()) {
						            memories = "This is the user's first request without context";
						        }
						        else {
						            memories = String.format("This is the history of previous requests:\\n %%s",
						                    state.value(memoryKey, List.of()).toString());
						        }

						        // call chatClient
						        int retryCount = Optional.ofNullable(maxRetryCount).orElse(1);
						        int retryInterval = Optional.ofNullable(retryIntervalMs).orElse(1000);
						        while (retryCount-- > 0) {
						            try {
						                // build messages
						                List<Message> messages = messageTemplates.stream()
						                    .map(messageTemplate -> messageTemplate.render(state))
						                    .toList();
						                String content = chatClient.prompt().system(memories).messages(messages).call().content();
						                if (content == null) {
						                    throw new RuntimeException("ChatClient error");
						                }
						                Map<String, Object> map = new HashMap<>(%s);
						                if (memoryKey != null) {
						                    map.put(memoryKey, content);
						                }
						                return map;
						            }
						            catch (Exception e) {
						                try {
						                    Thread.sleep(retryInterval);
						                } catch (InterruptedException ie) {
						                    Thread.currentThread().interrupt();
						                    break;
						                }
						            }
						        }

						        // error handling
						        if (defaultOutput != null) {
						            return %s;
						        }
						        else if (errorNextNode != null) {
						            return Map.of(nextNodeKey, errorNextNode);
						        }
						        else {
						            throw new IllegalStateException("No default output or error next node provided");
						        }
						    };
						}
						""",
				dialectType.equals(DSLDialectType.DIFY) ? "Map.of(outputKeyPrefix + \"text\", content)"
						: "Map.of(outputKeyPrefix + \"output\", content, outputKeyPrefix + \"reasoning_content\", content)",
				dialectType.equals(DSLDialectType.DIFY) ? "Map.of(outputKeyPrefix + \"text\", defaultOutput)"
						: "Map.of(outputKeyPrefix + \"output\", defaultOutput, outputKeyPrefix + \"reasoning_content\", defaultOutput)");
	}

	@Override
	public List<String> getImports() {
		return List.of("org.springframework.ai.chat.messages.Message",
				"org.springframework.ai.chat.messages.AssistantMessage",
				"org.springframework.ai.chat.messages.MessageType",
				"org.springframework.ai.chat.messages.SystemMessage",
				"org.springframework.ai.chat.messages.UserMessage",
				"com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions",
				"org.springframework.beans.factory.annotation.Autowired", "java.util.Optional");
	}

}
