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

import java.util.HashMap;
import java.util.Map;

/**
 * @author yingzi
 * @since 2025/9/14
 */

public class Mem0Server {

	private String version;

	private VectorStore vectorStore;

	private GraphStore graphStore;

	private Llm llm;

	private Embedder embedder;

	private String historyDbPath;

	private Project project;

	private String customFactExtractionPrompt;

	private String customUpdateMemoryPrompt;

	// 私有构造函数，防止直接实例化
	private Mem0Server() {
	}

	private Mem0Server(Mem0Server server) {
		this.version = server.version;
		this.vectorStore = server.vectorStore;
		this.graphStore = server.graphStore;
		this.llm = server.llm;
		this.embedder = server.embedder;
		this.historyDbPath = server.historyDbPath;
		this.project = server.project;
		this.customFactExtractionPrompt = server.customFactExtractionPrompt;
		this.customUpdateMemoryPrompt = server.customUpdateMemoryPrompt;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private final Mem0Server server = new Mem0Server();

		private Builder() {
		}

		public Builder version(String version) {
			server.version = version;
			return this;
		}

		public Builder vectorStore(VectorStore vectorStore) {
			server.vectorStore = vectorStore;
			return this;
		}

		public Builder graphStore(GraphStore graphStore) {
			server.graphStore = graphStore;
			return this;
		}

		public Builder llm(Llm llm) {
			server.llm = llm;
			return this;
		}

		public Builder embedder(Embedder embedder) {
			server.embedder = embedder;
			return this;
		}

		public Builder historyDbPath(String historyDbPath) {
			server.historyDbPath = historyDbPath;
			return this;
		}

		public Builder project(Project project) {
			server.project = project;
			return this;
		}

		public Builder customFactExtractionPrompt(String customFactExtractionPrompt) {
			server.customFactExtractionPrompt = customFactExtractionPrompt;
			return this;
		}

		public Builder customUpdateMemoryPrompt(String customUpdateMemoryPrompt) {
			server.customUpdateMemoryPrompt = customUpdateMemoryPrompt;
			return this;
		}

		public Mem0Server build() {
			return new Mem0Server(server);
		}

	}

	public static class Project {

		private String customCategories;

		private String customInstructions;

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private final Project project = new Project();

			private Builder() {
			}

			public Builder customCategories(String customCategories) {
				project.customCategories = customCategories;
				return this;
			}

			public Builder customInstructions(String customInstructions) {
				project.customInstructions = customInstructions;
				return this;
			}

			public Project build() {
				return new Project(project);
			}

		}

		// 私有构造函数，防止直接实例化
		private Project() {
		}

		// 私有构造函数，用于复制现有实例
		private Project(Project project) {
			this.customCategories = project.customCategories;
			this.customInstructions = project.customInstructions;
		}

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
		 * "OpenSearchConfig", "supabase": "SupabaseConfig", "weaviate": "WeaviateConfig",
		 * "faiss": "FAISSConfig", "langchain": "LangchainConfig",
		 */
		private Map<String, String> config;

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private final VectorStore vectorStore = new VectorStore();

			private Builder() {
			}

			public Builder provider(String provider) {
				vectorStore.provider = provider;
				return this;
			}

			public Builder config(Map<String, String> config) {
				vectorStore.config = config;
				return this;
			}

