package com.alibaba.cloud.ai.studio.admin.generator.service.generator.agent;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/8/28 17:56
 */
@Component
public class AgentTypeProviderRegistry {

    private final Map<String, AgentTypeProvider> providers;

    public AgentTypeProviderRegistry(List<AgentTypeProvider> providers) {
        this.providers = providers.stream().collect(Collectors.toMap(AgentTypeProvider::type, p -> p));
    }

    public AgentTypeProvider get(String type) {
        AgentTypeProvider p = providers.get(type);
        if (p == null) {
            throw new IllegalArgumentException("No AgentTypeProvider for type: " + type);
        }
        return p;
    }
}
