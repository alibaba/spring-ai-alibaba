/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.agenticrag;

import com.alibaba.cloud.ai.examples.multiagents.agenticrag.node.AnswerNode;
import com.alibaba.cloud.ai.examples.multiagents.agenticrag.node.CheckNode;
import com.alibaba.cloud.ai.examples.multiagents.agenticrag.node.ClassifyNode;
import com.alibaba.cloud.ai.examples.multiagents.agenticrag.node.RetrieveNode;
import com.alibaba.cloud.ai.examples.multiagents.agenticrag.tools.AgenticRagTools;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Agentic RAG pattern: the LLM decides whether retrieval is needed (classify), the
 * answer is generated from the retrieved context (answer), and a quality gate checks
 * whether the answer is complete and grounded (check). If not, the query is refined and
 * retrieval runs again, up to a configurable number of rounds.
 *
 * <p>In contrast to the fixed-pipeline RAG workflow in the {@code workflow} example
 * (rewrite → retrieve → prepare → agent), retrieval here is a tool the agent drives
 * dynamically, so simple questions skip retrieval entirely and complex questions can
 * trigger several retrieval rounds until the answer is complete.
 */
@Configuration
public class AgenticRagConfig {

	private static final String CLASSIFY_PROMPT = """
			You are the router of an agentic RAG system for a product knowledge base.
			Decide whether answering the question REQUIRES retrieving information from the knowledge base.
			The knowledge base contains: pricing plans, refund policies, API rate limits, features, and support information.
			Reply with exactly one word: RETRIEVE or ANSWER.
			- RETRIEVE: the question asks about facts that may be in the knowledge base (pricing, refunds, API, features, support).
			- ANSWER: the question is general or conversational, or it cannot be answered by the knowledge base.

			Question: %s
			""";

	private static final String ANSWER_PROMPT = """
			You are a customer support assistant for Acme Analytics, a SaaS analytics product.
			Answer the question using ONLY the context below.
			If the context does not contain the answer, state clearly that the information is not available in the knowledge base.
			Respond concisely, citing the specific facts you used.

			Context:
			%s

			Question: %s
			""";

	private static final String CHECK_PROMPT = """
			You are the quality gate of an agentic RAG system.
			Determine whether the answer below is complete and grounded in the provided context for the question.
			- If the answer fully addresses the question using the context, reply with exactly: COMPLETE
			- If the answer lacks required information (for example, the context misses part of the question, or the answer says the information is unavailable), reply with two lines:
			  INCOMPLETE
			  <a more specific search query that would find the missing information>

			Question: %s

			Context:
			%s

			Answer:
			%s
			""";

	private static final String AGENT_PROMPT = """
			You are the entry point of the agentic RAG demo.
			Use the answer_question tool to answer the user's question about the product knowledge base.
			""";

	/**
	 * In-memory vector store seeded with a small product knowledge base. In production,
	 * replace this with a persistent store (PostgreSQL + pgvector, Redis, etc.).
	 */
	@Bean
	public VectorStore agenticRagVectorStore(EmbeddingModel embeddingModel) {
		VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
		List<Document> docs = List.of(
				new Document("Pricing: The monthly plan costs $49 per month. The annual plan costs $490 per year and includes two months free.",
						Map.of("source", "pricing")),
				new Document("Refund policy: Annual plans can be refunded within 30 days of purchase for a full refund. After 30 days, refunds are prorated for the unused portion.",
						Map.of("source", "refunds")),
				new Document("Refund policy: Monthly plans are non-refundable. You can cancel at any time and the subscription ends at the end of the current billing period.",
						Map.of("source", "refunds")),
				new Document("API rate limits: The free tier allows 10 requests per minute. Paid plans allow 100 requests per minute. Enterprise plans have no rate limits.",
						Map.of("source", "api")),
				new Document("Features: Acme Analytics provides real-time dashboards, anomaly alerts, forecasting, and a public REST API.",
						Map.of("source", "features")),
				new Document("Support: Standard plans include email support with a 24-hour response time. Enterprise plans include 24/7 phone support and a dedicated account manager.",
						Map.of("source", "support")));
		vectorStore.add(docs);
		return vectorStore;
	}

	@Bean
	public CompiledGraph agenticRagGraph(ChatModel chatModel, VectorStore agenticRagVectorStore)
			throws GraphStateException {

		ClassifyNode classifyNode = new ClassifyNode(chatModel, CLASSIFY_PROMPT);
		RetrieveNode retrieveNode = new RetrieveNode(agenticRagVectorStore);
		AnswerNode answerNode = new AnswerNode(chatModel, ANSWER_PROMPT);
		CheckNode checkNode = new CheckNode(chatModel, CHECK_PROMPT);

		StateGraph graph = new StateGraph("agentic_rag", () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("question", new ReplaceStrategy());
			strategies.put("route", new ReplaceStrategy());
			strategies.put("search_query", new ReplaceStrategy());
			strategies.put("documents", new ReplaceStrategy());
			strategies.put("final_answer", new ReplaceStrategy());
			strategies.put("retry_count", new ReplaceStrategy());
			strategies.put("retrieval_rounds", new ReplaceStrategy());
			strategies.put("messages", new AppendStrategy(false));
			return strategies;
		});

		graph.addNode("classify", node_async(classifyNode))
				.addNode("retrieve", node_async(retrieveNode))
				.addNode("answer", node_async(answerNode))
				.addNode("check", node_async(checkNode))
				.addEdge(START, "classify")
				.addConditionalEdges("classify",
						edge_async(state -> state.<String>value("route").filter("retrieve"::equals).orElse("answer")),
						Map.of("retrieve", "retrieve", "answer", "answer"))
				.addEdge("retrieve", "answer")
				.addEdge("answer", "check")
				.addConditionalEdges("check",
						edge_async(state -> state.<String>value("route").filter("retry"::equals).orElse("done")),
						Map.of("retry", "retrieve", "done", END));

		return graph.compile();
	}

	@Bean
	public AgenticRagService agenticRagService(CompiledGraph agenticRagGraph) {
		return new AgenticRagService(agenticRagGraph);
	}

	/**
	 * Entry agent for Spring AI Alibaba Studio: exposes the agentic RAG graph as a
	 * chat agent whose single tool is the graph itself.
	 */
	@Bean
	public ReactAgent agenticRagAgent(ChatModel chatModel, AgenticRagService agenticRagService) {
		return ReactAgent.builder()
				.name("agentic_rag")
				.description("Answers questions about the product knowledge base using agentic RAG: decides whether to retrieve, and iterates until the answer is complete.")
				.systemPrompt(AGENT_PROMPT)
				.model(chatModel)
				.methodTools(new AgenticRagTools(agenticRagService))
				.inputType(String.class)
				.build();
	}
}
