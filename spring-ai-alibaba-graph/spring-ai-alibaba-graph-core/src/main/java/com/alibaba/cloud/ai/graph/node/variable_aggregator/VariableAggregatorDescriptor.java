package com.alibaba.cloud.ai.graph.node.variable_aggregator;

import com.alibaba.cloud.ai.graph.NodeActionDescriptor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class VariableAggregatorDescriptor extends NodeActionDescriptor {
    private List<List<String>> variables;
    private String outputType;
    private AdvancedSettings advancedSettings;

    @Data
    public static class Groups{
        private String outputType;
        private List<List<String>> variables;
        private String groupName;
        private String groupId;
    }

    @Data
    public static class AdvancedSettings {
        private boolean groupEnabled;
        private List<Groups> groups;
    }

}
