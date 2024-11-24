package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.ModelObservationEntity;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
public interface ModelObservationService {

    List<ModelObservationEntity> list();

    Integer insert(ModelObservationEntity modelObservationEntity);
}
