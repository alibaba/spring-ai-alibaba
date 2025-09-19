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

	private Mem0ChatMemoryProperties(Builder builder) {
		this.client = builder.client;
		this.server = builder.server;
	}

	public Client getClient() {
		return client;
	}

	public Server getServer() {
		return server;
	}

	public static class Builder {
		private Client client;
		private Server server;

		public Builder client(Client client) {
			this.client = client;
			return this;
		}

		public Builder server(Server server) {
			this.server = server;
			return this;
		}

		public Mem0ChatMemoryProperties build() {
			return new Mem0ChatMemoryProperties(this);
		}
	}

	public static class Client {
		private String baseUrl;
		private boolean enableCache;
		private int timeoutSeconds;
		private int maxRetryAttempts;

		private Client(Builder builder) {
			this.baseUrl = builder.baseUrl;
			this.enableCache = builder.enableCache;
			this.timeoutSeconds = builder.timeoutSeconds;
			this.maxRetryAttempts = builder.maxRetryAttempts;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public boolean isEnableCache() {
			return enableCache;
		}

		public int getTimeoutSeconds() {
			return timeoutSeconds;
		}

		public int getMaxRetryAttempts() {
			return maxRetryAttempts;
		}

		public static class Builder {
			private String baseUrl = "http://localhost:8888";
			private boolean enableCache = true;
			private int timeoutSeconds = 30;
			private int maxRetryAttempts = 3;

			public Builder baseUrl(String baseUrl) {
				this.baseUrl = baseUrl;
				return this;
			}

			public Builder enableCache(boolean enableCache) {
				this.enableCache = enableCache;
				return this;
			}

			public Builder timeoutSeconds(int timeoutSeconds) {
				this.timeoutSeconds = timeoutSeconds;
				return this;
			}

			public Builder maxRetryAttempts(int maxRetryAttempts) {
				this.maxRetryAttempts = maxRetryAttempts;
				return this;
			}

			public Client build() {
				return new Client(this);
			}
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

		private Server(Builder builder) {
			this.version = builder.version;
			this.vectorStore = builder.vectorStore;
			this.graphStore = builder.graphStore;
			this.llm = builder.llm;
			this.embedder = builder.embedder;
			this.historyDbPath = builder.historyDbPath;
			this.project = builder.project;
			this.customFactExtractionPrompt = builder.customFactExtractionPrompt;
			this.customUpdateMemoryPrompt = builder.customUpdateMemoryPrompt;
		}

		public String getVersion() {
			return version;
		}

		public VectorStore getVectorStore() {
			return vectorStore;
		}

		public GraphStore getGraphStore() {
			return graphStore;
		}

		public Llm getLlm() {
			return llm;
		}

		public Embedder getEmbedder() {
			return embedder;
		}

		public String getHistoryDbPath() {
			return historyDbPath;
		}

		public Project getProject() {
			return project;
		}

		public String getCustomFactExtractionPrompt() {
			return customFactExtractionPrompt;
		}

		public String getCustomUpdateMemoryPrompt() {
			return customUpdateMemoryPrompt;
		}

		public void setCustomFactExtractionPrompt(String prompt) {
			this.customFactExtractionPrompt = prompt;
		}

		public void setCustomUpdateMemoryPrompt(String prompt) {
			this.customUpdateMemoryPrompt = prompt;
		}

		public static class Builder {
			private String version;
			private VectorStore vectorStore;
			private GraphStore graphStore;
			private Llm llm;
			private Embedder embedder;
			private String historyDbPath;
			private Project project;
			private String customFactExtractionPrompt;
			private String customUpdateMemoryPrompt;

			public Builder version(String version) {
				this.version = version;
				return this;
			}

			public Builder vectorStore(VectorStore vectorStore) {
				this.vectorStore = vectorStore;
				return this;
			}

			public Builder graphStore(GraphStore graphStore) {
				this.graphStore = graphStore;
				return this;
			}

			public Builder llm(Llm llm) {
				this.llm = llm;
				return this;
			}

			public Builder embedder(Embedder embedder) {
				this.embedder = embedder;
				return this;
			}

			public Builder historyDbPath(String historyDbPath) {
				this.historyDbPath = historyDbPath;
				return this;
			}

			public Builder project(Project project) {
				this.project = project;
				return this;
			}

			public Builder customFactExtractionPrompt(String prompt) {
				this.customFactExtractionPrompt = prompt;
				return this;
			}

			public Builder customUpdateMemoryPrompt(String prompt) {
				this.customUpdateMemoryPrompt = prompt;
				return this;
			}

			public Server build() {
				return new Server(this);
			}
		}

		public static class Project {
			private String customCategories;
			private String customInstructions;

			private Project(Builder builder) {
				this.customCategories = builder.customCategories;
				this.customInstructions = builder.customInstructions;
			}

			public String getCustomCategories() {
				return customCategories;
			}

			public String getCustomInstructions() {
				return customInstructions;
			}

			public void setCustomCategories(String customCategories) {
				this.customCategories = customCategories;
			}

			public void setCustomInstructions(String customInstructions) {
				this.customInstructions = customInstructions;
			}

			public static class Builder {
				private String customCategories;
				private String customInstructions;

				public Builder customCategories(String customCategories) {
					this.customCategories = customCategories;
					return this;
				}

				public Builder customInstructions(String customInstructions) {
					this.customInstructions = customInstructions;
					return this;
				}

				public Project build() {
					return new Project(this);
				}
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

			private VectorStore(Builder builder) {
				this.provider = builder.provider;
				this.config = builder.config;
			}

			public String getProvider() {
				return provider;
			}

			public Map<String, String> getConfig() {
				Map<String, String> result = new HashMap<>();
				for (Map.Entry<String, String> entry : config.entrySet()) {
					String key = entry.getKey().replace("-", "_");
					result.put(key, entry.getValue());
				}
				return result;
			}

			public static class Builder {
				private String provider;
				private Map<String, String> config = new HashMap<>();

				public Builder provider(String provider) {
					this.provider = provider;
					return this;
				}

				public Builder config(Map<String, String> config) {
					if (config != null) {
						this.config = new HashMap<>(config);
					}
					return this;
				}

				public VectorStore build() {
					return new VectorStore(this);
				}
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

			private GraphStore(Builder builder) {
				this.provider = builder.provider;
				this.config = builder.config;
				this.llm = builder.llm;
				this.customPrompt = builder.customPrompt;
			}

			public String getProvider() {
				return provider;
			}

			public GraphStoreConfig getConfig() {
				return config;
			}

			public Llm getLlm() {
				return llm;
			}

			public String getCustomPrompt() {
				return customPrompt;
			}

			public static class Builder {
				private String provider;
				private GraphStoreConfig config;
				private Llm llm;
				private String customPrompt;

				public Builder provider(String provider) {
					this.provider = provider;
					return this;
				}

				public Builder config(GraphStoreConfig config) {
					this.config = config;
					return this;
				}

				public Builder llm(Llm llm) {
					this.llm = llm;
					return this;
				}

				public Builder customPrompt(String customPrompt) {
					this.customPrompt = customPrompt;
					return this;
				}

				public GraphStore build() {
					return new GraphStore(this);
				}
			}

			public static class GraphStoreConfig {

				private String url;

				private String username;

				private String password;

				// Neo4j supports the following two items:
				private String database;

				private Boolean baseLabel;

				private GraphStoreConfig(Builder builder) {
					this.url = builder.url;
					this.username = builder.username;
					this.password = builder.password;
					this.database = builder.database;
					this.baseLabel = builder.baseLabel;
				}

				public String getUrl() {
					return url;
				}

				public String getUsername() {
					return username;
				}

				public String getPassword() {
					return password;
				}

				public String getDatabase() {
					return database;
				}

				public Boolean getBaseLabel() {
					return baseLabel;
				}

				public static class Builder {
					private String url;
					private String username;
					private String password;
					private String database;
					private Boolean baseLabel;

					public Builder url(String url) {
						this.url = url;
						return this;
					}

					public Builder username(String username) {
						this.username = username;
						return this;
					}

					public Builder password(String password) {
						this.password = password;
						return this;
					}

					public Builder database(String database) {
						this.database = database;
						return this;
					}

					public Builder baseLabel(Boolean baseLabel) {
						this.baseLabel = baseLabel;
						return this;
					}

					public GraphStoreConfig build() {
						return new GraphStoreConfig(this);
					}
				}
			}
		}

		public static class Llm {
			private String provider;
			private LlmConfig config;

			private Llm(Builder builder) {
				this.provider = builder.provider;
				this.config = builder.config;
			}

			public String getProvider() {
				return provider;
			}

			public LlmConfig getConfig() {
				return config;
			}

			public static class Builder {
				private String provider;
				private LlmConfig config;

				public Builder provider(String provider) {
					this.provider = provider;
					return this;
				}

				public Builder config(LlmConfig config) {
					this.config = config;
					return this;
				}

				public Llm build() {
					return new Llm(this);
				}
			}

			public static class LlmConfig {
				private String apiKey;
				private double temperature;
				private String model;
				private String openaiBaseUrl;

				private LlmConfig(Builder builder) {
					this.apiKey = builder.apiKey;
					this.temperature = builder.temperature;
					this.model = builder.model;
					this.openaiBaseUrl = builder.openaiBaseUrl;
				}

				public String getApiKey() {
					return apiKey;
				}

				public double getTemperature() {
					return temperature;
				}

				public String getModel() {
					return model;
				}

				public String getOpenaiBaseUrl() {
					return openaiBaseUrl;
				}

				public static class Builder {
					private String apiKey;
					private double temperature = 0.7;
					private String model = "";
					private String openaiBaseUrl = "";

					public Builder apiKey(String apiKey) {
						this.apiKey = apiKey;
						return this;
					}

					public Builder temperature(double temperature) {
						this.temperature = temperature;
						return this;
					}

					public Builder model(String model) {
						this.model = model;
						return this;
					}

					public Builder openaiBaseUrl(String openaiBaseUrl) {
						this.openaiBaseUrl = openaiBaseUrl;
						return this;
					}

					public LlmConfig build() {
						return new LlmConfig(this);
					}
				}
			}
		}

		public static class Embedder {
			private String provider;
			private EmbedderConfig config;

			private Embedder(Builder builder) {
				this.provider = builder.provider;
				this.config = builder.config;
			}

			public String getProvider() {
				return provider;
			}

			public EmbedderConfig getConfig() {
				return config;
			}

			public static class Builder {
				private String provider;
				private EmbedderConfig config;

				public Builder provider(String provider) {
					this.provider = provider;
					return this;
				}

				public Builder config(EmbedderConfig config) {
					this.config = config;
					return this;
				}

				public Embedder build() {
					return new Embedder(this);
				}
			}

			public static class EmbedderConfig {
				private String apiKey;
				private String model;
				private String openaiBaseUrl;

				private EmbedderConfig(Builder builder) {
					this.apiKey = builder.apiKey;
					this.model = builder.model;
					this.openaiBaseUrl = builder.openaiBaseUrl;
				}

				public String getApiKey() {
					return apiKey;
				}

				public String getModel() {
					return model;
				}

				public String getOpenaiBaseUrl() {
					return openaiBaseUrl;
				}

				public static class Builder {
					private String apiKey;
					private String model;
					private String openaiBaseUrl;

					public Builder apiKey(String apiKey) {
						this.apiKey = apiKey;
						return this;
					}

					public Builder model(String model) {
						this.model = model;
						return this;
					}

					public Builder openaiBaseUrl(String openaiBaseUrl) {
						this.openaiBaseUrl = openaiBaseUrl;
						return this;
					}

					public EmbedderConfig build() {
						return new EmbedderConfig(this);
					}
				}
			}
		}
	}
}
