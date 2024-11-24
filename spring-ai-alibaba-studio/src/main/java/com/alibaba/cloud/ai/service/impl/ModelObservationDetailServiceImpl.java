package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.entity.ModelObservationDetailEntity;
import com.alibaba.cloud.ai.mapper.ModelObservationDetailMapper;
import com.alibaba.cloud.ai.service.ModelObservationDetailService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
@Service
public class ModelObservationDetailServiceImpl implements ModelObservationDetailService {

    @Resource
    private ModelObservationDetailMapper modelObservationDetailMapper;

    @Override
    public List<ModelObservationDetailEntity> list() {

        return modelObservationDetailMapper.selectList(null);
    }

    @Override
    public Integer insert(ModelObservationDetailEntity modelObservationDetailEntity) {
        return modelObservationDetailMapper.insert(modelObservationDetailEntity);
    }
}
