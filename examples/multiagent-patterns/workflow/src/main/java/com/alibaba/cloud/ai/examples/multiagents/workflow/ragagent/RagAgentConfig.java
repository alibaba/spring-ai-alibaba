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
package com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent;

import com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.node.PrepareAgentNode;
import com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.node.RewriteNode;
import com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.node.RetrieveNode;
import com.alibaba.cloud.ai.examples.multiagents.workflow.ragagent.tools.RagAgentTools;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * RAG agent workflow: rewrite query → retrieve documents → agent with context.
 * Equivalent to custom RAG workflow in multiagents/custom.md.
 */
@Configuration
@ConditionalOnProperty(name = "workflow.rag.enabled", havingValue = "true")
public class RagAgentConfig {

	private static final String REWRITE_PROMPT = """
			Rewrite this query to retrieve relevant WNBA information.
			The knowledge base contains: team rosters, game results with scores, and player statistics (PPG, RPG, APG).
			Focus on specific player names, team names, or stat categories mentioned.
			Original query: %s
			Respond with only the rewritten query, nothing else.
			""";

	private static final String AGENT_PROMPT = """
			You are a WNBA stats assistant. Use the provided context to answer questions.
			Context:
			%s

			Question: %s

			Respond concisely. If the context doesn't contain the answer, say so.
			""";

	@Bean
	public VectorStore ragVectorStore(EmbeddingModel embeddingModel) {
		VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
		// Seed with sample WNBA data (from custom.md)
		List<Document> docs = List.of(
				new Document("New York Liberty 2024 roster: Breanna Stewart, Sabrina Ionescu, Jonquel Jones, Courtney Vandersloot.",
						Map.of("source", "rosters")),
				new Document("Las Vegas Aces 2024 roster: A'ja Wilson, Kelsey Plum, Jackie Young, Chelsea Gray.",
						Map.of("source", "rosters")),
				new Document("Indiana Fever 2024 roster: Caitlin Clark, Aliyah Boston, Kelsey Mitchell, NaLyssa Smith.",
						Map.of("source", "rosters")),
				new Document("2024 WNBA Finals: New York Liberty defeated Minnesota Lynx 3-2 to win the championship.",
						Map.of("source", "games")),
				new Document("June 15, 2024: Indiana Fever 85, Chicago Sky 79. Caitlin Clark had 23 points and 8 assists.",
						Map.of("source", "games")),
				new Document("August 20, 2024: Las Vegas Aces 92, Phoenix Mercury 84. A'ja Wilson scored 35 points.",
						Map.of("source", "games")),
				new Document("A'ja Wilson 2024 season stats: 26.9 PPG, 11.9 RPG, 2.6 BPG. Won MVP award.",
						Map.of("source", "stats")),
				new Document("Caitlin Clark 2024 rookie stats: 19.2 PPG, 8.4 APG, 5.7 RPG. Won Rookie of the Year.",
						Map.of("source", "stats")),
				new Document("Breanna Stewart 2024 stats: 20.4 PPG, 8.5 RPG, 3.5 APG.", Map.of("source", "stats")));
		vectorStore.add(docs);
		return vectorStore;
	}

	@Bean
	public ReactAgent ragAgent(ChatModel chatModel) {
		return ReactAgent.builder()
				.name("rag_agent")
				.model(chatModel)
				.instruction("You are a WNBA stats assistant. Answer questions using the context provided.")
				.methodTools(new RagAgentTools())
				.inputType(String.class)
				.build();
	}

	@Bean
	public CompiledGraph ragGraph(ChatModel chatModel, VectorStore ragVectorStore, ReactAgent ragAgent)
			throws GraphStateException {

		StateGraph graph = new StateGraph("rag_workflow", () -> {
			Map<String, KeyStrategy> strategies = new HashMap<>();
			strategies.put("question", new ReplaceStrategy());
			strategies.put("rewritten_query", new ReplaceStrategy());
			strategies.put("documents", new ReplaceStrategy());
			strategies.put("messages", new AppendStrategy(false));
			return strategies;
		});

		RewriteNode rewriteNode = new RewriteNode(chatModel, REWRITE_PROMPT);
		RetrieveNode retrieveNode = new RetrieveNode(ragVectorStore);
		PrepareAgentNode prepareAgentNode = new PrepareAgentNode();

		graph.addNode("rewrite", node_async(rewriteNode))
				.addNode("retrieve", node_async(retrieveNode))
				.addNode("prepare_agent", node_async(prepareAgentNode))
				.addNode("agent", ragAgent.asNode(false, false))
				.addEdge(START, "rewrite")
				.addEdge("rewrite", "retrieve")
				.addEdge("retrieve", "prepare_agent")
				.addEdge("prepare_agent", "agent")
				.addEdge("agent", END);

		return graph.compile();
	}

	@Bean
	public RagAgentService ragAgentService(CompiledGraph ragGraph) {
		return new RagAgentService(ragGraph);
	}

	/**
	 * Builds the agent prompt with context and question, then invokes the agent.
	 * The RetrieveNode produces "documents" and "question" in state.
	 * We need a bridge node that formats the prompt and invokes the agent.
	 * ReactAgent.asNode expects "messages" with the last user message.
	 * So we need a node that: reads documents + question, creates UserMessage with AGENT_PROMPT, adds to messages.
	 */
	public static String buildAgentPrompt(String context, String question) {
		return AGENT_PROMPT.formatted(context, question);
	}
}
