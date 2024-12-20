package com.alibaba.cloud.ai.dashscope.embedding;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.ai.embedding.EmbeddingOptions;

/**
 * @author nuocheng.lxm
 * @author why_ohh
 * @author yuluo
 * @author <a href="mailto:550588941@qq.com">why_ohh</a>
 * @since 2024/8/1 11:14
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DashScopeEmbeddingOptions implements EmbeddingOptions {

	private @JsonProperty("model") String model;

	private @JsonProperty("text_type") String textType;

	private @JsonProperty("dimensions") Integer dimensions;

	public static Builder builder() {
		return new Builder();
	}

	public String getModel() {
		return this.model;
	}

	public void setModel(String model) {
		this.model = model;
	}

	public Integer getDimensions() {
		return this.dimensions;
	}

	public void setDimensions(Integer dimensions) {
		this.dimensions = dimensions;
	}

	public String getTextType() {
		return this.textType;
	}

	public void setTextType(String textType) {
		this.textType = textType;
	}

	public static class Builder {

		protected DashScopeEmbeddingOptions options;

		public Builder() {
			this.options = new DashScopeEmbeddingOptions();
		}

		public Builder withModel(String model) {
			this.options.setModel(model);
			return this;
		}

		public Builder withDimensions(Integer dimensions) {
			this.options.setDimensions(dimensions);
			return this;
		}

		public Builder withTextType(String textType) {
			this.options.setTextType(textType);
			return this;
		}

		public DashScopeEmbeddingOptions build() {
			return this.options;
		}

	}

}
