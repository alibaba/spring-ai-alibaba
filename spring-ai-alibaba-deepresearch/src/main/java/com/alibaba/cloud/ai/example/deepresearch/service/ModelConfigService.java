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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private File configFile;
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
        configFile = new File(resource.getFile());
    }

    public List<ModelParamRepositoryImpl.AgentModel> getModelConfigs() throws IOException {
        return modelParamRepository.loadModels();
    }

    public void updateModelConfigs(List<ModelParamRepositoryImpl.AgentModel> models) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put("models", models);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(configFile, map);
        agentManager.batchUpdateAgents(models);
    }
} 