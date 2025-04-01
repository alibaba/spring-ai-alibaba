package com.alibaba.cloud.ai.example.manus.dynamic.agent.service;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.DynamicAgent;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.entity.DynamicAgentEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.repository.DynamicAgentRepository;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.recorder.PlanExecutionRecorder;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class DynamicAgentLoader {
    
    private final DynamicAgentRepository repository;
    private final LlmService llmService;
    private final PlanExecutionRecorder recorder;
    private final ManusProperties properties;
    private final ToolCallingManager toolCallingManager;
    private final Map<String, ToolCallback> toolCallbackMap;

    public DynamicAgentLoader(DynamicAgentRepository repository,
                            LlmService llmService,
                            PlanExecutionRecorder recorder,
                            ManusProperties properties,
                            ToolCallingManager toolCallingManager,
                            Map<String, ToolCallback> toolCallbackMap) {
        this.repository = repository;
        this.llmService = llmService;
        this.recorder = recorder;
        this.properties = properties;
        this.toolCallingManager = toolCallingManager;
        this.toolCallbackMap = toolCallbackMap;
    }

    public DynamicAgent loadAgent(String agentName) {
        DynamicAgentEntity entity = repository.findByAgentName(agentName);
        if (entity == null) {
            throw new IllegalArgumentException("Agent not found: " + agentName);
        }

        return new DynamicAgent(
            llmService,
            recorder,
            properties,
            entity.getAgentName(),
            entity.getAgentDescription(),
            entity.getSystemPrompt(),
            entity.getNextStepPrompt(),
            toolCallbackMap,
            entity.getAvailableToolKeys(),
            toolCallingManager
        );
    }

    public List<DynamicAgentEntity> getAllAgents() {
        return repository.findAll();
    }
}
