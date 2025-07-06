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

package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A NodeAction that uses the RAG pipeline to generate a response based on user input.
 *
 * @author hupei
 */
public class RagNode implements NodeAction {

    private static final Logger logger = LoggerFactory.getLogger(RagNode.class);

    private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    private final ChatClient chatClient;

    public RagNode(RetrievalAugmentationAdvisor retrievalAugmentationAdvisor, ChatClient chatClient) {
        this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
        this.chatClient = chatClient;
    }

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        logger.info("rag_node is running.");
        String query = state.value("query", String.class)
                .orElseThrow(() -> new IllegalArgumentException("Query is missing from state"));

        // Use the advisor to get the RAG-enhanced response directly
        Flux<ChatResponse> streamResult = chatClient.prompt()
                .advisors(this.retrievalAugmentationAdvisor)
                .user(query)
                .stream()
                .chatResponse()
                .timeout(Duration.ofSeconds(60))
                .retry(2);
        logger.info("RAG node produced a result.");

        var generatedContent = StreamingChatGenerator.builder()
                .startingNode("rag_llm_stream")
                .startingState(state)
                .mapResult(response -> Map.of("rag_content",
                        Objects.requireNonNull(response.getResult().getOutput().getText()))).build(streamResult);
        logger.info("RAG node produced a streaming result.");
        Map<String, Object> updated = new HashMap<>();
        updated.put("rag_content", generatedContent);

        return updated;
    }

}
