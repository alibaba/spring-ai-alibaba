package com.alibaba.cloud.ai.example.deepresearch.repository;

import com.alibaba.cloud.ai.example.deepresearch.util.ResourceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class ModelParamRepository {

    //JSON key in configuration file
    private static final String MODELS_ORER_AGENT = "models";

    private final Map<String, List<AgentModel>> modelSet;

    public ModelParamRepository(@Value("classpath:agents-config.json") Resource agentsConfig,
                                ObjectMapper objectMapper){
        try {
            this.modelSet = objectMapper.readValue(ResourceUtil.loadResourceAsString(agentsConfig),
                    new TypeReference<Map<String, List<AgentModel>>>() {
                    });

        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error in parsing model configuration", e);
        }
    }

    /**
     * Get the list of agent models.
     *
     * @return a list of AgentModel parameters.
     */
    public List<AgentModel> loadModels() {
        return modelSet.getOrDefault(MODELS_ORER_AGENT, List.of());
    }

    //fixme: To read external data in the future, this object needs to be redesigned
    public record AgentModel(String name, String modelName) {
    }
}