			public VectorStore build() {
				return new VectorStore(vectorStore);
			}

		}

		// 私有构造函数，防止直接实例化
		private VectorStore() {
		}

		// 私有构造函数，用于复制现有实例
		private VectorStore(VectorStore vectorStore) {
			this.provider = vectorStore.provider;
			this.config = vectorStore.config;
		}

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

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private final GraphStore graphStore = new GraphStore();

			private Builder() {
			}

			public Builder provider(String provider) {
				graphStore.provider = provider;
				return this;
			}

			public Builder config(GraphStoreConfig config) {
				graphStore.config = config;
				return this;
			}

			public Builder llm(Llm llm) {
				graphStore.llm = llm;
				return this;
			}

			public Builder customPrompt(String customPrompt) {
				graphStore.customPrompt = customPrompt;
				return this;
			}

			public GraphStore build() {
				return new GraphStore(graphStore);
			}

		}

		// 私有构造函数，防止直接实例化
		private GraphStore() {
		}

		// 私有构造函数，用于复制现有实例
		private GraphStore(GraphStore graphStore) {
			this.provider = graphStore.provider;
			this.config = graphStore.config;
			this.llm = graphStore.llm;
			this.customPrompt = graphStore.customPrompt;
		}

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

			public static Builder builder() {
				return new Builder();
			}

			public static class Builder {

				private final GraphStoreConfig config = new GraphStoreConfig();

				private Builder() {
				}

				public Builder url(String url) {
					config.url = url;
					return this;
				}

				public Builder username(String username) {
					config.username = username;
					return this;
				}

				public Builder password(String password) {
					config.password = password;
					return this;
				}

				public Builder database(String database) {
					config.database = database;
					return this;
				}

				public Builder baseLabel(Boolean baseLabel) {
					config.baseLabel = baseLabel;
					return this;
				}

				public GraphStoreConfig build() {
					return new GraphStoreConfig(config);
				}

			}

			// 私有构造函数，防止直接实例化
			private GraphStoreConfig() {
			}

			// 私有构造函数，用于复制现有实例
			private GraphStoreConfig(GraphStoreConfig config) {
				this.url = config.url;
				this.username = config.username;
				this.password = config.password;
				this.database = config.database;
				this.baseLabel = config.baseLabel;
			}

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

			public String getDatabase() {
				return database;
			}

			public void setDatabase(String database) {
				this.database = database;
			}

			public Boolean getBaseLabel() {
				return baseLabel;
			}

			public void setBaseLabel(Boolean baseLabel) {
				this.baseLabel = baseLabel;
			}

		}

	}

	public static class Llm {

		private String provider;

		private LlmConfig config;

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private Llm llm = new Llm();

			private Builder() {
			}

			public Builder provider(String provider) {
				llm.provider = provider;
				return this;
			}

			public Builder config(LlmConfig config) {
				llm.config = config;
				return this;
			}

			public Llm build() {
				return new Llm(llm);
			}

		}

		// 私有构造函数，防止直接实例化
		private Llm() {
		}

		// 私有构造函数，用于复制现有实例
		private Llm(Llm llm) {
			this.provider = llm.provider;
			this.config = llm.config;
		}

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

			public static Builder builder() {
				return new Builder();
			}

			public static class Builder {

				private final LlmConfig config = new LlmConfig();

				private Builder() {
				}

				public Builder apiKey(String apiKey) {
					config.apiKey = apiKey;
					return this;
				}

				public Builder temperature(double temperature) {
					config.temperature = temperature;
					return this;
				}

				public Builder model(String model) {
					config.model = model;
					return this;
				}

				public Builder openaiBaseUrl(String openaiBaseUrl) {
					config.openaiBaseUrl = openaiBaseUrl;
					return this;
				}

				public LlmConfig build() {
					return new LlmConfig(config);
				}

			}

			// 私有构造函数，防止直接实例化
			private LlmConfig() {
			}

			// 私有构造函数，用于复制现有实例
			private LlmConfig(LlmConfig config) {
				this.apiKey = config.apiKey;
				this.temperature = config.temperature;
				this.model = config.model;
				this.openaiBaseUrl = config.openaiBaseUrl;
			}

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

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private final Embedder embedder = new Embedder();

			private Builder() {
			}

			public Builder provider(String provider) {
				embedder.provider = provider;
				return this;
			}

			public Builder config(EmbedderConfig config) {
				embedder.config = config;
				return this;
			}

			public Embedder build() {
				return new Embedder(embedder);
			}

		}

		// 私有构造函数，防止直接实例化
		private Embedder() {
		}

		// 私有构造函数，用于复制现有实例
		private Embedder(Embedder embedder) {
			this.provider = embedder.provider;
			this.config = embedder.config;
		}

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

			public static Builder builder() {
				return new Builder();
			}

			public static class Builder {

				private final EmbedderConfig config = new EmbedderConfig();

				private Builder() {
				}

				public Builder apiKey(String apiKey) {
					config.apiKey = apiKey;
					return this;
				}

				public Builder model(String model) {
					config.model = model;
					return this;
				}

				public Builder openaiBaseUrl(String openaiBaseUrl) {
					config.openaiBaseUrl = openaiBaseUrl;
					return this;
				}

				public EmbedderConfig build() {
					return new EmbedderConfig(config);
				}

			}

			// 私有构造函数，防止直接实例化
			private EmbedderConfig() {
			}

			// 私有构造函数，用于复制现有实例
			private EmbedderConfig(EmbedderConfig config) {
				this.apiKey = config.apiKey;
				this.model = config.model;
				this.openaiBaseUrl = config.openaiBaseUrl;
			}

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
