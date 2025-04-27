package com.alibaba.cloud.ai.example.manus.dynamic.mcp.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo.McpState;

@Service
public class McpStateHolderService {

	private Map<String, McpState> mcpStateMap = new ConcurrentHashMap<>();

	public McpState getMcpState(String key) {
		return mcpStateMap.get(key);
	}

	public void setMcpState(String key, McpState state) {
		mcpStateMap.put(key, state);
	}

	public void removeMcpState(String key) {
		mcpStateMap.remove(key);
	}

}
