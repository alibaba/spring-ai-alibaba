package com.alibaba.example.chatmemory.mem0;

import com.alibaba.example.chatmemory.config.Mem0ChatMemoryProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

/**
 * Mem0 API 客户端实现
 *
 * 直接调用 Mem0 REST API 接口 参考文档: http://localhost:8888/docs
 */
public class Mem0ServiceClient {

	private static final Logger logger = LoggerFactory.getLogger(Mem0ServiceClient.class);

	private final WebClient webClient;

	private final ObjectMapper objectMapper;

	private final Mem0ChatMemoryProperties config;

	private final ResourceLoader resourceLoader;

	// Mem0 API 端点
	private static final String CONFIGURE_ENDPOINT = "/configure";

	private static final String MEMORIES_ENDPOINT = "/memories";

	private static final String SEARCH_ENDPOINT = "/search";

	private static final String RESET_ENDPOINT = "/reset";

	/**
	 * 构造函数
	 */
	public Mem0ServiceClient(Mem0ChatMemoryProperties config, ResourceLoader resourceLoader) {
		this.config = config;
		this.resourceLoader = resourceLoader;
		this.objectMapper = new ObjectMapper();
		// json key序列化为_风格
		this.objectMapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
		// 忽略空值和空集合
		this.objectMapper.registerModule(new JavaTimeModule())
			.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY);

