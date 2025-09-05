package com.alibaba.cloud.ai.graph.agent.factory;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.DefaultBuilder;

public class DefaultAgentBuilderFactory implements AgentBuilderFactory {
    
    @Override
    public Builder builder() {
        return new DefaultBuilder();
    }
}
