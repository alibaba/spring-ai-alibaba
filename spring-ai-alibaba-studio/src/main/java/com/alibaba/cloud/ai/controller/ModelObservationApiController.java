package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.entity.ModelObservationEntity;
import com.alibaba.cloud.ai.service.ModelObservationService;
import com.alibaba.cloud.ai.service.impl.ModelObservationServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @Description:
 * @Author: XiaoYunTao
 * @Date: 2024/11/19
 */
@CrossOrigin
@RestController
@RequestMapping("studio/api/model_observation")
public class ModelObservationApiController {

    private final ModelObservationServiceImpl modelObservationServiceImpl;

    public ModelObservationApiController(ModelObservationServiceImpl modelObservationServiceImpl) {
        this.modelObservationServiceImpl = modelObservationServiceImpl;
    }

    @GetMapping(value = "list", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public R<List<ModelObservationEntity>> list() {
        List<ModelObservationEntity> list = modelObservationServiceImpl.list();
        return R.success(list);
    }

}
