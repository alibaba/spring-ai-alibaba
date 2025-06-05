package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * NodeData for ParameterParsingNode, which contains three fields: inputTextKey, parameters, and outputKey.
 */
public class ParameterParsingNodeData extends NodeData {

    private String inputTextKey;

    private List<Map<String, String>> parameters;

    private String outputKey;

    public ParameterParsingNodeData() {
        super(Collections.emptyList(), Collections.emptyList());
    }

    public ParameterParsingNodeData(List<VariableSelector> inputs, List<com.alibaba.cloud.ai.model.Variable> outputs) {
        super(inputs, outputs);
    }

    public String getInputTextKey() {
        return inputTextKey;
    }

    public ParameterParsingNodeData setInputTextKey(String inputTextKey) {
        this.inputTextKey = inputTextKey;
        return this;
    }

    public List<Map<String, String>> getParameters() {
        return parameters;
    }

    public ParameterParsingNodeData setParameters(List<Map<String, String>> parameters) {
        this.parameters = parameters;
        return this;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public ParameterParsingNodeData setOutputKey(String outputKey) {
        this.outputKey = outputKey;
        return this;
    }
}
