package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration;
import com.alibaba.cloud.ai.example.manus.config.startUp.ManusConfiguration.ToolCallBackContext;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.model.Tool;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AgentServiceImpl implements AgentService {

    @Autowired
    private DynamicAgentLoader dynamicAgentLoader;

    @Autowired
    private DynamicAgentRepository repository;

    @Autowired
    private ManusConfiguration manusConfiguration;


    @Override
    public List<AgentConfig> getAllAgents() {
        return repository.findAll().stream()
                .map(this::mapToAgentConfig)
                .collect(Collectors.toList());
    }

    @Override
    public AgentConfig getAgentById(String id) {
        DynamicAgentEntity entity = repository.findById(Long.parseLong(id))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + id));
        return mapToAgentConfig(entity);
    }

    @Override
    public AgentConfig createAgent(AgentConfig config) {
        DynamicAgentEntity entity = new DynamicAgentEntity();
        updateEntityFromConfig(entity, config);
        entity = repository.save(entity);
        return mapToAgentConfig(entity);
    }

    @Override
    public AgentConfig updateAgent(AgentConfig config) {
        DynamicAgentEntity entity = repository.findById(Long.parseLong(config.getId()))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + config.getId()));
        updateEntityFromConfig(entity, config);
        entity = repository.save(entity);
        return mapToAgentConfig(entity);
    }

    @Override
    public void deleteAgent(String id) {
        repository.deleteById(Long.parseLong(id));
    }

    @Override
    public List<Tool> getAvailableTools() {

        Map<String, ToolCallBackContext>  toolcallContext = manusConfiguration.toolCallbackMap(null);
        return toolcallContext.entrySet().stream()
                .map(entry -> {
                    Tool tool = new Tool();
                    tool.setKey(entry.getKey());
                    tool.setName(entry.getKey()); // You might want to provide a more friendly name
                    tool.setEnabled(true);
                    return tool;
                })
                .collect(Collectors.toList());
    }

    private AgentConfig mapToAgentConfig(DynamicAgentEntity entity) {
        AgentConfig config = new AgentConfig();
        config.setId(entity.getId().toString());
        config.setName(entity.getAgentName());
        config.setDescription(entity.getAgentDescription());
        config.setSystemPrompt(entity.getSystemPrompt());
        config.setNextStepPrompt(entity.getNextStepPrompt());
        config.setAvailableTools(entity.getAvailableToolKeys());
        config.setClassName(entity.getClassName());
        return config;
    }

    private void updateEntityFromConfig(DynamicAgentEntity entity, AgentConfig config) {
        entity.setAgentName(config.getName());
        entity.setAgentDescription(config.getDescription());
        entity.setSystemPrompt(config.getSystemPrompt());
        entity.setNextStepPrompt(config.getNextStepPrompt());
        entity.setAvailableToolKeys(config.getAvailableTools());
        entity.setClassName(config.getClassName());
    }
}
