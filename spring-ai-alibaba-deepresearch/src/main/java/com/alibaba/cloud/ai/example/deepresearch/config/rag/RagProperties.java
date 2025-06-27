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
			private int rrfWindowSize = 100;

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

}
