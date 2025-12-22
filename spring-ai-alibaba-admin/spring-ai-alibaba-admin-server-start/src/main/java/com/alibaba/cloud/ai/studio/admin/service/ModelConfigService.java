package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.ModelConfigResponse;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;

import java.util.List;

public interface ModelConfigService {

    /**
     * 创建模型配置
     *
     * @param request 创建请求
     * @return 模型配置响应
     */
    ModelConfigResponse create(ModelConfigCreateRequest request) throws StudioException;

    /**
     * 更新模型配置
     *
     * @param request 更新请求
     * @return 模型配置响应
     */
    ModelConfigResponse update(ModelConfigUpdateRequest request) throws StudioException;

    /**
     * 删除模型配置
     *
     * @param id 模型配置ID
     */
    void delete(Long id) throws StudioException;

    /**
     * 获取模型配置列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<ModelConfigResponse> list(ModelConfigQueryRequest request);

    /**
     * 根据ID获取模型配置详情
     *
     * @param id 模型配置ID
     * @return 模型配置响应
     */
    ModelConfigResponse getById(Long id) throws StudioException;

    /**
     * 获取启用的模型配置列表
     *
     * @return 启用的模型配置列表
     */
    List<ModelConfigResponse> getEnabledConfigs();
    
    /**
     * 支持的模型提供商列表
     *
     * @return 支持的模型提供商列表
     */
    List<String> getSupportedProviders();
    
}
