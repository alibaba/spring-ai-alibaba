package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.PromptVersion;
import com.alibaba.cloud.ai.studio.admin.dto.PromptVersionDetail;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptVersionCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptVersionListRequest;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;

public interface PromptVersionService {

    /**
     * 创建Prompt版本
     *
     * @param request 创建请求
     * @return Prompt版本
     */
    PromptVersion create(PromptVersionCreateRequest request) throws StudioException;

    /**
     * 根据Prompt Key和版本获取Prompt版本详情
     *
     * @param promptKey Prompt Key
     * @param version   版本号
     * @return Prompt版本详情
     */
    PromptVersionDetail getByPromptKeyAndVersion(String promptKey, String version) throws StudioException;

    /**
     * 分页查询Prompt版本列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<PromptVersion> list(PromptVersionListRequest request);
}
