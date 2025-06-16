package com.alibaba.cloud.ai.example.deepresearch.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import java.util.HashMap;
import java.util.Map;

/**
 * A NodeAction that uses the RAG pipeline to generate a response based on user input.
 *
 * @author hupei
 */
public class RagNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(RagNode.class);

	private final RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

	private final ChatClient.Builder chatClientBuilder;

	public RagNode(RetrievalAugmentationAdvisor retrievalAugmentationAdvisor, ChatClient.Builder chatClientBuilder) {
		this.retrievalAugmentationAdvisor = retrievalAugmentationAdvisor;
		this.chatClientBuilder = chatClientBuilder;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("rag_node is running.");
		String query = state.value("query", String.class)
			.orElseThrow(() -> new IllegalArgumentException("Query is missing from state"));

		// Use the advisor to get the RAG-enhanced response directly
		String ragResult = chatClientBuilder.build()
			.prompt()
			.advisors(this.retrievalAugmentationAdvisor)
			.user(query)
			.call()
			.content();

		logger.info("RAG node produced a result.");

		Map<String, Object> updated = new HashMap<>();
		updated.put("rag_content", ragResult);

		return updated;
	}

}
