package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * NodeData for ToolNode, in addition to the original llmResponseKey, outputKey, toolNames.
 */
public class ToolNodeData extends NodeData {

    private String llmResponseKey;

    private String outputKey;

    private List<String> toolNames;

    private List<String> toolCallbacks;

    public ToolNodeData() {
        super(Collections.emptyList(), Collections.emptyList());
    }

    public ToolNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs) {
        super(inputs, outputs);
    }

    public String getLlmResponseKey() {
        return llmResponseKey;
    }

    public ToolNodeData setLlmResponseKey(String llmResponseKey) {
        this.llmResponseKey = llmResponseKey;
        return this;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public ToolNodeData setOutputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }

    public List<String> getToolNames() {
        return toolNames;
    }

    public ToolNodeData setToolNames(List<String> toolNames) {
        this.toolNames = toolNames;
        return this;
    }

    public List<String> getToolCallbacks() {
        return toolCallbacks;
    }

    public ToolNodeData setToolCallbacks(List<String> toolCallbacks) {
        this.toolCallbacks = toolCallbacks;
        return this;
    }
}
