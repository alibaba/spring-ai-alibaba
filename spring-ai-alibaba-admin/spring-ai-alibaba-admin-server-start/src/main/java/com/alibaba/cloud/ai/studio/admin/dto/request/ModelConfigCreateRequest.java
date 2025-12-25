package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.ModelParameterDef;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelConfigCreateRequest {

    /**
     * 模型名称
     */
    @NotBlank(message = "模型名称不能为空")
    @Size(max = 100, message = "模型名称不能超过100个字符")
    private String name;

    /**
     * 提供商
     */
    @NotBlank(message = "提供商不能为空")
    @Size(max = 50, message = "提供商不能超过50个字符")
    private String provider;

    /**
     * 模型标识符
     */
    @NotBlank(message = "模型标识符不能为空")
    @Size(max = 100, message = "模型标识符不能超过100个字符")
    private String modelName;

    /**
     * 模型服务地址
     */
    @NotBlank(message = "模型服务地址不能为空")
    @Size(max = 500, message = "模型服务地址不能超过500个字符")
    private String baseUrl;

    /**
     * API密钥
     */
    @NotBlank(message = "API密钥不能为空")
    @Size(max = 500, message = "API密钥不能超过500个字符")
    private String apiKey;

    /**
     * 默认参数配置
     */
    private Map<String, Object> defaultParameters;

    /**
     * 支持的参数定义
     */
    private List<ModelParameterDef> supportedParameters;
}
