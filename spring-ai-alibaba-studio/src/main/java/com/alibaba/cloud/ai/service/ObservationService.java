package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.ObservationEntity;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
public interface ObservationService {

    List<ObservationEntity> list();

    Integer insert(ObservationEntity ObservationEntity);

    void exportObservation();
}
