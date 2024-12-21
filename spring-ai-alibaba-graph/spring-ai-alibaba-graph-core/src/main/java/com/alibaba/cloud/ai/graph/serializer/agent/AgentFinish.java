package com.alibaba.cloud.ai.graph.serializer.agent;

import java.util.Map;

public record AgentFinish(Map<String, Object> returnValues, String log) {
}
