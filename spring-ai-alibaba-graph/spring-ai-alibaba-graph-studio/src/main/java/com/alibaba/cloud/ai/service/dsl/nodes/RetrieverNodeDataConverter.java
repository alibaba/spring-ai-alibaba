package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.RetrieverNodeData;
import com.alibaba.cloud.ai.service.dsl.NodeDataConverter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class RetrieverNodeDataConverter implements NodeDataConverter<RetrieverNodeData> {

	@Override
	public Boolean supportType(String nodeType) {
		return NodeType.RETRIEVER.value().equals(nodeType);
	}

	@Override
	public RetrieverNodeData parseDifyData(Map<String, Object> data) {
		List<String> selector = (List<String>) data.get("query_variable_selector");
		List<VariableSelector> inputs = List.of(new VariableSelector(selector.get(0), selector.get(1)));
		Map<String, Object> configMap = (Map<String, Object>) data.get("multiple_retrieval_config");
		Map<String, Object> rerankConfigMap = (Map<String, Object>) configMap.get("reranking_model");
		Float rerankThreshold = (Float) Optional.ofNullable(rerankConfigMap.get("score_threshold"))
			.orElse(RetrieverNodeData.RerankOptions.DEFAULT_RERANK_THRESHOLD);
		RetrieverNodeData.RerankOptions rerankOptions = new RetrieverNodeData.RerankOptions()
			.setEnableRerank((Boolean) configMap.get("reranking_enable"))
			.setRerankModelName((String) rerankConfigMap.get("model"))
			.setRerankModelProvider((String) rerankConfigMap.get("provider"))
			.setRerankTopK((Integer) configMap.get("top_k"))
			.setRerankThreshold(rerankThreshold);
		// we can't access dify's vector store, so we use a dummy name here
		RetrieverNodeData.RetrievalOptions retrievalOptions = new RetrieverNodeData.RetrievalOptions()
			.setStoreName("[need to replace]dify knowledge")
			.setRerankOptions(RetrieverNodeData.DEFAULT_RERANK_OPTIONS);
		return new RetrieverNodeData(inputs, RetrieverNodeData.OUTPUT_SCHEMA).setOptions(List.of(retrievalOptions))
			.setMultipleRetrievalOptions(rerankOptions);
	}

	@Override
	public Map<String, Object> dumpDifyData(RetrieverNodeData nodeData) {
		Map<String, Object> data = new HashMap<>();
		RetrieverNodeData.RerankOptions rerankConfig = nodeData.getMultipleRetrievalOptions();
		Map<String, Object> configMap = Map.of("reranking_enabled", rerankConfig.getEnableRerank(), "reranking_mode",
				"reranking_model", "reranking_model",
				Map.of("model", rerankConfig.getRerankModelName(), "provider", rerankConfig.getRerankModelProvider()),
				"score_threshold", rerankConfig.getRerankThreshold(), "top_k", rerankConfig.getRerankTopK());
		data.put("dataset_ids", List.of());
		data.put("multiple_retrieval_config", configMap);
		data.put("query_variable_selector", List.of(nodeData.getInputs().get(0).getNamespace(),
				nodeData.getInputs().get(0).getName()));
		data.put("retrieval_mode", "multiple");
		return data;
	}

}
