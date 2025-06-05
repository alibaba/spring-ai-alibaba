package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;

import java.util.Collections;
import java.util.List;

/**
 * NodeData for HumanNode
 */
public class HumanNodeData extends NodeData {

    public static final List<Variable> DEFAULT_OUTPUT_SCHEMA = Collections.emptyList();

    /** interruptStrategy，"always" or "conditioned" */
    private String interruptStrategy;

    /** When interruptStrategy is "conditioned", check the boolean value for this status key */
    private String interruptConditionKey;

    /**  When human feedback is received, only those keys in the feedback data are updated to OverAllState */
    private List<String> stateUpdateKeys;

    public HumanNodeData() {
        super(Collections.emptyList(), DEFAULT_OUTPUT_SCHEMA);
        this.interruptStrategy = "always";
    }

    public HumanNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
        super(inputs, outputs);
        this.interruptStrategy = "always";
    }

    public String getInterruptStrategy() {
        return interruptStrategy;
    }

    public void setInterruptStrategy(String interruptStrategy) {
        this.interruptStrategy = interruptStrategy;
    }

    public String getInterruptConditionKey() {
        return interruptConditionKey;
    }

    public void setInterruptConditionKey(String interruptConditionKey) {
        this.interruptConditionKey = interruptConditionKey;
    }

    public List<String> getStateUpdateKeys() {
        return stateUpdateKeys;
    }

    public void setStateUpdateKeys(List<String> stateUpdateKeys) {
        this.stateUpdateKeys = stateUpdateKeys;
    }
}
