package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.entity.ObservationEntity;
import com.alibaba.cloud.ai.mapper.ObservationMapper;
import com.alibaba.cloud.ai.service.ObservationService;
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
public class ObservationServiceImpl implements ObservationService {

    @Resource
    private ObservationMapper observationMapper;

    @Override
    public List<ObservationEntity> list() {

        return observationMapper.selectList(null);
    }

    @Override
    public Integer insert(ObservationEntity ObservationEntity){
        return observationMapper.insert(ObservationEntity);
    }

    @Override
    public void exportObservation() {
        String fileName = "Observation_" + "simpleWrite_" + System.currentTimeMillis() + ".xlsx";
        EasyExcel.write(fileName, ObservationEntity.class).sheet("模板").doWrite(observationMapper.selectList(null));
    }
}
