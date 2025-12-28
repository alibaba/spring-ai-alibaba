package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.PromptTemplate;
import com.alibaba.cloud.ai.studio.admin.dto.PromptTemplateDetail;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptTemplateListRequest;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;

public interface PromptTemplateService {

    /**
     * 根据模板Key获取Prompt模板详情
     *
     * @param promptTemplateKey 模板Key
     * @return Prompt模板详情
     */
    PromptTemplateDetail getByPromptTemplateKey(String promptTemplateKey) throws StudioException;

    /**
     * 分页查询Prompt模板列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<PromptTemplate> list(PromptTemplateListRequest request) throws StudioException;
}
