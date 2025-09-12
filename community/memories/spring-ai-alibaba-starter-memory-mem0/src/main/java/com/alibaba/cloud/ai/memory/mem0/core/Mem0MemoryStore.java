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
package com.alibaba.cloud.ai.memory.mem0.core;

import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerRequest;
import com.alibaba.cloud.ai.memory.mem0.model.Mem0ServerResp;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.util.JacksonUtils;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.alibaba.cloud.ai.memory.mem0.advisor.Mem0ChatMemoryAdvisor.*;

/**
 * @author miaoyumeng
 * @since 2025/06/24 14:28
 */
public class Mem0MemoryStore implements InitializingBean, VectorStore {

	private final Mem0ServiceClient mem0Client;

	private final ObjectMapper objectMapper;

	private final Mem0FilterExpressionConverter mem0FilterExpressionConverter;

	protected Mem0MemoryStore(Mem0ServiceClient client) {
		this.mem0Client = client;
		this.mem0FilterExpressionConverter = new Mem0FilterExpressionConverter();
		this.objectMapper = JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules()).build();
	}

	public static Mem0MemoryStoreBuilder builder(Mem0ServiceClient client) {
		return new Mem0MemoryStoreBuilder(client);
	}

	public static final class Mem0MemoryStoreBuilder {

		private final Mem0ServiceClient client;

		public Mem0MemoryStoreBuilder(Mem0ServiceClient client) {
			this.client = client;
		}

		public Mem0MemoryStore build() {
			return new Mem0MemoryStore(client);
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public void add(List<Document> documents) {
		// TODO 将role相同的message合并
		List<Mem0ServerRequest.MemoryCreate> messages = documents.stream()
			.map(doc -> Mem0ServerRequest.MemoryCreate.builder()
				.messages(
						List.of(new Mem0ServerRequest.Message(doc.getMetadata().get("role").toString(), doc.getText())))
				.metadata(doc.getMetadata())
				.agentId(doc.getMetadata().containsKey(AGENT_ID) ? doc.getMetadata().get(AGENT_ID).toString() : null)
				.runId(doc.getMetadata().containsKey(RUN_ID) ? doc.getMetadata().get(RUN_ID).toString() : null)
				.userId(doc.getMetadata().containsKey(USER_ID) ? doc.getMetadata().get(USER_ID).toString() : null)
				.build())
			.toList();
		// TODO 增加异步方式
		messages.forEach(mem0Client::addMemory);
	}

	@Override
	public void delete(List<String> idList) {
		idList.forEach(mem0Client::deleteMemory);
	}

	@Override
	public void delete(Filter.Expression filterExpression) {
		throw new UnsupportedOperationException(
				"The Mem0 Server only supports delete operation that must include userId, agentId, or runId.");
	}

	@Override
	public List<Document> similaritySearch(String query) {
		throw new UnsupportedOperationException(
				"The Mem0 Server only supports queries that must include userId, agentId, or runId.");
	}

	@Override
	public List<Document> similaritySearch(SearchRequest request) {
		Mem0ServerRequest.SearchRequest search = (Mem0ServerRequest.SearchRequest) request;

		if (request.getFilterExpression() != null) {
			String jsonStr = this.mem0FilterExpressionConverter.convertExpression(request.getFilterExpression());

			Map<String, Object> filtersMap = null;
			if (jsonStr != null && !jsonStr.isEmpty()) {
				try {
					filtersMap = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
					});
				}
				catch (Exception e) {
					// If conversion fails, use an empty Map.
					filtersMap = new HashMap<>();
				}
				search.setFilters(filtersMap);
			}
		}

		Mem0ServerResp mem0ServerResp = mem0Client.searchMemories(search);
		List<Mem0ServerResp.Mem0Results> results = mem0ServerResp.getResults();
		List<Mem0ServerResp.Mem0Relation> relations = mem0ServerResp.getRelations();

		List<Document> documents = Stream.concat(results.stream().map(result -> {
			Map<String, Object> meta = new HashMap<>();
			meta.put("type", "results");
			meta.put("id", result.getId());
			meta.put("memory", result.getMemory());
			meta.put("hash", result.getHash());
			meta.put("created_at", result.getCreatedAt());
			meta.put("updated_at", result.getUpdatedAt());
			meta.put("user_id", result.getUserId());
			meta.put("agent_id", result.getAgentId());
			meta.put("run_id", result.getRunId());
			meta.put("score", result.getScore());
			meta.put("metadata", result.getMetadata());
			meta.put("role", result.getRole());

			if (result.getMetadata() != null)
				meta.putAll(result.getMetadata());
			return new Document(result.getId(), result.getMemory(), filterNullElement(meta));
		}), relations.stream().map(relation -> {
			Map<String, Object> meta = new HashMap<>();
			meta.put("type", "relations");
			meta.put("source", relation.getSource());
			meta.put("relationship", relation.getRelationship());
			meta.put("target", relation.getTarget());
			meta.put("destination", relation.getDestination());
			String text = relation.getSource() + " --[" + relation.getRelationship() + "]--> "
					+ (StringUtils.hasText(relation.getTarget()) ? relation.getTarget() : relation.getDestination());
			return new Document(text, filterNullElement(meta));
		})).toList();
		return documents;

	}

	private Map<String, Object> filterNullElement(Map<String, Object> map) {
		return map.entrySet()
			.stream()
			.filter(entry -> entry.getValue() != null && !"".equals(entry.getValue()))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

}
