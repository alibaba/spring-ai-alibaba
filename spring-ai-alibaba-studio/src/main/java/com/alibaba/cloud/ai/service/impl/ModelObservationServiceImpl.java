package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.entity.ModelObservationEntity;
import com.alibaba.cloud.ai.mapper.ModelObservationMapper;
import com.alibaba.cloud.ai.service.ModelObservationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
@Service
public class ModelObservationServiceImpl implements ModelObservationService {

    @Resource
    private ModelObservationMapper modelObservationMapper;

    @Override
    public List<ModelObservationEntity> list() {

        return modelObservationMapper.selectList(null);
    }


}
