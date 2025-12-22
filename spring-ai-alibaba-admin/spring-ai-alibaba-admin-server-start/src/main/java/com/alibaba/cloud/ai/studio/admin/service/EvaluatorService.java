package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorDebugResult;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorTestRequest;
import com.alibaba.cloud.ai.studio.admin.dto.Evaluator;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorUpdateRequest;

import java.util.Map;

public interface EvaluatorService {

    /**
     * 创建评估器
     */
    Evaluator create(EvaluatorCreateRequest request);

    /**
     * 分页查询评估器列表
     */
    PageResult<Evaluator> list(EvaluatorListRequest request);

    /**
     * 根据ID获取评估器
     */
    Evaluator getById(Long id);

    /**
     * 更新评估器
     */
    Evaluator update(EvaluatorUpdateRequest request);

    /**
     * 根据ID删除评估器
     */
    void deleteById(Long id);

    /**
     * 调试评估器
     */
    EvaluatorDebugResult debug(EvaluatorTestRequest request);


} 