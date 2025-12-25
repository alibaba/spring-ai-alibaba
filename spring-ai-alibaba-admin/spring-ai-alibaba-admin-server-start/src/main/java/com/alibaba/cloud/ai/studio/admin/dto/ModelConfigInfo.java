package com.alibaba.cloud.ai.studio.admin.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ModelConfigInfo {

    /**
     * 模型配置ID（必需字段）
     */
    @JsonProperty("modelId")
    private Long modelId;

    /**
     * 动态参数存储
     * 存储除modelId外的所有模型参数
     */
    @JsonIgnore
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * Jackson反序列化时处理未知属性
     * 将所有除modelId外的属性存储到parameters中
     */
    @JsonAnySetter
    private void setDynamicProperty(String key, Object value) {
        if (!"modelId".equals(key)) {
            parameters.put(key, value);
        }
    }

    /**
     * Jackson序列化时输出动态属性
     */
    @JsonAnyGetter
    public Map<String, Object> getParameters() {
        return parameters;
    }

    /**
     * 获取指定参数值
     *
     * @param parameterName 参数名
     * @return 参数值
     */
    public Object getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

    /**
     * 获取指定参数值（指定类型）
     *
     * @param parameterName 参数名
     * @param type         期望的类型
     * @param <T>          类型参数
     * @return 参数值
     */
    @SuppressWarnings("unchecked")
    public <T> T getParameter(String parameterName, Class<T> type) {
        Object value = parameters.get(parameterName);
        if (value == null) {
            return null;
        }
        
        try {
            return (T) value;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                String.format("参数 %s 的值 %s 无法转换为类型 %s", 
                    parameterName, value, type.getSimpleName()), e);
        }
    }

    /**
     * 设置参数值
     *
     * @param parameterName 参数名
     * @param value        参数值
     */
    public void setParameter(String parameterName, Object value) {
        parameters.put(parameterName, value);
    }

    /**
     * 检查是否包含指定参数
     *
     * @param parameterName 参数名
     * @return 是否包含
     */
    public boolean hasParameter(String parameterName) {
        return parameters.containsKey(parameterName);
    }

    /**
     * 获取所有参数
     *
     * @return 参数Map
     */
    public Map<String, Object> getAllParameters() {
        return parameters;
    }
}
