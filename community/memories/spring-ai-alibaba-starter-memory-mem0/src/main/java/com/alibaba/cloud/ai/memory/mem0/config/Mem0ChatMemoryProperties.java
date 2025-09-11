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
package com.alibaba.cloud.ai.memory.mem0.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = Mem0ChatMemoryProperties.MEM0_PREFIX)
public class Mem0ChatMemoryProperties {

	public static final String MEM0_PREFIX = "spring.ai.alibaba.mem0";

	private Client client;

	private Server server;

	public static class Client {

		private String baseUrl = "http://localhost:8888";

		private boolean enableCache = true;

		private int timeoutSeconds = 30;

		private int maxRetryAttempts = 3;

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}

		public boolean isEnableCache() {
			return enableCache;
		}

		public void setEnableCache(boolean enableCache) {
			this.enableCache = enableCache;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public void setTimeoutSeconds(int timeoutSeconds) {
			this.timeoutSeconds = timeoutSeconds;
		}

		public int getMaxRetryAttempts() {
			return maxRetryAttempts;
		}

		public void setMaxRetryAttempts(int maxRetryAttempts) {
			this.maxRetryAttempts = maxRetryAttempts;
		}

	}

	public static class Server {

		private String version;

		private VectorStore vectorStore;

		private GraphStore graphStore;

		private Llm llm;

		private Embedder embedder;

		private String historyDbPath;

		private Project project;

		private String customFactExtractionPrompt;

		private String customUpdateMemoryPrompt;

		public static class Project {

			private String customCategories;

			private String customInstructions;

			public String getCustomCategories() {
				return customCategories;
			}

			public void setCustomCategories(String customCategories) {
				this.customCategories = customCategories;
			}

			public String getCustomInstructions() {
				return customInstructions;
			}

			public void setCustomInstructions(String customInstructions) {
				this.customInstructions = customInstructions;
			}

		}

		public static class VectorStore {

			private String provider;

			/**
			 * The following vector databases are supported. For specific configurations,
			 * please refer to the official documentation or Mem0 source code. "qdrant":
			 * "QdrantConfig", "chroma": "ChromaDbConfig", "pgvector": "PGVectorConfig",
			 * "pinecone": "PineconeConfig", "mongodb": "MongoDBConfig", "milvus":
			 * "MilvusDBConfig", "baidu": "BaiduDBConfig", "upstash_vector":
			 * "UpstashVectorConfig", "azure_ai_search": "AzureAISearchConfig", "redis":
			 * "RedisDBConfig", "elasticsearch": "ElasticsearchConfig",
			 * "vertex_ai_vector_search": "GoogleMatchingEngineConfig", "opensearch":
			 * "OpenSearchConfig", "supabase": "SupabaseConfig", "weaviate":
			 * "WeaviateConfig", "faiss": "FAISSConfig", "langchain": "LangchainConfig",
			 */
			private Map<String, String> config;

			public String getProvider() {
				return provider;
			}

			public void setProvider(String provider) {
				this.provider = provider;
			}

			public Map<String, String> getConfig() {
				Map<String, String> result = new HashMap<>();
				for (Map.Entry<String, String> entry : config.entrySet()) {
					String key = entry.getKey().replace("-", "_");
					result.put(key, entry.getValue());
				}
				return result;
			}

			public void setConfig(Map<String, String> config) {
				this.config = config;
			}

		}

		public static class GraphStore {

			private String provider;

			private GraphStoreConfig config;

			private Llm llm;

			/*
			 * customPrompt: classpath:/prompts/system-message.st
			 */
			private String customPrompt;

			public String getProvider() {
				return provider;
			}

			public void setProvider(String provider) {
				this.provider = provider;
			}

			public GraphStoreConfig getConfig() {
				return config;
			}

			public void setConfig(GraphStoreConfig config) {
				this.config = config;
			}

			public Llm getLlm() {
				return llm;
			}

			public void setLlm(Llm llm) {
				this.llm = llm;
			}

			public String getCustomPrompt() {
				return customPrompt;
			}

			public void setCustomPrompt(String customPrompt) {
				this.customPrompt = customPrompt;
			}

