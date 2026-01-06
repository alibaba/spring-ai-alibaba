package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.Prompt;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.PromptDO;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.mapper.PromptMapper;
import com.alibaba.cloud.ai.studio.admin.mapper.PromptVersionMapper;
import com.alibaba.cloud.ai.studio.admin.service.PromptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptServiceImpl implements PromptService {
    
    private final PromptMapper promptMapper;
    
    private final PromptVersionMapper promptVersionMapper;
    
    /**
     * 从Map创建Prompt对象
     */
    private Prompt createPromptFromMap(Map<String, Object> map) {
        if (map == null) {
            return null;
        }
        
        return Prompt.builder().promptKey((String) map.get("prompt_key"))
                .promptDescription((String) map.get("prompt_desc")).latestVersion((String) map.get("latest_version"))
                .latestVersionStatus((String) map.get("latest_version_status")).tags((String) map.get("tags"))
                .createTime(map.get("create_time") != null ? ((LocalDateTime) map.get("create_time")).atZone(
                        java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null).updateTime(
                        map.get("update_time") != null ? ((LocalDateTime) map.get("update_time")).atZone(
                                java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() : null).build();
    }
    
    @Override
    @Transactional
    public Prompt create(PromptCreateRequest request) throws StudioException {
        log.info("创建Prompt: {}", request);
        
        // 检查Prompt Key是否已存在
        PromptDO existingPrompt = promptMapper.selectByPromptKey(request.getPromptKey());
        if (existingPrompt != null) {
            throw new StudioException(StudioException.CONFLICT, "Prompt Key已存在: " + request.getPromptKey());
        }
        
        PromptDO promptDO = PromptDO.builder().promptKey(request.getPromptKey())
                .promptDesc(request.getPromptDescription()).tags(request.getTags()).build();
        
        promptMapper.insert(promptDO);
        log.info("Prompt创建成功: {}", promptDO.getId());
        
        return Prompt.fromDO(promptDO);
    }
    
    @Override
    public Prompt getByPromptKey(String promptKey) throws StudioException {
        log.info("查询Prompt详情: {}", promptKey);
        
        Map<String, Object> promptMap = promptMapper.selectByPromptKeyWithLatestVersionStatus(promptKey);
        if (promptMap == null) {
            throw new StudioException(StudioException.NOT_FOUND, "Prompt不存在: " + promptKey);
        }
        
        return createPromptFromMap(promptMap);
    }
    
    @Override
    public PageResult<Prompt> list(PromptListRequest request) throws StudioException {
        log.info("查询Prompt列表: {}", request);
        
        // 验证搜索模式参数
        if (request.getSearch() != null && !"accurate".equals(request.getSearch()) && !"blur".equals(
                request.getSearch())) {
            throw new StudioException(StudioException.INVALID_PARAM, "搜索模式必须是accurate或blur");
        }
        
        int offset = (request.getPageNo() - 1) * request.getPageSize();
        
        List<Map<String, Object>> promptMapList = promptMapper.selectListWithLatestVersionStatus(request.getSearch(),
                request.getTag(), request.getPromptKey(), offset, request.getPageSize());
        
        int totalCount = promptMapper.selectCount(request.getSearch(), request.getTag(), request.getPromptKey());
        
        List<Prompt> promptList = promptMapList.stream().map(this::createPromptFromMap).collect(Collectors.toList());
        
        return new PageResult<>((long) totalCount, (long) request.getPageNo(), (long) request.getPageSize(),
                promptList);
    }
    
    @Override
    @Transactional
    public Prompt update(PromptUpdateRequest request) throws StudioException {
        log.info("更新Prompt: {}", request);
        
        // 检查Prompt是否存在
        PromptDO existingPrompt = promptMapper.selectByPromptKey(request.getPromptKey());
        if (existingPrompt == null) {
            throw new StudioException(StudioException.NOT_FOUND, "Prompt不存在: " + request.getPromptKey());
        }
        
        PromptDO promptDO = PromptDO.builder().promptKey(request.getPromptKey())
                .promptDesc(request.getPromptDescription()).tags(request.getTags()).build();
        
        promptMapper.update(promptDO);
        log.info("Prompt更新成功: {}", request.getPromptKey());
        
        return getByPromptKey(request.getPromptKey());
    }
    
    @Override
    @Transactional
    public void deleteByPromptKey(String promptKey) throws StudioException {
        log.info("删除Prompt及其所有版本: {}", promptKey);
        
        // 检查Prompt是否存在
        PromptDO existingPrompt = promptMapper.selectByPromptKey(promptKey);
        if (existingPrompt == null) {
            log.info("Prompt不存在，无需删除: {}", promptKey);
            return;
        }
        
        // 先删除所有版本
        int deletedVersionsCount = promptVersionMapper.deleteByPromptKey(promptKey);
        log.info("Prompt {} 的所有版本删除完成，共删除 {} 个版本", promptKey, deletedVersionsCount);
        
        // 再删除Prompt本身
        promptMapper.deleteByPromptKey(promptKey);
        log.info("Prompt删除成功: {}", promptKey);
    }
    
    @Override
    @Transactional
    public void updateLatestVersion(String promptKey, String latestVersion) {
        log.info("更新Prompt最新版本: promptKey={}, latestVersion={}", promptKey, latestVersion);
        
        promptMapper.updateLatestVersion(promptKey, latestVersion);
        log.info("Prompt最新版本更新成功");
    }
}