		// 创建 WebClient 连接到 Mem0 API
		this.webClient = WebClient.builder()
			.baseUrl(config.getClient().getBaseUrl())
			.defaultHeader("Content-Type", "application/json")
			.build();
	}

	/**
	 * 配置 Mem0
	 */
	public void configure(Mem0ChatMemoryProperties.Server config) {
		try {
			if (Objects.nonNull(config.getProject())) {
				config.getProject().setCustomInstructions(this.loadPrompt(config.getProject().getCustomInstructions()));
				config.getProject().setCustomCategories(this.loadPrompt(config.getProject().getCustomCategories()));
			}
			if (Objects.nonNull(config.getVectorStore())) {
				config.getGraphStore().setCustomPrompt(this.loadPrompt(config.getGraphStore().getCustomPrompt()));
			}
			config.setCustomFactExtractionPrompt(this.loadPrompt(config.getCustomFactExtractionPrompt()));
			config.setCustomUpdateMemoryPrompt(this.loadPrompt(config.getCustomUpdateMemoryPrompt()));

			String requestJson = objectMapper.writeValueAsString(config);
			String response = webClient.post()
				.uri(CONFIGURE_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestJson))
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(this.config.getClient().getTimeoutSeconds()))
				.block();
			if (StringUtils.hasText(response) && response.contains("successfully")) {
				logger.info("Mem0 configuration updated successfully");
			}
			else {
				logger.error("Failed to configure Mem0: {}", response);
				throw new RuntimeException("Failed to configure Mem0");
			}
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		catch (Exception e) {
			logger.error("Failed to configure Mem0: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to configure Mem0", e);
		}
	}

	/**
	 * 添加记忆
	 */
	public void addMemory(Mem0ServerRequest.MemoryCreate memoryCreate) {
		try {
			// 添加调试信息
			String requestJson = objectMapper.writeValueAsString(memoryCreate);

			String response = webClient.post()
				.uri(MEMORIES_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestJson))
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
				});
				logger.info("Successfully added memory with {} messages", memoryCreate.getMessages().size());
			}
		}
		catch (WebClientResponseException e) {
			String errorBody = e.getResponseBodyAsString();
			logger.error("HTTP error adding memory: {} - {}", e.getStatusCode(), errorBody, e);
			throw new RuntimeException("Failed to add memory: " + errorBody, e);
		}
		catch (Exception e) {
			logger.error("UNKNOW error adding memory: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to add memory", e);
		}

	}

	/**
	 * 获取所有记忆
	 */
	public Mem0ServerResp getAllMemories(String userId, String runId, String agentId) {
		try {
			String response = webClient.get().uri(uriBuilder -> {
				uriBuilder.path(MEMORIES_ENDPOINT);
				if (userId != null)
					uriBuilder.queryParam("user_id", userId);
				if (runId != null)
					uriBuilder.queryParam("run_id", runId);
				if (agentId != null)
					uriBuilder.queryParam("agent_id", agentId);
				return uriBuilder.build();
			})
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				// Mem0 服务返回 {"results":[],"relations":[]} 格式
				return objectMapper.readValue(response, new TypeReference<Mem0ServerResp>() {
				});
			}
		}
		catch (Exception e) {
			logger.error("Failed to get memories: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to get memories", e);
		}

		return new Mem0ServerResp();
	}

	/**
	 * 获取单个记忆
	 */
	public Mem0ServerResp getMemory(String memoryId) {
		try {
			String response = webClient.get()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				Mem0ServerResp memory = objectMapper.readValue(response, Mem0ServerResp.class);
				logger.info("Retrieved memory: {}", memoryId);
				return memory;
			}
		}
		catch (Exception e) {
			logger.error("Failed to get memory {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to get memory " + memoryId, e);
		}

		return null;
	}

	/**
	 * 搜索记忆
	 */
	public Mem0ServerResp searchMemories(Mem0ServerRequest.SearchRequest searchRequest) {
		try {
			// SEARCH_ENDPOINT 要求query必须有值，所以做了一个回退机制
			if (!StringUtils.hasText(searchRequest.getQuery())) {
				return getAllMemories(searchRequest.getUserId(), searchRequest.getRunId(), searchRequest.getAgentId());
			}

			// 添加调试日志
			String requestJson = objectMapper.writeValueAsString(searchRequest);
			logger.info("Sending search request to Mem0: {}", requestJson);

			String response = webClient.post()
				.uri(SEARCH_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(requestJson))
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				logger.info("Received response from Mem0: " + response);
				// Mem0 服务返回 {"results":[],"relations":[]} 格式
				return objectMapper.readValue(response, new TypeReference<Mem0ServerResp>() {
				});

			}
		}
		catch (Exception e) {
			logger.error("Failed to search memories: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to search memories", e);
		}

		return new Mem0ServerResp();
	}

	/**
	 * 更新记忆
	 */
	public Map<String, Object> updateMemory(String memoryId, Map<String, Object> updatedMemory) {
		try {
			String response = webClient.put()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
				.bodyValue(updatedMemory)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.retry(config.getClient().getMaxRetryAttempts())
				.block();

			if (response != null) {
				Map<String, Object> result = objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
				});
				logger.info("Successfully updated memory: " + memoryId);
				return result;
			}
		}
		catch (Exception e) {
			logger.error("Failed to update memory {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to update memory", e);
		}

		return new HashMap<>();
	}

	/**
	 * 获取记忆历史
	 */
	public List<Map<String, Object>> getMemoryHistory(String memoryId) {
		try {
			String response = webClient.get()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}/history", memoryId)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			if (response != null) {
				// 尝试解析为对象，然后提取数组
				Map<String, Object> responseMap = objectMapper.readValue(response,
						new TypeReference<Map<String, Object>>() {
						});

				// 检查是否有 data 字段包含数组
				if (responseMap.containsKey("data")) {
					Object data = responseMap.get("data");
					if (data instanceof List) {
						List<Map<String, Object>> history = objectMapper.convertValue(data,
								new TypeReference<List<Map<String, Object>>>() {
								});
						return history;
					}
				}

				// 如果没有 data 字段，尝试直接解析为数组
				try {
					List<Map<String, Object>> history = objectMapper.readValue(response,
							new TypeReference<List<Map<String, Object>>>() {
							});
					logger.info("Retrieved history for memory: {}", memoryId);
					return history;
				}
				catch (Exception e) {
					logger.error("Failed to parse history response as array, trying as object: {}", e.getMessage());
				}

				// 如果都失败了，返回空列表
				logger.warn("Could not parse memory history from response: {}", response);
				return new ArrayList<>();
			}
		}
		catch (Exception e) {
			logger.error("Failed to get memory history {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to get memory history", e);
		}

		return new ArrayList<>();
	}

	/**
	 * 删除单个记忆
	 */
	public void deleteMemory(String memoryId) {
		try {
			webClient.delete()
				.uri(MEMORIES_ENDPOINT + "/{memoryId}", memoryId)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			logger.info("Successfully deleted memory: {}", memoryId);
		}
		catch (Exception e) {
			logger.error("Failed to delete memory {}: {}", memoryId, e.getMessage(), e);
			throw new RuntimeException("Failed to delete memory", e);
		}
	}

	/**
	 * 删除所有记忆
	 */
	public void deleteAllMemories(String userId, String runId, String agentId) {
		try {
			webClient.delete().uri(uriBuilder -> {
				uriBuilder.path(MEMORIES_ENDPOINT);
				if (userId != null)
					uriBuilder.queryParam("user_id", userId);
				if (runId != null)
					uriBuilder.queryParam("run_id", runId);
				if (agentId != null)
					uriBuilder.queryParam("agent_id", agentId);
				return uriBuilder.build();
			})
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			logger.info("Successfully deleted all memories");
		}
		catch (Exception e) {
			logger.error("Failed to delete all memories: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to delete all memories", e);
		}
	}

	/**
	 * 重置所有记忆
	 */
	public void resetAllMemories() {
		try {
			webClient.post()
				.uri(RESET_ENDPOINT)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(config.getClient().getTimeoutSeconds()))
				.block();

			logger.info("Successfully reset all memories");
		}
		catch (Exception e) {
			logger.error("Failed to reset all memories: " + e.getMessage(), e);
			throw new RuntimeException("Failed to reset all memories", e);
		}
	}

	public String loadPrompt(String classPath) throws Exception {
		if (StringUtils.hasText(classPath)) {
			Resource resource = resourceLoader.getResource(classPath);
			if (!resource.exists()) {
				throw new IllegalArgumentException("Prompt resource not found: " + classPath);
			}
			// 读取文件内容为字符串
			return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
		}
		return null;
	}

}
