package com.alibaba.cloud.ai.graph.node.variable_aggregator;

import com.alibaba.cloud.ai.graph.CompileConfig;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.NodeActionWithConfig;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.state.NodeState;
import com.alibaba.cloud.ai.graph.utils.TryConsumer;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class VariableAggregatorNodeAction implements NodeActionWithConfig {
    private VariableAggregatorDescriptor variableAggregatorDescriptor;
    private CompileConfig compileConfig;
    private static final String OUT_TYPE_PROMPT = "outType";

    public VariableAggregatorNodeAction(VariableAggregatorDescriptor variableAggregatorDescriptor,
                                        CompileConfig compileConfig) {
        this.variableAggregatorDescriptor = variableAggregatorDescriptor;
        this.compileConfig = compileConfig;
    }

    @Override
    public Map<String, Object> apply(NodeState t, RunnableConfig config) throws Exception {
        SaverConfig saverConfig = compileConfig.getSaverConfig();
        BaseCheckpointSaver checkpointSaver = saverConfig.get();
        VariableAggregatorDescriptor.AdvancedSettings advancedSettings = variableAggregatorDescriptor.getAdvancedSettings();
        AtomicReference<Object> outPutResult = new AtomicReference<>();
        boolean isGroup = (advancedSettings == null || !advancedSettings.isGroupEnabled());
        String groupName = "";
        String outType = "";
        if (isGroup) {
            List<List<String>> variables = variableAggregatorDescriptor.getVariables();
            setOutPut(config, variables, checkpointSaver, outPutResult);
            outType = variableAggregatorDescriptor.getOutputType();
        } else {
            List<VariableAggregatorDescriptor.Groups> groups = advancedSettings.getGroups();
            for (VariableAggregatorDescriptor.Groups group : groups) {
                List<List<String>> variables = group.getVariables();
                setOutPut(config, variables, checkpointSaver, outPutResult);
                groupName = group.getGroupName();
                outType = group.getOutputType();
            }
        }
        if (outPutResult.get() == null) throw new IllegalStateException("output is null");
        if (!isGroup) {
            return Map.of(NodeState.OUTPUT, Map.of(groupName, Map.of(NodeState.OUTPUT, outPutResult), OUT_TYPE_PROMPT, outType));
        }
        return Map.of(NodeState.OUTPUT, Map.of(OUT_TYPE_PROMPT, outType, NodeState.OUTPUT, outPutResult));
    }

    private static void setOutPut(RunnableConfig config, List<List<String>> variables, BaseCheckpointSaver checkpointSaver, AtomicReference<Object> outPutResult) {
        for (List<String> variable : variables) {
            Optional<Checkpoint> byNodeId = checkpointSaver.getByNodeId(config, variable.get(0));
            if (outPutResult.get() == null) {
                byNodeId.ifPresent((TryConsumer<Checkpoint, Throwable>)
                        checkpoint -> outPutResult.set(checkpoint.getState().get(variable.get(1))));
            }
        }
    }

}
