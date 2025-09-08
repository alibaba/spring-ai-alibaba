package com.alibaba.cloud.ai.agent.nacos.vo;

import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Data;

@Data
public class McpServersVO {

	List<McpServerVO> mcpServers;

	@Data
	public static class McpServerVO {

		String mcpServerName;

		String version;

		Set<String> whiteTools;

		Map<String, String> passHeaders;

		Map<String, String> passQueryParams;

	}

}