			public static class GraphStoreConfig {

				private String url;

				private String username;

				private String password;

				// Neo4j supports the following two items:
				private String database;

				private Boolean baseLabel;

				public String getUrl() {
					return url;
				}

				public void setUrl(String url) {
					this.url = url;
				}

				public String getUsername() {
					return username;
				}

				public void setUsername(String username) {
					this.username = username;
				}

				public String getPassword() {
					return password;
				}

				public void setPassword(String password) {
					this.password = password;
				}

			}

		}

		public static class Llm {

			private String provider;

			private LlmConfig config;

			public String getProvider() {
				return provider;
			}

			public void setProvider(String provider) {
				this.provider = provider;
			}

			public LlmConfig getConfig() {
				return config;
			}

			public void setConfig(LlmConfig config) {
				this.config = config;
			}

			public static class LlmConfig {

				private String apiKey;

				private double temperature;

				private String model;

				private String openaiBaseUrl;

				public String getApiKey() {
					return apiKey;
				}

				public void setApiKey(String apiKey) {
					this.apiKey = apiKey;
				}

				public double getTemperature() {
					return temperature;
				}

				public void setTemperature(double temperature) {
					this.temperature = temperature;
				}

				public String getModel() {
					return model;
				}

				public void setModel(String model) {
					this.model = model;
				}

				public String getOpenaiBaseUrl() {
					return openaiBaseUrl;
				}

				public void setOpenaiBaseUrl(String openaiBaseUrl) {
					this.openaiBaseUrl = openaiBaseUrl;
				}

			}

		}

		public static class Embedder {

			private String provider;

			private EmbedderConfig config;

			public String getProvider() {
				return provider;
			}

			public void setProvider(String provider) {
				this.provider = provider;
			}

			public EmbedderConfig getConfig() {
				return config;
			}

			public void setConfig(EmbedderConfig config) {
				this.config = config;
			}

			public static class EmbedderConfig {

				private String apiKey;

				private String model;

				private String openaiBaseUrl;

				public String getApiKey() {
					return apiKey;
				}

				public void setApiKey(String apiKey) {
					this.apiKey = apiKey;
				}

				public String getModel() {
					return model;
				}

				public void setModel(String model) {
					this.model = model;
				}

				public String getOpenaiBaseUrl() {
					return openaiBaseUrl;
				}

				public void setOpenaiBaseUrl(String openaiBaseUrl) {
					this.openaiBaseUrl = openaiBaseUrl;
				}

			}

		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public VectorStore getVectorStore() {
			return vectorStore;
		}

		public void setVectorStore(VectorStore vectorStore) {
			this.vectorStore = vectorStore;
		}

		public GraphStore getGraphStore() {
			return graphStore;
		}

		public void setGraphStore(GraphStore graphStore) {
			this.graphStore = graphStore;
		}

		public Llm getLlm() {
			return llm;
		}

		public void setLlm(Llm llm) {
			this.llm = llm;
		}

		public Embedder getEmbedder() {
			return embedder;
		}

		public void setEmbedder(Embedder embedder) {
			this.embedder = embedder;
		}

		public String getHistoryDbPath() {
			return historyDbPath;
		}

		public void setHistoryDbPath(String historyDbPath) {
			this.historyDbPath = historyDbPath;
		}

		public Project getProject() {
			return project;
		}

		public void setProject(Project project) {
			this.project = project;
		}

		public String getCustomFactExtractionPrompt() {
			return customFactExtractionPrompt;
		}

		public void setCustomFactExtractionPrompt(String customFactExtractionPrompt) {
			this.customFactExtractionPrompt = customFactExtractionPrompt;
		}

		public String getCustomUpdateMemoryPrompt() {
			return customUpdateMemoryPrompt;
		}

		public void setCustomUpdateMemoryPrompt(String customUpdateMemoryPrompt) {
			this.customUpdateMemoryPrompt = customUpdateMemoryPrompt;
		}

	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Server getServer() {
		return server;
	}

	public void setServer(Server server) {
		this.server = server;
	}

}
