package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Configuration class for agent models.
 *
 * @author ViliamSun
 * @since 0.1.0
 */
@Configuration
public class AgentModelsConfiguration {

    private static final String BEAN_NAME_SUFFIX = "ChatClientBuilder";

    private final ConfigurableBeanFactory beanFactory;


    private final List<ModelParamRepository.AgentModel> models;

    public AgentModelsConfiguration(ModelParamRepository modelParamRepository, ConfigurableBeanFactory beanFactory) {
        Assert.notNull(modelParamRepository, "ModelParamRepository must not be null");
        this.beanFactory = beanFactory;
        // load models from the repository
        this.models = modelParamRepository.loadModels();
    }

    @Bean
    public CommandLineRunner buildAgentChatClientBuilder(Map<String, DashScopeChatModel> agentModels) {
        return method -> {
            agentModels.forEach((key, value) -> {
                String beanName = key.concat(BEAN_NAME_SUFFIX);
                beanFactory.registerSingleton(beanName, value);
            });
        };
    }

    @Bean
    public Map<String, DashScopeChatModel> agentModels() {
        // Convert AgentModel to DashScopeChatModel
        return models.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ModelParamRepository.AgentModel::name, model ->
                                DashScopeChatModel.builder()
                                        .dashScopeApi(DashScopeApi.builder().build())
                                        .defaultOptions(
                                                DashScopeChatOptions
                                                        .builder()
                                                        .withModel(model.modelName())
                                                        .withTemperature(DashScopeChatModel.DEFAULT_TEMPERATURE)
                                                        .build()
                                        ).build(),
                        (existing, replacement) -> existing) // On duplicate key, keep the)
                );
    }
}
