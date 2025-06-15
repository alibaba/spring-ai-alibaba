package com.alibaba.cloud.ai.example.deepresearch.config.rag;

import com.alibaba.cloud.ai.example.deepresearch.config.DeepResearchProperties;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for RAG (Retrieval Augmented Generation) functionality.
 *
 * @author xiaofeilog
 */
@ConfigurationProperties(prefix = RagProperties.RAG_PREFIX)
public class RagProperties {

	public static final String RAG_PREFIX = DeepResearchProperties.PREFIX + ".rag";

	private boolean enabled = true;

	private String vectorStoreType = "simple";

	private final Simple simple = new Simple();

	private final Pipeline pipeline = new Pipeline();

	/**
	 * 数据加载相关的配置
	 */
	private final Data data = new Data();

	/**
	 * Elasticsearch配置属性
	 */
	private final Elasticsearch elasticsearch = new Elasticsearch();

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

			private boolean enabled = false;

			private String directory;

			// 默认每小时执行一次
			private String cron = "0 0 * * * *";

			// 处理完成后的文件归档目录
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

	public static class Simple {

		private String storagePath = "vector_store.json";

		public String getStoragePath() {
			return storagePath;
		}

		public void setStoragePath(String storagePath) {
			this.storagePath = storagePath;
		}

	}

	public static class Pipeline {

		private boolean queryExpansionEnabled = false;

		private boolean queryTranslationEnabled = false;

		private String queryTranslationLanguage = "English";

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

		private String indexName = "spring-ai-rag-es-index";

		private int dimensions = 1536;

		private String uris; // e.g. "http://localhost:9200"

		private String username;

		private String password;

		private SimilarityFunction similarityFunction;

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

	}

}
