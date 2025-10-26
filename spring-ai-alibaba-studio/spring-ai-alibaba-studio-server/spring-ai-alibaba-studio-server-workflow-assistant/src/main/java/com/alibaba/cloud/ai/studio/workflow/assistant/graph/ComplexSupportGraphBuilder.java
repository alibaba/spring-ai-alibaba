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

package com.alibaba.cloud.ai.studio.workflow.assistant.graph;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.node.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;

@Component
public class ComplexSupportGraphBuilder {

	@Value("classpath:data/manual.txt")
	private Resource manualResource;

	@Bean
	public CompiledGraph buildGraph(ChatModel chatModel, VectorStore vectorStore,
			ToolCallbackResolver toolCallbackResolver) throws GraphStateException {

		// ChatClient
		ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor()).build();

		// Build StateGraph with KeyStrategyFactory
		StateGraph graph = new StateGraph(() -> {
			Map<String, KeyStrategy> keyStrategyMap = new java.util.HashMap<>();
			for (String key : List.of("input", "attachments", "docs", "parameterParsing_output", "classifier_output",
					"retrieved_docs", "filtered_docs", "http_response", "llm_response", "tool_result", "human_feedback",
					"answer")) {
				keyStrategyMap.put(key, (o1, o2) -> o2);
			}
			return keyStrategyMap;
		});

		// —— 1. Document extraction ——
		DocumentExtractorNode extractNode = DocumentExtractorNode.builder()
			.fileList(List.of(getResourceFilePath(manualResource)))
			.paramsKey("attachments")
			.outputKey("docs")
			.build();
		graph.addNode("extractDocs", AsyncNodeAction.node_async(extractNode));

		// —— 2. Parameter parsing ——
		ParameterParsingNode paramNode = ParameterParsingNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.parameters(List.of(
				ParameterParsingNode.param("ticketId", "string", "工单编号"),
				ParameterParsingNode.param("priority", "string", "优先级")))
			.build();
		graph.addNode("parseParams", AsyncNodeAction.node_async(paramNode));

		// —— 3. Classification of issues ——
		QuestionClassifierNode qcNode = QuestionClassifierNode.builder()
			.chatClient(chatClient)
			.inputTextKey("input")
			.categories(List.of("售后", "技术支持", "投诉", "咨询"))
			.classificationInstructions(List.of("请仅返回最合适的类别名称String类型，例如：售后、运输、产品质量、其他；不要多余的标记或格式。 正确返回结果： 售后 "))
			.build();
		graph.addNode("classify", AsyncNodeAction.node_async(qcNode));

		// —— 4. Knowledge Retrieval ——
		KnowledgeRetrievalNode krNode = KnowledgeRetrievalNode.builder()
			.userPromptKey("classifier_output")
			.vectorStore(vectorStore)
			.topK(5)
			.similarityThreshold(0.5)
			.enableRanker(false)
			.build();
		graph.addNode("retrieveDocs", AsyncNodeAction.node_async(krNode));

		// —— 5. List Operate ——
		// ListOperatorNode<ListOperatorNode.StringElement> listOp =
		// ListOperatorNode.<ListOperatorNode.StringElement>builder()
		// .inputTextKey("input")
		// .outputTextKey("filtered_docs")
		// .filter(e -> e.contains("significant")) // 保留带“重要”关键字的文档
		// .limitNumber(5L)
		// .elementClassType(ListOperatorNode.StringElement.class)
		// .build();
		// graph.addNode("filterDocs", AsyncNodeAction.node_async(listOp));

		// —— 6. call http endpoint ——
		// in this case, you should create a mock http endpoint to test this node, and
		// change http_response to String
		HttpNode httpNode = HttpNode.builder()
			.webClient(WebClient.builder().build())
			.method(HttpMethod.GET)
			.url("http://localhost:8080/api/graph/mock/http?" + "ticketId=12345" + "&category=售后")
			.outputKey("http_response")
			.build();
		graph.addNode("syncTicket", AsyncNodeAction.node_async(httpNode));

