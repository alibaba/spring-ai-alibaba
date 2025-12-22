package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorTemplate;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorTemplateListRequest;

public interface EvaluatorTemplateService {


    
    EvaluatorTemplate get(Long id);
    

    PageResult<EvaluatorTemplate> list(EvaluatorTemplateListRequest request);
}