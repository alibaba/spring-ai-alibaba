package com.alibaba.cloud.ai.agent.nacos.vo;

import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class PartnerAgentsVO {

	List<PartnerAgentVO> agents;

	@Data
	public static class PartnerAgentVO {

		String agentName;

		Map<String, String> headers;

		Map<String, String> queryPrams;

	}
}
