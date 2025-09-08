/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.config.rag;

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for RAG (Retrieval Augmented Generation) functionality.
 *
 * @author hupei
 */
@ConfigurationProperties(prefix = RagProperties.RAG_PREFIX)
public class RagProperties {

	public static final String RAG_PREFIX = DeepResearchProperties.PREFIX + ".rag";

	/**
	 * Whether to enable the RAG feature
	 * The default value is false
	 */
	private boolean enabled = false;

	/**
	 * Vector store type
	 * Defaults to "simple"
	 * Other available options include "elasticsearch"
	 */
	private String vectorStoreType = "simple";

	/**
	 * Request timeout (in seconds). Default: 60
	 */
	private Integer timeoutSeconds = 60;

	/**
	 * Number of retry attempts. Default: 2
	 */
	private Integer retryTimes = 2;

	/**
	 * Simple vector store configuration
	 */
	private final Simple simple = new Simple();

	/**
	 * RAG enhancement configuration
	 */
	private final Pipeline pipeline = new Pipeline();

	/**
	 * Data loading configuration
	 */
	private final Data data = new Data();

	/**
	 * Elasticsearch configuration properties
	 */
	private final Elasticsearch elasticsearch = new Elasticsearch();

	/**
	 * Knowledge base configuration
	 */
	private final ProfessionalKnowledgeBases professionalKnowledgeBases = new ProfessionalKnowledgeBases();

	/**
	 * Text splitting configuration
	 */
	private final TextSplitter textSplitter = new TextSplitter();

