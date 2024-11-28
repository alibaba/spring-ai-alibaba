package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.ObservationDetailEntity;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
public interface ObservationDetailService {

	List<ObservationDetailEntity> list();

	Integer insert(ObservationDetailEntity modelObservationDetailEntity);

	void exportObservationDetail();

}
