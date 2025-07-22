package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.agents.AgentManager;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepository;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepositoryImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ModelConfigService {
    private static final String CONFIG_PATH = "model-config.json";
    private final ModelParamRepository modelParamRepository;
    private final AgentManager agentManager;

    public ModelConfigService(ModelParamRepository modelParamRepository, AgentManager agentManager) {
        this.modelParamRepository = modelParamRepository;
        this.agentManager = agentManager;
    }

    @PostConstruct
    private void init() throws IOException {
        URL resource = getClass().getClassLoader().getResource(CONFIG_PATH);
        if (resource == null) {
            throw new IOException("model-config.json not found in resources");
        }
    }

    public List<ModelParamRepositoryImpl.AgentModel> getModelConfigs() throws IOException {
        return modelParamRepository.loadModels();
    }

    public void updateModelConfigs(List<ModelParamRepositoryImpl.AgentModel> models) throws IOException {
        // todo: 这里可以增加修改配置文件的逻辑
        agentManager.batchUpdateAgents(models);
    }
} 