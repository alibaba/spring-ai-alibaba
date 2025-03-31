package com.alibaba.cloud.ai.example.manus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

@Component
@ConfigurationProperties(prefix = "spring.ai")
public class AIProperties {

    @ConfigProperty(
        group = "ai",
        subGroup = "openai",
        key = "base_url",
        path = "spring.ai.openai.base-url",
        description = "OpenAI 兼容接口基础URL",
        defaultValue = "https://dashscope.aliyuncs.com/compatible-mode",
        inputType = ConfigInputType.TEXT
    )
    private String baseUrl;

    @ConfigProperty(
        group = "ai",
        subGroup = "openai",
        key = "api_key",
        path = "spring.ai.openai.api-key",
        description = "DashScope API密钥",
        defaultValue = "",
        inputType = ConfigInputType.TEXT
    )
    private String apiKey;

    @ConfigProperty(
        group = "ai",
        subGroup = "openai",
        key = "model",
        path = "spring.ai.openai.chat.options.model",
        description = "使用的AI模型",
        defaultValue = "qwen-max-latest",
        inputType = ConfigInputType.SELECT,
        options = {
            @ConfigOption(value = "qwen-max-latest", label = "通义千问Max"),
            @ConfigOption(value = "qwen-plus-latest", label = "通义千问Plus"),
            @ConfigOption(value = "qwen-turbo-latest", label = "通义千问Turbo")
        }
    )
    private String model;

    @ConfigProperty(
        group = "ai",
        subGroup = "mcp",
        key = "request_timeout",
        path = "spring.ai.mcp.client.request-timeout",
        description = "MCP请求超时时间(毫秒)",
        defaultValue = "60000",
        inputType = ConfigInputType.NUMBER
    )
    private Integer requestTimeout;

    // Getters and Setters
    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Integer requestTimeout) {
        this.requestTimeout = requestTimeout;
    }
}
