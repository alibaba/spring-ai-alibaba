package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.Prompt;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;

public interface PromptService {

    /**
     * 创建Prompt
     *
     * @param request 创建请求
     * @return Prompt
     */
    Prompt create(PromptCreateRequest request) throws StudioException;

    /**
     * 根据Prompt Key获取Prompt
     *
     * @param promptKey Prompt Key
     * @return Prompt
     */
    Prompt getByPromptKey(String promptKey) throws StudioException;

    /**
     * 分页查询Prompt列表
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<Prompt> list(PromptListRequest request) throws StudioException;

    /**
     * 更新Prompt
     *
     * @param request 更新请求
     * @return Prompt
     */
    Prompt update(PromptUpdateRequest request) throws StudioException;

    /**
     * 根据Prompt Key删除Prompt
     *
     * @param promptKey Prompt Key
     */
    void deleteByPromptKey(String promptKey) throws StudioException;

    /**
     * 更新最新版本
     *
     * @param promptKey     Prompt Key
     * @param latestVersion 最新版本
     */
    void updateLatestVersion(String promptKey, String latestVersion);
}
