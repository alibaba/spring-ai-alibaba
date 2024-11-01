package com.alibaba.cloud.ai.memory.persistence;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.entity.ConversationMemoryForES;
import com.alibaba.cloud.ai.memory.handler.PersistenceHandler;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Title es persistent processing class.<br>
 * Description process the interaction of data with es.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class ElasticsearchPersistence implements PersistenceHandler {

	private static final Logger logger = LoggerFactory.getLogger(ElasticsearchPersistence.class);

	@Autowired
	private ElasticsearchClient elasticsearchClient;

	@Autowired
	private ChatMemoryProperties memoryProperties;

	private static final String INDEX_NAME = "conversation_memory";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public void saveMessage(String conversationId, List<ChatMessage> messages) {
		String documentId = conversationId + "_" + memoryProperties.getMemoryType();

		try {
			GetRequest getRequest = GetRequest.of(g -> g.index(INDEX_NAME).id(documentId));
			GetResponse<ConversationMemoryForES> getResponse = elasticsearchClient.get(getRequest,
					ConversationMemoryForES.class);
			if (getResponse.found() && Objects.nonNull(getResponse.source())) {
				ConversationMemoryForES existingMemory = getResponse.source();
				existingMemory.setContent(JSON.toJSONString(messages));
				existingMemory.setUpdatedAt(System.currentTimeMillis());
				UpdateRequest<ConversationMemoryForES, ConversationMemoryForES> updateRequest = UpdateRequest
					.of(u -> u.index(INDEX_NAME).id(documentId).doc(existingMemory).refresh(Refresh.True));
				elasticsearchClient.update(updateRequest, ConversationMemoryForES.class);

			}
			else {
				ConversationMemoryForES newMemory = ConversationMemoryForES.builder()
					.id(IdUtil.simpleUUID())
					.conversationId(conversationId)
					.memoryType(memoryProperties.getMemoryType())
					.content(objectMapper.writeValueAsString(messages))
					.createdAt(System.currentTimeMillis())
					.build();
				IndexRequest<ConversationMemoryForES> indexRequest = IndexRequest
					.of(i -> i.index(INDEX_NAME).id(documentId).document(newMemory).refresh(Refresh.True));
				elasticsearchClient.index(indexRequest);
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to save memory to Elasticsearch", e);
		}
	}

	@Override
	public List<ChatMessage> getMessages(String conversationId, int windowSize) {
		try {
			SearchRequest request = SearchRequest.of(s -> s.index(INDEX_NAME)
				.query(q -> q
					.term(t -> t.field("_id").value(conversationId + "_" + memoryProperties.getMemoryType()))));

			SearchResponse<ConversationMemoryForES> response = elasticsearchClient.search(request,
					ConversationMemoryForES.class);
			if (CollUtil.isEmpty(response.hits().hits())) {
				return CollUtil.newArrayList();
			}
			List<Hit<ConversationMemoryForES>> hits = response.hits().hits();
			ConversationMemoryForES history = hits.get(0).source();
			if (Objects.isNull(history) || StringUtils.isBlank(history.getContent())) {
				return CollUtil.newArrayList();
			}
			List<ChatMessage> messagesList = JSON.parseArray(history.getContent(), ChatMessage.class);
			if (windowSize > 0) {
				return messagesList.stream()
					.skip(Math.max(messagesList.size() - windowSize * 2, 0))
					.collect(Collectors.toList());
			}
			else {
				return messagesList;
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to retrieve memory from Elasticsearch", e);
		}
	}

	@Override
	public void updateHistory(String conversationId, List<ChatMessage> messages) {
		try {
			String messagesStr = this.objectMapper.writeValueAsString(messages);
			Map<String, JsonData> params = Map.of("messages", JsonData.of(messagesStr));
			UpdateByQueryRequest request = UpdateByQueryRequest.of(u -> u.index("conversation_memory")
				.query(q -> q.term(t -> t.field("_id")
					.value(conversationId + "_" + ElasticsearchPersistence.this.memoryProperties.getMemoryType())))
				.script(s -> s.inline(
						i -> i.source("ctx._source['content'] = params.messages").lang("painless").params(params))));
			this.elasticsearchClient.updateByQuery(request);
		}
		catch (IOException var5) {
			throw new RuntimeException("Failed to update memory from Elasticsearch", var5);
		}
	}

	@Override
	public void clearMessages(String conversationId) {
		try {
			UpdateByQueryRequest request = UpdateByQueryRequest.of(u -> u.index(INDEX_NAME)
				.query(q -> q.term(t -> t.field("_id").value(conversationId + "_" + memoryProperties.getMemoryType())))
				.script(s -> s.inline(i -> i.source("ctx._source['content'] = ''").lang("painless"))));

			elasticsearchClient.updateByQuery(request);
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to clear memory from Elasticsearch", e);
		}
	}

	@Override
	public void checkAndCreateTable() {
		try {
			if (!elasticsearchClient.indices().exists(b -> b.index(INDEX_NAME)).value()) {
				elasticsearchClient.indices()
					.create(c -> c.index(INDEX_NAME)
						.mappings(m -> m.properties("id", p -> p.keyword(x -> x))
							.properties("conversationId", p -> p.keyword(k -> k))
							.properties("content", p -> p.text(t -> t))
							.properties("memoryType", p -> p.text(i -> i))
							.properties("createdAt", p -> p.long_(d -> d))
							.properties("updatedAt", p -> p.long_(d -> d))));
				logger.info("index 'conversation_memory' created successfully.");
			}
			else {
				logger.info("index 'conversation_memory' already exists.");
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to check or create index in Elasticsearch", e);
		}
	}

}