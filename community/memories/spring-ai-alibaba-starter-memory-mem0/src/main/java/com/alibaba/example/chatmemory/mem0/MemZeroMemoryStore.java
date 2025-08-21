package com.alibaba.example.chatmemory.mem0;

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

import static com.alibaba.example.chatmemory.mem0.MemZeroChatMemoryAdvisor.*;

/**
 * @author miaoyumeng
 * @date 2025/06/24 14:28
 * @description TODO
 */
public class MemZeroMemoryStore implements InitializingBean, VectorStore {

	private final MemZeroServiceClient mem0Client;

	private final ObjectMapper objectMapper;

	private final MemZeroFilterExpressionConverter mem0FilterExpressionConverter;

	protected MemZeroMemoryStore(MemZeroServiceClient client) {
		this.mem0Client = client;
		this.mem0FilterExpressionConverter = new MemZeroFilterExpressionConverter();
		this.objectMapper = JsonMapper.builder().addModules(JacksonUtils.instantiateAvailableModules()).build();
	}

	public static MemZeroMemoryStoreBuilder builder(MemZeroServiceClient client) {
		return new MemZeroMemoryStoreBuilder(client);
	}

	public static final class MemZeroMemoryStoreBuilder {

		private final MemZeroServiceClient client;

		public MemZeroMemoryStoreBuilder(MemZeroServiceClient client) {
			this.client = client;
		}

		public MemZeroMemoryStore build() {
			return new MemZeroMemoryStore(client);
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {

	}

	@Override
	public void add(List<Document> documents) {
		// TODO 将role相同的message合并
		List<MemZeroServerRequest.MemoryCreate> messages = documents.stream()
			.map(doc -> MemZeroServerRequest.MemoryCreate.builder()
				.messages(List
					.of(new MemZeroServerRequest.Message(doc.getMetadata().get("role").toString(), doc.getText())))
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
		MemZeroServerRequest.SearchRequest search = (MemZeroServerRequest.SearchRequest) request;

		if (request.getFilterExpression() != null) {
			String jsonStr = this.mem0FilterExpressionConverter.convertExpression(request.getFilterExpression());

			Map<String, Object> filtersMap = null;
			if (jsonStr != null && !jsonStr.isEmpty()) {
				try {
					filtersMap = objectMapper.readValue(jsonStr, new TypeReference<Map<String, Object>>() {
					});
				}
				catch (Exception e) {
					// 如果转换失败，使用空Map
					filtersMap = new HashMap<>();
				}
				search.setFilters(filtersMap);
			}
		}

		MemZeroServerResp memZeroServerResp = mem0Client.searchMemories(search);
		List<MemZeroServerResp.MemZeroResults> results = memZeroServerResp.getResults();
		List<MemZeroServerResp.MemZeroRelation> relations = memZeroServerResp.getRelations();

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
