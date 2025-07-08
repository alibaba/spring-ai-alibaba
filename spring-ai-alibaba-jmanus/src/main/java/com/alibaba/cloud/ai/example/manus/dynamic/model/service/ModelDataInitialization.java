package com.alibaba.cloud.ai.example.manus.dynamic.model.service;

import com.alibaba.cloud.ai.example.manus.dynamic.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.model.model.enums.ModelType;
import com.alibaba.cloud.ai.example.manus.dynamic.model.repository.DynamicModelRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * @author lizhenning
 * @date 2025/7/8
 */
@Service
public class ModelDataInitialization {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;
    @Value("${spring.ai.openai.api-key}")
    private String apiKey;
    @Value("${spring.ai.openai.chat.options.model}")
    private String model;


    private final DynamicModelRepository repository;

    public ModelDataInitialization(DynamicModelRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        if (repository.count() == 0) {
            DynamicModelEntity dynamicModelEntity = new DynamicModelEntity();
            dynamicModelEntity.setBaseUrl(baseUrl);
            dynamicModelEntity.setApiKey(apiKey);
            dynamicModelEntity.setModelName(model);
            dynamicModelEntity.setClassName(model);
            dynamicModelEntity.setModelDescription("base model");
            dynamicModelEntity.setType(ModelType.GENERAL.name());
            repository.save(dynamicModelEntity);
        }
    }

}
