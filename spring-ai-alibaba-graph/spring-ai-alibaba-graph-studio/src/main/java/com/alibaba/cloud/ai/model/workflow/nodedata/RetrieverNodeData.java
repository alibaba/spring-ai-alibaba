package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@NoArgsConstructor
@Data
public class RetrieverNodeData extends NodeData {

	public static final List<Variable> INPUT_SCHEMA = List.of(new Variable("query", VariableType.STRING.value()));

	public static final List<Variable> OUTPUT_SCHEMA = List
		.of(new Variable("documents", VariableType.ARRAY_OBJECT.value()));

	public static final RerankOptions DEFAULT_RERANK_OPTIONS = new RerankOptions();

	private List<RetrievalOptions> options;

	private RerankOptions multipleRetrievalOptions;

	public RetrieverNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public enum RetrievalMode {

		DENSE("dense", "semantic_search"),

		SPARSE("sparse", "full_text_search"),

		HYBRID("hybrid", "hybrid_search");
		;

		private String value;

		private String difyValue;

		RetrievalMode(String value, String difyValue) {
			this.value = value;
			this.difyValue = difyValue;
		}

		public String value() {
			return value;
		}

		public RetrievalMode difyValueOf(String difyMode) {
			for (RetrievalMode mode : RetrievalMode.values()) {
				if (mode.difyValue.equals(difyMode)) {
					return mode;
				}
			}
			return null;
		}

	}

	@Data
	@Accessors(chain = true)
	public static class RetrievalOptions {

		public static final String DEFAULT_STORE_NAME = "default";

		public static final String DEFAULT_EMBEDDING_MODEL_NAME = "text-embedding-v1";

		public static final String DEFAULT_EMBEDDING_MODEL_PROVIDER = "tongyi";

		public static final String DEFAULT_RETRIEVAL_MODE = RetrievalMode.DENSE.value;

		public static final Integer DEFAULT_DENSE_TOP_K = 50;

		public static final Integer DEFAULT_SPARSE_TOP_K = 50;

		private String storeName;

		private String embeddingModelName = DEFAULT_EMBEDDING_MODEL_NAME;

		private String embeddingModelProvider = DEFAULT_EMBEDDING_MODEL_PROVIDER;

		private String retrievalMode = DEFAULT_RETRIEVAL_MODE;

		private Integer denseTopK = DEFAULT_DENSE_TOP_K;

		private Integer sparseTopK = DEFAULT_SPARSE_TOP_K;

		private RerankOptions rerankOptions;

		private Map<String, Object> extraProperties;

	}

	@Data
	@Accessors(chain = true)
	public static class RerankOptions {

		public static final String DEFAULT_RERANK_MODEL_NAME = "gte-rerank-hybrid";

		public static final String DEFAULT_RERANK_MODEL_PROVIDER = "tongyi";

		public static final Float DEFAULT_RERANK_THRESHOLD = 0.1F;

		public static final Integer DEFAULT_RERANK_TOP_K = 5;

		private Boolean enableRerank = true;

		private String rerankModelName = DEFAULT_RERANK_MODEL_NAME;

		private String rerankModelProvider = DEFAULT_RERANK_MODEL_PROVIDER;

		private Float rerankThreshold = DEFAULT_RERANK_THRESHOLD;

		private Integer rerankTopK = DEFAULT_RERANK_TOP_K;

	}

}
