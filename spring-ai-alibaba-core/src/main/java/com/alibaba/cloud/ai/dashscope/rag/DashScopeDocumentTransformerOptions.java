package com.alibaba.cloud.ai.dashscope.rag;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author nuocheng.lxm
 * @since 2024/8/6 16:21
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeDocumentTransformerOptions {

	/** Maximum number of retry attempts. */
	private @JsonProperty("chunk_size") int chunkSize = 500;

	/** Overlap size between consecutive chunks. */
	private @JsonProperty("overlap_size") int overlapSize = 100;

	/** Separator characters for splitting texts. */
	private @JsonProperty("separator") String separator = "|,|，|。|？|！|\\n|\\\\?|\\\\!";

	/** parse format type. */
	private @JsonProperty("file_type") String fileType = "idp";

	/** language of tokenizor, accept cn, en, any. Notice that <any> mode will be slow. */
	private @JsonProperty("input_type") String language = "cn";

	public static DashScopeDocumentTransformerOptions.Builder builder() {
		return new DashScopeDocumentTransformerOptions.Builder();
	}

	public int getChunkSize() {
		return chunkSize;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	public int getOverlapSize() {
		return overlapSize;
	}

	public void setOverlapSize(int overlapSize) {
		this.overlapSize = overlapSize;
	}

	public String getSeparator() {
		return separator;
	}

	public void setSeparator(String separator) {
		this.separator = separator;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public static class Builder {

		protected DashScopeDocumentTransformerOptions options;

		public Builder() {
			this.options = new DashScopeDocumentTransformerOptions();
		}

		public DashScopeDocumentTransformerOptions.Builder withChunkSize(int chunkSize) {
			this.options.setChunkSize(chunkSize);
			return this;
		}

		public DashScopeDocumentTransformerOptions.Builder withOverlapSize(int overlapSize) {
			this.options.setOverlapSize(overlapSize);
			return this;
		}

		public DashScopeDocumentTransformerOptions.Builder withSeparator(String separator) {
			this.options.setSeparator(separator);
			return this;
		}

		public DashScopeDocumentTransformerOptions.Builder withFileType(String fileType) {
			this.options.setFileType(fileType);
			return this;
		}

		public DashScopeDocumentTransformerOptions.Builder withLanguage(String language) {
			this.options.setLanguage(language);
			return this;
		}

		public DashScopeDocumentTransformerOptions build() {
			return this.options;
		}

	}

}
