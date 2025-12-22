package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.Experiment;
import com.alibaba.cloud.ai.studio.admin.dto.ExperimentEvaluatorResult;
import com.alibaba.cloud.ai.studio.admin.dto.ExperimentEvaluatorResultDetail;
import com.alibaba.cloud.ai.studio.admin.dto.request.ExperimentCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ExperimentEvaluatorResultDetailListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ExperimentListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorExperimentsListRequest;

import java.util.List;
import java.util.Map;

public interface ExperimentService {

    /**
     * 创建实验
     */
    Experiment create(ExperimentCreateRequest request);

    /**
     * 分页查询实验列表
     */
    PageResult<Experiment> list(ExperimentListRequest request);

    /**
     * 根据ID获取实验
     */
    Experiment getById(Long id);


    List<ExperimentEvaluatorResult> getResults(Long ExperimentId);


    PageResult<ExperimentEvaluatorResultDetail> getResult(ExperimentEvaluatorResultDetailListRequest request);



    /**
     * 停止实验
     */
    Experiment stop(Long id);

    /**
     * 根据ID删除实验
     */
    void deleteById(Long id);


    /**
     * 根据ID删除实验
     */
    void restartById(Long id);

    /**
     * 获取使用指定评估器的实验列表
     *
     * @param request 查询请求
     * @return 分页的实验结果
     */
    PageResult<Experiment> getExperimentsByEvaluator(EvaluatorExperimentsListRequest request);
}