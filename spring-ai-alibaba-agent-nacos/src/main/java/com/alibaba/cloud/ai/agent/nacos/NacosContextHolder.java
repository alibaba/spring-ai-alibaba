package com.alibaba.cloud.ai.agent.nacos;

import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.agent.nacos.vo.AgentVO;
import com.alibaba.cloud.ai.agent.nacos.vo.McpServersVO;
import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.cloud.ai.agent.nacos.vo.PromptVO;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import lombok.Data;

@Data
public class NacosContextHolder {

	AgentVO agentVO;

	PromptVO promptVO;

	ModelVO modelVO;

	McpServersVO mcpServersVO;

	ObservationMetadataAwareOptions observationMetadataAwareOptions;

	ReactAgent reactAgent;

	Map<String, PromptListener> promptListeners = new HashMap<>();

}
