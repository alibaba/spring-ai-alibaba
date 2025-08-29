package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;

public class NacosAgentBuilderFactory implements AgentBuilderFactory {

	@Override
	public Builder builder() {
		return new NacosReactAgentBuilder();
	}
}
