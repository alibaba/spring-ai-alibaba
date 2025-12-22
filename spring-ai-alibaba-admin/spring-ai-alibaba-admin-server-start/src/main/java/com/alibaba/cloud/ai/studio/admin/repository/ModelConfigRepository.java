package com.alibaba.cloud.ai.studio.admin.repository;

import com.alibaba.cloud.ai.studio.admin.entity.ModelConfigDO;

import java.util.List;

/**
 * 模型配置仓储接口（文件驱动实现）。
 */
public interface ModelConfigRepository {

    ModelConfigDO findById(Long id);

    boolean existsById(Long id);

    List<ModelConfigDO> list(String name, String provider, Integer status, int offset, int limit);

    int count(String name, String provider, Integer status);

    List<ModelConfigDO> listEnabled();
}


