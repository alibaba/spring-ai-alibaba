package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.PromptTemplate;
import com.alibaba.cloud.ai.studio.admin.dto.PromptTemplateDetail;
import com.alibaba.cloud.ai.studio.admin.dto.request.PromptTemplateListRequest;
import com.alibaba.cloud.ai.studio.admin.entity.PromptTemplateDO;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.mapper.PromptTemplateMapper;
import com.alibaba.cloud.ai.studio.admin.service.PromptTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromptTemplateServiceImpl implements PromptTemplateService {

    private final PromptTemplateMapper promptTemplateMapper;

    @Override
    public PromptTemplateDetail getByPromptTemplateKey(String promptTemplateKey) throws StudioException {
        log.info("查询Prompt模板详情: {}", promptTemplateKey);

        PromptTemplateDO promptTemplateDO = promptTemplateMapper.selectByPromptTemplateKey(promptTemplateKey);
        if (promptTemplateDO == null) {
            throw new StudioException(StudioException.NOT_FOUND, "Prompt模板不存在: " + promptTemplateKey);
        }
        return PromptTemplateDetail.fromDO(promptTemplateDO);
    }

    @Override
    public PageResult<PromptTemplate> list(PromptTemplateListRequest request) throws StudioException {
        log.info("查询Prompt模板列表: {}", request);
        
        // 验证搜索模式参数
        if (request.getSearch() != null && 
            !"accurate".equals(request.getSearch()) &&
            !"blur".equals(request.getSearch())) {
            throw new StudioException(StudioException.INVALID_PARAM, "搜索模式必须是accurate或blur");
        }

        int offset = (request.getPageNo() - 1) * request.getPageSize();

        List<PromptTemplateDO> promptTemplateDOList = promptTemplateMapper.selectList(
                request.getSearch(),
                request.getTag(),
                request.getPromptTemplateKey(),
                offset,
                request.getPageSize()
        );

        int totalCount = promptTemplateMapper.selectCount(
                request.getSearch(),
                request.getTag(),
                request.getPromptTemplateKey()
        );

        List<PromptTemplate> promptTemplateList = promptTemplateDOList.stream()
                .map(PromptTemplate::fromDO)
                .collect(Collectors.toList());

        return new PageResult<>((long) totalCount, (long) request.getPageNo(), (long) request.getPageSize(), promptTemplateList);
    }
}
