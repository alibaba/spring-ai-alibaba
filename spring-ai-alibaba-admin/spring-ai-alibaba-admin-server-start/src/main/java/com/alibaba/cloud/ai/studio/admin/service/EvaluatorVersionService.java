package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorVersion;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorVersionCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorVersionListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorVersionUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorVersionDO;

public interface EvaluatorVersionService {

    /**
     * 创建评估器版本
     */
    EvaluatorVersion create(EvaluatorVersionCreateRequest request);

    /**
     * 分页查询评估器列表
     */
    PageResult<EvaluatorVersion>list(EvaluatorVersionListRequest request);

    /**
     * 根据ID获取评估器版本
     */
    EvaluatorVersion getById(Long id);

    /**
     * 更新评估器版本
     */
    EvaluatorVersion update(EvaluatorVersionUpdateRequest request);



} 