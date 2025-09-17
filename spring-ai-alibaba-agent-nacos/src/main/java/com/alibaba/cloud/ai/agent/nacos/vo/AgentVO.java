package com.alibaba.cloud.ai.agent.nacos.vo;

import lombok.Data;

@Data
public class AgentVO {

	String promptKey;

	String description;

	int maxIterations;
}