		// —— 7. call LLM ——
		LlmNode llmNode = LlmNode.builder()
			.chatClient(chatClient)
			.systemPromptTemplate("你是客服助手，请基于以下信息撰写回复：")
			.userPromptTemplateKey("http_response")
			.messagesKey("user_prompt")
			.outputKey("llm_response")
			.build();
		graph.addNode("invokeLLM", AsyncNodeAction.node_async(llmNode));

		// —— 8. Perform a tool call (optional) ——
		ToolNode toolNode = ToolNode.builder()
			.llmResponseKey("llm_response")
			.outputKey("tool_result")
			.toolCallbackResolver(toolCallbackResolver)
			.toolNames(List.of("sendEmail", "updateCRM"))
			.build();
		graph.addNode("invokeTool", AsyncNodeAction.node_async(toolNode));

		// —— 9. human callback ——
		// HumanNode humanNode = new HumanNode("conditioned",
		// 	st -> st.value("tool_result").map(r -> r.toString().contains("ERROR")).orElse(false),
		// 	st -> Map.of("answer", st.value("tool_result").orElse("").toString()));
		// graph.addNode("humanReview", AsyncNodeAction.node_async(humanNode));

		// —— 10. end print (this node need to defined in ssa)——
		AnswerNode ansNode = AnswerNode.builder().answer("{{answer}}").build();
		graph.addNode("finalAnswer", AsyncNodeAction.node_async(ansNode));

		graph.addEdge(START, "extractDocs")
			.addEdge("extractDocs", "parseParams")
			.addEdge("parseParams", "classify")
			.addEdge("classify", "retrieveDocs")
			.addEdge("retrieveDocs", "syncTicket")
			// .addEdge("filterDocs", "syncTicket")
			.addEdge("syncTicket", "invokeLLM")
			.addEdge("invokeLLM", "invokeTool")
			.addEdge("invokeTool", "finalAnswer")
			// .addEdge("humanReview", "finalAnswer")
			.addEdge("finalAnswer", END);

		return graph.compile();
	}

	/**
	 * Converts a Spring {@link Resource} to an absolute file path.
	 * <p>
	 * This is a workaround because {@code DocumentExtractorNode} requires a direct file system path
	 * and cannot handle classpath resources, especially when running from a JAR file.
	 * </p><p>
	 * This method implements a robust strategy:
	 * <ul>
	 *     <li>If the resource is a file on the file system (e.g., during development), it returns its absolute path.</li>
	 *     <li>If the resource is inside a JAR file, it extracts the resource to a temporary file
	 *     and returns the path to that temporary file.</li>
	 * </ul>
	 * A shutdown hook is registered to clean up the temporary file on JVM exit.
	 * </p>
	 *
	 * @param resource the Spring resource to resolve.
	 * @return the absolute file path of the resource.
	 * @throws RuntimeException if the resource cannot be resolved to a file path.
	 */
	private String getResourceFilePath(Resource resource) {
		try {
			// This works only if the resource is directly on the filesystem.
			// It will fail if the resource is inside a JAR.
			return resource.getFile().getAbsolutePath();
		}
		catch (IOException e) {
			// This is the fallback for resources inside a JAR.
			try {
				Path tempFile = Files.createTempFile("resource-", "-" + resource.getFilename());
				try (var inputStream = resource.getInputStream()) {
					Files.copy(inputStream, tempFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
				// Ensure the temporary file is deleted when the application shuts down.
				Runtime.getRuntime().addShutdownHook(new Thread(() -> {
					try {
						Files.deleteIfExists(tempFile);
					}
					catch (IOException ex) {
						// Log the error but don't prevent shutdown
						System.err.println("Failed to delete temporary file: " + tempFile);
					}
				}));
				return tempFile.toAbsolutePath().toString();
			}
			catch (IOException ex) {
				throw new RuntimeException("Failed to resolve resource to a file path: " + resource, ex);
			}
		}
	}

}
