package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;

import java.util.List;

/**
 * 模型配置桥接服务
 * 将底层Manager层（ModelManager、ProviderManager）的数据转换为ModelConfigDO格式
 * 供ChatClientFactory等上层服务使用
 */
public interface ModelConfigBridgeService {

    /**
     * 根据ID查找模型配置
     * @param id 模型配置ID（可以是ModelEntity的id或name）
     * @return ModelConfigDO，如果不存在返回null
     */
    ModelConfigDO findById(Long id);

    /**
     * 检查模型配置是否存在
     * @param id 模型配置ID
     * @return true如果存在，false否则
     */
    boolean existsById(Long id);

    /**
     * 查询模型配置列表（支持分页和过滤）
     * @param name 模型名称（模糊匹配）
     * @param provider 提供商名称
     * @param status 状态：1-启用，0-禁用
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 模型配置列表
     */
    List<ModelConfigDO> list(String name, String provider, Integer status, int offset, int limit);

    /**
     * 统计符合条件的模型配置数量
     * @param name 模型名称（模糊匹配）
     * @param provider 提供商名称
     * @param status 状态：1-启用，0-禁用
     * @return 数量
     */
    int count(String name, String provider, Integer status);

    /**
     * 获取所有启用的模型配置列表
     * @return 启用的模型配置列表
     */
    List<ModelConfigDO> listEnabled();

    /**
     * 根据provider和modelId查找模型配置
     * @param provider 提供商名称
     * @param modelId 模型ID
     * @return ModelConfigDO，如果不存在返回null
     */
    ModelConfigDO findByProviderAndModelId(String provider, String modelId);
}