	// Getters
	public Data getData() {
		return data;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getVectorStoreType() {
		return vectorStoreType;
	}

	public void setVectorStoreType(String vectorStoreType) {
		this.vectorStoreType = vectorStoreType;
	}

	public Simple getSimple() {
		return simple;
	}

	public Pipeline getPipeline() {
		return pipeline;
	}

	public Elasticsearch getElasticsearch() {
		return elasticsearch;
	}

	public ProfessionalKnowledgeBases getProfessionalKnowledgeBases() {
		return professionalKnowledgeBases;
	}

	public void setTimeoutSeconds(Integer timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
	}

	public Integer getTimeoutSeconds() {
		return timeoutSeconds;
	}

	public void setRetryTimes(Integer retryTimes) {
		this.retryTimes = retryTimes;
	}

	public Integer getRetryTimes() {
		return retryTimes;
	}

	public TextSplitter getTextSplitter() {
		return textSplitter;
	}

	/**
	 * Simple vector store configuration.
	 */
	public static class Simple {

		/**
		 * Storage path for the simple vector store. Default: "vector_store.json"
		 */
		private String storagePath = "vector_store.json";

		public String getStoragePath() {
			return storagePath;
		}

		public void setStoragePath(String storagePath) {
			this.storagePath = storagePath;
		}

	}

	/**
	 * RAG enhancement configuration
	 */
	public static class Pipeline {

		/**
		 * Whether to enable query expansion. Default: false
		 */
		private boolean queryExpansionEnabled = false;

		/**
		 * Whether to enable query translation. Default: false
		 */
		private boolean queryTranslationEnabled = false;

		/**
		 * Target language for query translation. Default: English
		 */
		private String queryTranslationLanguage = "English";

		/**
		 * Whether to enable first result selection in post-processing. Default: false
		 */
		private boolean postProcessingSelectFirstEnabled = false;

		/**
		 * Search configuration
		 */
		private int topK = 5;

		private double similarityThreshold = 0.7;

		private boolean deduplicationEnabled = true;

		/**
		 * Post-processing configuration
		 */
		private boolean rerankEnabled = true;

		private int rerankTopK = 10;

		private double rerankThreshold = 0.5;

		// Getters and Setters for Pipeline properties...
		public boolean isQueryExpansionEnabled() {
			return queryExpansionEnabled;
		}

		public void setQueryExpansionEnabled(boolean queryExpansionEnabled) {
			this.queryExpansionEnabled = queryExpansionEnabled;
		}

		public boolean isQueryTranslationEnabled() {
			return queryTranslationEnabled;
		}

		public void setQueryTranslationEnabled(boolean queryTranslationEnabled) {
			this.queryTranslationEnabled = queryTranslationEnabled;
		}

		public String getQueryTranslationLanguage() {
			return queryTranslationLanguage;
		}

		public void setQueryTranslationLanguage(String queryTranslationLanguage) {
			this.queryTranslationLanguage = queryTranslationLanguage;
		}

		public boolean isPostProcessingSelectFirstEnabled() {
			return postProcessingSelectFirstEnabled;
		}

		public void setPostProcessingSelectFirstEnabled(boolean postProcessingSelectFirstEnabled) {
			this.postProcessingSelectFirstEnabled = postProcessingSelectFirstEnabled;
		}

		public int getTopK() {
			return topK;
		}

		public void setTopK(int topK) {
			this.topK = topK;
		}

		public double getSimilarityThreshold() {
			return similarityThreshold;
		}

		public void setSimilarityThreshold(double similarityThreshold) {
			this.similarityThreshold = similarityThreshold;
		}

		public boolean isDeduplicationEnabled() {
			return deduplicationEnabled;
		}

		public void setDeduplicationEnabled(boolean deduplicationEnabled) {
			this.deduplicationEnabled = deduplicationEnabled;
		}

		public boolean isRerankEnabled() {
			return rerankEnabled;
		}

		public void setRerankEnabled(boolean rerankEnabled) {
			this.rerankEnabled = rerankEnabled;
		}

		public int getRerankTopK() {
			return rerankTopK;
		}

		public void setRerankTopK(int rerankTopK) {
			this.rerankTopK = rerankTopK;
		}

		public double getRerankThreshold() {
			return rerankThreshold;
		}

		public void setRerankThreshold(double rerankThreshold) {
			this.rerankThreshold = rerankThreshold;
		}

	}

	/**
	 * Elasticsearch configuration
	 */
	public static class Elasticsearch {

		/**
		 * Elasticsearch index name. Default: "spring-ai-rag-es-index"。
		 */
		private String indexName = "spring-ai-rag-es-index";

		/**
		 * Vector dimension. Default: 1536。
		 */
		private int dimensions = 1536;

		/**
		 * Elasticsearch connection URI, e.g., "http://localhost:9200"
		 */
		private String uris;

		/**
		 * Elasticsearch username
		 */
		private String username;

		/**
		 * Elasticsearch password
		 */
		private String password;

		/**
		 * Similarity function configuration
		 */
		private SimilarityFunction similarityFunction;

		/**
		 * Hybrid search configuration
		 */
		private final Hybrid hybrid = new Hybrid();

		// Getters and Setters
		public String getIndexName() {
			return indexName;
		}

		public void setIndexName(String indexName) {
			this.indexName = indexName;
		}

		public int getDimensions() {
			return dimensions;
		}

		public void setDimensions(int dimensions) {
			this.dimensions = dimensions;
		}

		public String getUris() {
			return uris;
		}

		public void setUris(String uris) {
			this.uris = uris;
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

		public SimilarityFunction getSimilarityFunction() {
			return similarityFunction;
		}

		public void setSimilarityFunction(SimilarityFunction similarityFunction) {
			this.similarityFunction = similarityFunction;
		}

		public Hybrid getHybrid() {
			return hybrid;
		}

		/**
		 * Hybrid search configuration.
		 * Hybrid search combines BM25 and KNN retrieval methods,
		 * using the Reciprocal Rank Fusion (RRF) algorithm for result fusion.
		 */
		public static class Hybrid {

			/**
			 * Whether to enable hybrid search. Default: false
			 */
			private boolean enabled = false;

			/**
			 * Weight for BM25 search
			 */
			private float bm25Boost = 1.0f;

			/**
			 * Weight for KNN search
			 */
			private float knnBoost = 1.0f;

			/**
			 * Window size for the RRF algorithm
			 */
			private int rrfWindowSize = 10;

			/**
			 * Ranking constant for the RRF algorithm
			 */
			private int rrfRankConstant = 60;

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public float getBm25Boost() {
				return bm25Boost;
			}

			public void setBm25Boost(float bm25Boost) {
				this.bm25Boost = bm25Boost;
			}

			public float getKnnBoost() {
				return knnBoost;
			}

			public void setKnnBoost(float knnBoost) {
				this.knnBoost = knnBoost;
			}

			public int getRrfWindowSize() {
				return rrfWindowSize;
			}

			public void setRrfWindowSize(int rrfWindowSize) {
				this.rrfWindowSize = rrfWindowSize;
			}

			public int getRrfRankConstant() {
				return rrfRankConstant;
			}

			public void setRrfRankConstant(int rrfRankConstant) {
				this.rrfRankConstant = rrfRankConstant;
			}

		}

	}

	/**
	 * Data loading configuration
	 */
	public static class Data {

		/**
		 * Data source location loaded at application startup. Supports ant-style patterns, e.g., "classpath:/data/*.md",
		 * "file:/path/to/docs/"
		 */
		private List<String> locations = new ArrayList<>();

		/**
		 * Scheduled folder scanning configuration
		 */
		private final Scan scan = new Scan();

		public List<String> getLocations() {
			return locations;
		}

		public void setLocations(List<String> locations) {
			this.locations = locations;
		}

		public Scan getScan() {
			return scan;
		}

		public static class Scan {

			/**
			 * Whether to enable scheduled scanning. Default: false
			 */
			private boolean enabled = false;

			/**
			 * Directory path to scan
			 */
			private String directory;

			/**
			 * Cron expression for the scheduled task. Defaults to running hourly
			 */
			private String cron = "0 0 * * * *";

			/**
			 * Archive directory for processed files
			 */
			private String archiveDirectory;

			// Getters and Setters
			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public String getDirectory() {
				return directory;
			}

			public void setDirectory(String directory) {
				this.directory = directory;
			}

			public String getCron() {
				return cron;
			}

			public void setCron(String cron) {
				this.cron = cron;
			}

			public String getArchiveDirectory() {
				return archiveDirectory;
			}

			public void setArchiveDirectory(String archiveDirectory) {
				this.archiveDirectory = archiveDirectory;
			}

		}

	}

	/**
	 * Knowledge base configuration
	 */
	public static class ProfessionalKnowledgeBases {

		/**
		 * Whether to enable expert knowledge base decision-making. Default: true
		 */
		private boolean decisionEnabled = true;

		/**
		 * List of expert knowledge bases
		 */
		private List<KnowledgeBase> knowledgeBases = new ArrayList<>();

		public boolean isDecisionEnabled() {
			return decisionEnabled;
		}

		public void setDecisionEnabled(boolean decisionEnabled) {
			this.decisionEnabled = decisionEnabled;
		}

		public List<KnowledgeBase> getKnowledgeBases() {
			return knowledgeBases;
		}

		public void setKnowledgeBases(List<KnowledgeBase> knowledgeBases) {
			this.knowledgeBases = knowledgeBases;
		}

		/**
		 * Individual expert knowledge base configuration
		 */
		public static class KnowledgeBase {

			/**
			 * ID of knowledgeBase
			 */
			private String id;

			/**
			 * Name of knowledgeBase
			 */
			private String name;

			/**
			 * Description of knowledgeBase, used by the large language model to determine if querying is needed
			 */
			private String description;

			/**
			 * Type of knowledgeBase: api, elasticsearch
			 */
			private String type = "api";

			/**
			 * API configuration
			 */
			private final Api api = new Api();

			/**
			 * Whether enable
			 */
			private boolean enabled = true;

			/**
			 * Priority (lower numerical values indicate higher priority)
			 */
			private int priority = 100;

			public String getId() {
				return id;
			}

			public void setId(String id) {
				this.id = id;
			}

			public String getName() {
				return name;
			}

			public void setName(String name) {
				this.name = name;
			}

			public String getDescription() {
				return description;
			}

			public void setDescription(String description) {
				this.description = description;
			}

			public String getType() {
				return type;
			}

			public void setType(String type) {
				this.type = type;
			}

			public Api getApi() {
				return api;
			}

			public boolean isEnabled() {
				return enabled;
			}

			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}

			public int getPriority() {
				return priority;
			}

			public void setPriority(int priority) {
				this.priority = priority;
			}

			/**
			 * API configuration
			 */
			public static class Api {

				/**
				 * API type: dashscope, custom
				 */
				private String provider = "dashscope";

				/**
				 * API URL
				 */
				private String url;

				/**
				 * API Key
				 */
				private String apiKey;

				/**
				 * Model name (applicable to DashScope and similar platforms)
				 */
				private String model;

				/**
				 * Request timeout duration (in milliseconds)
				 */
				private int timeoutMs = 30000;

				/**
				 * Maximum number of returned results
				 */
				private int maxResults = 5;

				public String getProvider() {
					return provider;
				}

				public void setProvider(String provider) {
					this.provider = provider;
				}

				public String getUrl() {
					return url;
				}

				public void setUrl(String url) {
					this.url = url;
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

				public int getTimeoutMs() {
					return timeoutMs;
				}

				public void setTimeoutMs(int timeoutMs) {
					this.timeoutMs = timeoutMs;
				}

				public int getMaxResults() {
					return maxResults;
				}

				public void setMaxResults(int maxResults) {
					this.maxResults = maxResults;
				}

			}

		}

	}

	/**
	 * Text splitting configuration
	 */
	public static class TextSplitter {

		/**
		 * Default chunk size (in tokens). Default: 800
		 */
		private int defaultChunkSize = 800;

		/**
		 * Chunk overlap size (in tokens). Default: 100
		 */
		private int overlap = 100;

		/**
		 * Minimum chunk size (in tokens). Default: 5
		 */
		private int minChunkSizeToSplit = 5;

		/**
		 * Maximum chunk size (in tokens). Default: 10000
		 */
		private int maxChunkSize = 10000;

		/**
		 * Whether to preserve separators. Default: true
		 */
		private boolean keepSeparator = true;

		/**
		 * Whether to enable debug mode. Default: false
		 */
		private boolean debugMode = false;

		public int getDefaultChunkSize() {
			return defaultChunkSize;
		}

		public void setDefaultChunkSize(int defaultChunkSize) {
			this.defaultChunkSize = defaultChunkSize;
		}

		public int getOverlap() {
			return overlap;
		}

		public void setOverlap(int overlap) {
			this.overlap = overlap;
		}

		public int getMinChunkSizeToSplit() {
			return minChunkSizeToSplit;
		}

		public void setMinChunkSizeToSplit(int minChunkSizeToSplit) {
			this.minChunkSizeToSplit = minChunkSizeToSplit;
		}

		public int getMaxChunkSize() {
			return maxChunkSize;
		}

		public void setMaxChunkSize(int maxChunkSize) {
			this.maxChunkSize = maxChunkSize;
		}

		public boolean isKeepSeparator() {
			return keepSeparator;
		}

		public void setKeepSeparator(boolean keepSeparator) {
			this.keepSeparator = keepSeparator;
		}

		public boolean isDebugMode() {
			return debugMode;
		}

		public void setDebugMode(boolean debugMode) {
			this.debugMode = debugMode;
		}

	}

}
