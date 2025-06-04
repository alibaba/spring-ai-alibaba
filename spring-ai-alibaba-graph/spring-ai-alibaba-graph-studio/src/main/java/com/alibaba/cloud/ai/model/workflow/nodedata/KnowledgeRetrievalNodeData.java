package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.graph.node.KnowledgeRetrievalNode;
import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankOptions;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.Collections;
import java.util.List;

/**
 * NodeData for KnowledgeRetrievalNode, encapsulating all Builder properties.
 */
public class KnowledgeRetrievalNodeData extends NodeData {

    public static final List<Variable> DEFAULT_OUTPUT_SCHEMA =
        List.of(new Variable("retrieved_documents", VariableType.ARRAY_OBJECT.value()));

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

    public KnowledgeRetrievalNodeData() {
        super(Collections.emptyList(), DEFAULT_OUTPUT_SCHEMA);
    }

    public KnowledgeRetrievalNodeData(List<VariableSelector> inputs,
                                      List<Variable> outputs) {
        super(inputs, outputs);
    }

    // Getters and Setters for all properties

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
}
