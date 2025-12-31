package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.ModelConfigResponse;
import com.alibaba.cloud.ai.studio.admin.dto.ModelParameterDef;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.repository.ModelConfigRepository;
import com.alibaba.cloud.ai.studio.admin.service.ModelConfigService;
import com.alibaba.cloud.ai.studio.admin.service.client.ChatClientFactoryDelegate;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelConfigServiceImpl implements ModelConfigService {

    private final ModelConfigRepository modelConfigRepository;
    private final ObjectMapper objectMapper;
    private final ChatClientFactoryDelegate chatClientFactoryDelegate;

    // 写接口已下线：保留签名以保证接口兼容性（控制器已不再暴露），避免误用
    @Override
    public ModelConfigResponse create(ModelConfigCreateRequest request) throws StudioException {
        throw new StudioException(StudioException.NO_RIGHT, "不支持通过接口创建模型配置，请使用 model-config.yml");
    }

    @Override
    public ModelConfigResponse update(ModelConfigUpdateRequest request) throws StudioException {
        throw new StudioException(StudioException.NO_RIGHT, "不支持通过接口更新模型配置，请使用 model-config.yml");
    }

    @Override
    public void delete(Long id) throws StudioException {
        throw new StudioException(StudioException.NO_RIGHT, "不支持通过接口删除模型配置，请使用 model-config.yml");
    }

    @Override
    public PageResult<ModelConfigResponse> list(ModelConfigQueryRequest request) {
        log.info("查询模型配置列表: {}", request);

        int offset = (request.getPage() - 1) * request.getSize();

        List<ModelConfigDO> list = modelConfigRepository.list(
                request.getName(),
                request.getProvider(),
                request.getStatus(),
                offset,
                request.getSize()
        );

        int total = modelConfigRepository.count(
                request.getName(),
                request.getProvider(),
                request.getStatus()
        );

        List<ModelConfigResponse> responses = list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        PageResult<ModelConfigResponse> pageResult = new PageResult<>();
        pageResult.setPageItems(responses);
        pageResult.setTotalCount((long) total);
        pageResult.setPageNumber((long) request.getPage());
        pageResult.setPageSize((long) request.getSize());
        pageResult.setTotalPage((long) ((total + request.getSize() - 1) / request.getSize()));
        return pageResult;
    }

    @Override
    public ModelConfigResponse getById(Long id) throws StudioException {
        log.info("获取模型配置详情，ID: {}", id);

        ModelConfigDO modelConfig = modelConfigRepository.findById(id);
        if (modelConfig == null) {
            throw new StudioException(StudioException.NOT_FOUND, "模型配置不存在，ID: " + id);
        }
        
        return convertToResponse(modelConfig);
    }

    @Override
    public List<ModelConfigResponse> getEnabledConfigs() {
        log.info("获取启用的模型配置列表");

        List<ModelConfigDO> list = modelConfigRepository.listEnabled();
        return list.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getSupportedProviders() {
        return chatClientFactoryDelegate.getSupportedProviders();
    }

    /**
     * 转换实体对象为响应对象
     */
    private ModelConfigResponse convertToResponse(ModelConfigDO modelConfig) {
        if (modelConfig == null) {
            return null;
        }

        ModelConfigResponse response = new ModelConfigResponse();
        response.setId(modelConfig.getId());
        response.setName(modelConfig.getName());
        response.setProvider(modelConfig.getProvider());
        response.setModelName(modelConfig.getModelName());
        response.setBaseUrl(modelConfig.getBaseUrl());
        response.setDefaultParameters(fromJsonString(modelConfig.getDefaultParameters(), new TypeReference<Map<String, Object>>() {}));
        response.setSupportedParameters(fromJsonString(modelConfig.getSupportedParameters(), new TypeReference<List<ModelParameterDef>>() {}));
        response.setStatus(modelConfig.getStatus());
        response.setCreateTime(modelConfig.getCreateTime());
        response.setUpdateTime(modelConfig.getUpdateTime());

        return response;
    }

    /**
     * 对象转JSON字符串
     */
    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("对象转JSON失败", e);
            return null;
        }
    }

    /**
     * JSON字符串转对象
     */
    private <T> T fromJsonString(String json, TypeReference<T> typeRef) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (Exception e) {
            log.error("JSON转对象失败", e);
            return null;
        }
    }
}
