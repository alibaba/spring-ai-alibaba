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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import java.util.Collections;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;

import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

/**
 * NodeData for KnowledgeRetrievalNode, encapsulating all Builder properties.
 */
public class KnowledgeRetrievalNodeData extends NodeData {

	public static Variable getDefaultOutputSchema() {
		return new Variable("result", VariableType.ARRAY_OBJECT);
	}

	private String userPromptKey;

	private String userPrompt;

	private String topKKey;

	private Integer topK;

	private String similarityThresholdKey;

	private Double similarityThreshold;

	private String filterExpressionKey;

	private Filter.Expression filterExpression;

	private String enableRankerKey;

	private Boolean enableRanker;

	private String rerankModelKey;

	private RerankModel rerankModel;

	private String rerankOptionsKey;

	private DashScopeRerankOptions rerankOptions;

	private String vectorStoreKey;

	private VectorStore vectorStore;

	private String outputKey;

	private String retrievalMode;

	private String embeddingModelName;

	private String embeddingProviderName;

	private Double vectorWeight;

	public KnowledgeRetrievalNodeData() {
		super(Collections.emptyList(), List.of(getDefaultOutputSchema()));
	}

	public KnowledgeRetrievalNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	public String getOutputKey() {
		return outputKey;
	}

	public void setOutputKey(String outputKey) {
		this.outputKey = outputKey;
	}

	public String getUserPromptKey() {
		return userPromptKey;
	}

	public void setUserPromptKey(String userPromptKey) {
		this.userPromptKey = userPromptKey;
	}

	public String getUserPrompt() {
		return userPrompt;
	}

	public void setUserPrompt(String userPrompt) {
		this.userPrompt = userPrompt;
	}

	public String getTopKKey() {
		return topKKey;
	}

	public void setTopKKey(String topKKey) {
		this.topKKey = topKKey;
	}

	public Integer getTopK() {
		return topK;
	}

	public void setTopK(Integer topK) {
		this.topK = topK;
	}

	public String getSimilarityThresholdKey() {
		return similarityThresholdKey;
	}

	public void setSimilarityThresholdKey(String similarityThresholdKey) {
		this.similarityThresholdKey = similarityThresholdKey;
	}

	public Double getSimilarityThreshold() {
		return similarityThreshold;
	}

	public void setSimilarityThreshold(Double similarityThreshold) {
		this.similarityThreshold = similarityThreshold;
	}

	public String getFilterExpressionKey() {
		return filterExpressionKey;
	}

	public void setFilterExpressionKey(String filterExpressionKey) {
		this.filterExpressionKey = filterExpressionKey;
	}

	public Filter.Expression getFilterExpression() {
		return filterExpression;
	}

	public void setFilterExpression(Filter.Expression filterExpression) {
		this.filterExpression = filterExpression;
	}

	public String getEnableRankerKey() {
		return enableRankerKey;
	}

	public void setEnableRankerKey(String enableRankerKey) {
		this.enableRankerKey = enableRankerKey;
	}

	public Boolean getEnableRanker() {
		return enableRanker;
	}

	public void setEnableRanker(Boolean enableRanker) {
		this.enableRanker = enableRanker;
	}

	public String getRerankModelKey() {
		return rerankModelKey;
	}

	public void setRerankModelKey(String rerankModelKey) {
		this.rerankModelKey = rerankModelKey;
	}

	public RerankModel getRerankModel() {
		return rerankModel;
	}

	public void setRerankModel(RerankModel rerankModel) {
		this.rerankModel = rerankModel;
	}

	public String getRerankOptionsKey() {
		return rerankOptionsKey;
	}

	public void setRerankOptionsKey(String rerankOptionsKey) {
		this.rerankOptionsKey = rerankOptionsKey;
	}

	public DashScopeRerankOptions getRerankOptions() {
		return rerankOptions;
	}

	public void setRerankOptions(DashScopeRerankOptions rerankOptions) {
		this.rerankOptions = rerankOptions;
	}

	public String getVectorStoreKey() {
		return vectorStoreKey;
	}

	public void setVectorStoreKey(String vectorStoreKey) {
		this.vectorStoreKey = vectorStoreKey;
	}

	public VectorStore getVectorStore() {
		return vectorStore;
	}

	public void setVectorStore(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	public String getEmbeddingProviderName() {
		return embeddingProviderName;
	}

	public void setEmbeddingProviderName(String embeddingProviderName) {
		this.embeddingProviderName = embeddingProviderName;
	}

	public String getRetrievalMode() {
		return retrievalMode;
	}

	public void setRetrievalMode(String retrievalMode) {
		this.retrievalMode = retrievalMode;
	}

	public String getEmbeddingModelName() {
		return embeddingModelName;
	}

	public void setEmbeddingModelName(String embeddingModelName) {
		this.embeddingModelName = embeddingModelName;
	}

	public Double getVectorWeight() {
		return vectorWeight;
	}

	public void setVectorWeight(Double vectorWeight) {
		this.vectorWeight = vectorWeight;
	}

	// KnowledgeRetrieval的用户模板Key就是用户输入Key
	public String getInputKey() {
		return this.userPromptKey;
	}

	public void setInputKey(String userPromptKey) {
		this.userPromptKey = userPromptKey;
	}

}
