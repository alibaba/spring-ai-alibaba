package com.alibaba.cloud.ai.agent.nacos;

import com.alibaba.cloud.ai.graph.agent.Builder;
import com.alibaba.cloud.ai.graph.agent.factory.AgentBuilderFactory;

public class NacosAgentBuilderFactory implements AgentBuilderFactory {

	private NacosOptions nacosOptions;

	public NacosAgentBuilderFactory(NacosOptions nacosOptions) {
		this.nacosOptions = nacosOptions;
	}

	@Override
	public Builder builder() {
		return new NacosReactAgentBuilder().nacosOptions(this.nacosOptions);
	}
}
