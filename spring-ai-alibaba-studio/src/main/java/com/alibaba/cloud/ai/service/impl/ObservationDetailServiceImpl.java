package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.entity.ObservationDetailEntity;
import com.alibaba.cloud.ai.entity.ObservationEntity;
import com.alibaba.cloud.ai.mapper.ModelObservationDetailMapper;
import com.alibaba.cloud.ai.service.ObservationDetailService;
import com.alibaba.excel.EasyExcel;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
@Service
public class ObservationDetailServiceImpl implements ObservationDetailService {

	@Resource
	private ModelObservationDetailMapper observationDetailMapper;

	@Override
	public List<ObservationDetailEntity> list() {

		return observationDetailMapper.selectList(null);
	}

	@Override
	public Integer insert(ObservationDetailEntity modelObservationDetailEntity) {
		return observationDetailMapper.insert(modelObservationDetailEntity);
	}

	@Override
	public void exportObservationDetail() {
		String fileName = "Observation_" + "simpleWrite_" + System.currentTimeMillis() + ".xlsx";
		EasyExcel.write(fileName, ObservationDetailEntity.class)
			.sheet("模板")
			.doWrite(observationDetailMapper.selectList(null));
	}

}
