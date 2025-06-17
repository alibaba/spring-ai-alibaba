package com.alibaba.cloud.ai.example.deepresearch.agents;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.example.deepresearch.repository.ModelParamRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * This configuration class will configure the corresponding
 * ChatClientBuilder tool according to the `agents-config.json`
 * file to provide multi-agent capabilities for deepsearch.
 * different {@link ChatClient.Builder} will be created
 * according to the name column.
 * for example: `researchChatClientBuilder`
 *
 * @author ViliamSun
 * @since 0.1.0
 */
@Configuration
public class AgentModelsConfiguration implements InitializingBean {

    private static final String BEAN_NAME_SUFFIX = "ChatClientBuilder";

    private final List<ModelParamRepository.AgentModel> models;

    private final DashScopeConnectionProperties commonProperties;

    private final BiConsumer<String, DashScopeChatModel> registerConsumer;

    public AgentModelsConfiguration(ModelParamRepository modelParamRepository,
                                    ConfigurableBeanFactory beanFactory,
                                    DashScopeConnectionProperties dashScopeConnectionProperties) {
        Assert.notNull(modelParamRepository, "ModelParamRepository must not be null");
        this.commonProperties = dashScopeConnectionProperties;
        // load models from the repository
        this.models = modelParamRepository.loadModels();
        this.registerConsumer = (key, value) ->
                beanFactory.registerSingleton(key.concat(BEAN_NAME_SUFFIX), ChatClient.create(value).mutate());
    }

    /**
     * Creates a map of agent model names to their corresponding {@link DashScopeChatModel} instances.
     *
     * @return a map where the key is the model name and the value is the {@link DashScopeChatModel} instance.
     */
    private Map<String, DashScopeChatModel> agentModels() {
        // Convert AgentModel to DashScopeChatModel
        return models.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(ModelParamRepository.AgentModel::name, model ->
                                DashScopeChatModel.builder()
                                        .dashScopeApi(DashScopeApi.builder().apiKey(commonProperties.getApiKey()).build())
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

    @Override
    public void afterPropertiesSet() throws Exception {
        this.agentModels().forEach(registerConsumer);
    }
}
