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
	 * 是否启用RAG功能，默认为false。
	 */
	private boolean enabled = false;

	/**
	 * 向量存储类型，默认为"simple"，可选值还包括"elasticsearch"。
	 */
	private String vectorStoreType = "simple";

	/**
	 * 请求超时时间，单位为秒，默认为60秒。
	 */
	private Integer timeoutSeconds = 60;

	/**
	 * 重试次数，默认为2次。
	 */
	private Integer retryTimes = 2;

	/**
	 * 简单向量存储配置。
	 */
	private final Simple simple = new Simple();

	/**
	 * RAG增强配置。
	 */
	private final Pipeline pipeline = new Pipeline();

	/**
	 * 数据加载相关的配置
	 */
	private final Data data = new Data();

	/**
	 * Elasticsearch配置属性
	 */
	private final Elasticsearch elasticsearch = new Elasticsearch();

	/**
	 * 专业知识库配置
	 */
	private final ProfessionalKnowledgeBases professionalKnowledgeBases = new ProfessionalKnowledgeBases();

	/**
	 * 文本分割配置
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
	 * 简单向量存储配置。
	 */
	public static class Simple {

		/**
		 * 简单向量存储的存储路径，默认为"vector_store.json"。
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
	 * RAG增强配置。
	 */
	public static class Pipeline {

		/**
		 * 是否启用查询扩展功能，默认为false。
		 */
		private boolean queryExpansionEnabled = false;

		/**
		 * 是否启用查询翻译功能，默认为false。
		 */
		private boolean queryTranslationEnabled = false;

		/**
		 * 查询翻译的目标语言，默认为"English"。
		 */
		private String queryTranslationLanguage = "English";

		/**
		 * 是否启用后处理选择第一个结果功能，默认为false。
		 */
		private boolean postProcessingSelectFirstEnabled = false;

		/**
		 * 搜索配置
		 */
		private int topK = 5;

		private double similarityThreshold = 0.7;

		private boolean deduplicationEnabled = true;

		/**
		 * 后处理配置
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
	 * Elasticsearch配置
	 */
	public static class Elasticsearch {

		/**
		 * Elasticsearch索引名称，默认为"spring-ai-rag-es-index"。
		 */
		private String indexName = "spring-ai-rag-es-index";

		/**
		 * 向量维度，默认为1536。
		 */
		private int dimensions = 1536;

		/**
		 * Elasticsearch连接URI，例如"http://localhost:9200"。
		 */
		private String uris;

		/**
		 * Elasticsearch用户名。
		 */
		private String username;

		/**
		 * Elasticsearch密码。
		 */
		private String password;

		/**
		 * 相似度函数配置。
		 */
		private SimilarityFunction similarityFunction;

		/**
		 * 混合搜索配置
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
		 * 混合搜索配置。 混合搜索结合了BM25和KNN搜索，使用RRF算法进行结果融合。
		 */
		public static class Hybrid {

			/**
			 * 是否启用混合搜索，默认为false。
			 */
			private boolean enabled = false;

			/**
			 * BM25搜索的权重。
			 */
			private float bm25Boost = 1.0f;

			/**
			 * KNN搜索的权重。
			 */
			private float knnBoost = 1.0f;

			/**
			 * RRF算法中的窗口大小。
			 */
			private int rrfWindowSize = 10;

			/**
			 * RRF算法中的排名常数。
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
	 * 数据加载相关的配置。
	 */
	public static class Data {

		/**
		 * 应用启动时加载的数据源位置. 支持ant-style patterns, e.g., "classpath:/data/*.md",
		 * "file:/path/to/docs/"
		 */
		private List<String> locations = new ArrayList<>();

		/**
		 * 定时扫描文件夹的配置
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
			 * 是否启用定时扫描，默认为false。
			 */
			private boolean enabled = false;

			/**
			 * 要扫描的目录路径。
			 */
			private String directory;

			/**
			 * 定时任务的cron表达式，默认每小时执行一次。
			 */
			private String cron = "0 0 * * * *";

			/**
			 * 处理完成后的文件归档目录。
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
	 * 专业知识库配置
	 */
	public static class ProfessionalKnowledgeBases {

		/**
		 * 是否启用专业知识库决策，默认为true
		 */
		private boolean decisionEnabled = true;

		/**
		 * 专业知识库列表
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
		 * 单个专业知识库配置
		 */
		public static class KnowledgeBase {

			/**
			 * 知识库ID
			 */
			private String id;

			/**
			 * 知识库名称
			 */
			private String name;

			/**
			 * 知识库描述，用于大模型判断是否需要查询
			 */
			private String description;

			/**
			 * 知识库类型：api, elasticsearch
			 */
			private String type = "api";

			/**
			 * API配置
			 */
			private final Api api = new Api();

			/**
			 * 是否启用
			 */
			private boolean enabled = true;

			/**
			 * 优先级，数字越小优先级越高
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
			 * API配置
			 */
			public static class Api {

				/**
				 * API类型：dashscope, custom
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
				 * 模型名称（适用于dashscope等）
				 */
				private String model;

				/**
				 * 请求超时时间（毫秒）
				 */
				private int timeoutMs = 30000;

				/**
				 * 最大返回结果数
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
	 * 文本分割配置
	 */
	public static class TextSplitter {

		/**
		 * 默认分块大小（token数量），默认800
		 */
		private int defaultChunkSize = 800;

		/**
		 * 分块重叠大小（token数量），默认100
		 */
		private int overlap = 100;

		/**
		 * 最小分块大小（token数量），默认5
		 */
		private int minChunkSizeToSplit = 5;

		/**
		 * 最大分块大小（token数量），默认10000
		 */
		private int maxChunkSize = 10000;

		/**
		 * 是否保持分隔符，默认true
		 */
		private boolean keepSeparator = true;

		/**
		 * 是否启用调试模式，默认false
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
