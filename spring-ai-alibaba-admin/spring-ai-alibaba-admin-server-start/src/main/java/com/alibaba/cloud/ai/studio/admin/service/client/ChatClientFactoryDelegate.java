package com.alibaba.cloud.ai.studio.admin.service.client;

import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;
import com.alibaba.cloud.ai.studio.admin.repository.ModelConfigRepository;
import com.alibaba.cloud.ai.studio.admin.service.ModelConfigBridgeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.observation.ChatClientObservationConvention;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ChatClientFactoryDelegate {
    
    private final ModelConfigRepository modelConfigRepository;
    
    private final ModelConfigBridgeService modelConfigBridgeService;
    
    private final ObjectMapper objectMapper;
    
    private final Map<String, ChatClientFactory> chatClientFactories;
    
    private final ObservationRegistry observationRegistry;

    private final ChatClientObservationConvention customObservationConvention;
    
    public ChatClientFactoryDelegate(ModelConfigRepository modelConfigRepository, 
            ModelConfigBridgeService modelConfigBridgeService,
            ObjectMapper objectMapper,
            ObservationRegistry observationRegistry,
            ChatClientObservationConvention customObservationConvention,
            OpenAiChatClientFactory openAiChatClientFactory,
            DashScopeChatClientFactory dashScopeChatClientFactory,
            DeepSeekChatClientFactory deepSeekChatClientFactory) {
        this.modelConfigRepository = modelConfigRepository;
        this.modelConfigBridgeService = modelConfigBridgeService;
        this.objectMapper = objectMapper;
        this.chatClientFactories = new HashMap<>();
        this.observationRegistry = observationRegistry;
        this.customObservationConvention = customObservationConvention;
        register(openAiChatClientFactory);
        register(dashScopeChatClientFactory);
        register(deepSeekChatClientFactory);
    }
    
    public void register(ChatClientFactory factory) {
        String provider = factory.getSupportProvider();
        chatClientFactories.put(provider, factory);
    }
    
    public List<String> getSupportedProviders() {
        return List.copyOf(chatClientFactories.keySet());
    }
    
    public ChatClient createChatClient(Long modelConfigId, Map<String, Object> userParameters) {
        return createChatClient(modelConfigId, userParameters, null,null);
    }
    
    public ChatClient createChatClient(Long modelConfigId, Map<String, Object> userParameters, Map<String, String> observationMetadata){
        return createChatClient(modelConfigId, userParameters, null, observationMetadata);
    }
    
    public ChatClient createChatClient(Long modelConfigId, Map<String, Object> userParameters, List<Advisor> advisors, Map<String, String> observationMetadata) {
        // 优先从桥接服务查找（从Manager层查询）
        ModelConfigDO config = modelConfigBridgeService.findById(modelConfigId);
        
        // 如果桥接服务中找不到，尝试从ModelConfigRepository查找（保持向后兼容）
        if (config == null) {
            log.debug("桥接服务中未找到模型配置 id={}，尝试从 ModelConfigRepository 查找", modelConfigId);
            config = modelConfigRepository.findById(modelConfigId);
        }
        
        if (config == null) {
            throw new RuntimeException("模型配置不存在: " + modelConfigId);
        }
        
        if (config.getStatus() != 1) {
            throw new RuntimeException("模型配置已禁用: " + modelConfigId);
        }

        String provider = config.getProvider().toLowerCase();
        log.info("创建模型客户端，提供商: {}, 模型: {}", provider, config.getModelName());
        
        ChatClientFactory factory = chatClientFactories.get(provider);
        if (factory == null) {
            // 如果找不到对应的 provider factory，默认使用 OpenAI
            log.warn("未找到提供商 {} 对应的工厂，使用默认的 OpenAI 工厂", provider);
            factory = chatClientFactories.get(OpenAiChatClientFactory.OPEN_AI_PROVIDER);
            if (factory == null) {
                throw new UnsupportedOperationException("不支持的模型提供商: " + config.getProvider() + "，且默认的 OpenAI 工厂也不可用");
            }
        }
        
        ChatModel chatModel = factory.buildChatModel(config);
        Map<String, Object> mergedParameters = mergeParameters(config, userParameters);
        ChatOptions options = factory.buildChatOptions(config, mergedParameters, observationMetadata);
        if (advisors != null) {
            return ChatClient.builder(chatModel,observationRegistry,customObservationConvention).defaultOptions(options).defaultAdvisors(advisors)
                    .build();
        } else {
            return ChatClient.builder(chatModel,observationRegistry,customObservationConvention).defaultOptions(options).defaultAdvisors()
                    .build();
        }
    }
    
    /**
     * 合并默认参数和用户参数
     * 用户参数具有更高优先级，会覆盖默认参数
     *
     * @param userParameters 用户传入的参数
     * @return 合并后的参数
     */
    private Map<String, Object> mergeParameters(ModelConfigDO config, Map<String, Object> userParameters) {
        Map<String, Object> mergedParameters = new HashMap<>();
        
        // 首先添加默认参数
        if (StringUtils.hasText(config.getDefaultParameters())) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> defaultParams = objectMapper.readValue(
                        config.getDefaultParameters(),
                        Map.class
                );
                mergedParameters.putAll(defaultParams);
                log.debug("加载默认参数: {}", defaultParams);
            } catch (Exception e) {
                log.warn("解析默认参数失败: {}", config.getDefaultParameters(), e);
            }
        }
        
        // 然后添加用户参数（覆盖默认参数）
        if (userParameters != null) {
            mergedParameters.putAll(userParameters);
            log.debug("用户参数: {}", userParameters);
        }
        
        log.debug("合并后的参数: {}", mergedParameters);
        return mergedParameters;
    }
}
