package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorTemplate;
import com.alibaba.cloud.ai.studio.admin.dto.request.EvaluatorTemplateListRequest;
import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorTemplateDO;
import com.alibaba.cloud.ai.studio.admin.mapper.EvaluatorTemplateMapper;

import com.alibaba.cloud.ai.studio.admin.service.EvaluatorTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluatorTemplateServiceImpl implements EvaluatorTemplateService {
    
    private final EvaluatorTemplateMapper evaluatorPromptTemplateMapper;


    @Override
    public EvaluatorTemplate get(Long id) {
        EvaluatorTemplateDO templateDO = evaluatorPromptTemplateMapper.selectById(id);
        return EvaluatorTemplate.fromDO(templateDO);
    }

    @Override
    public PageResult<EvaluatorTemplate> list(EvaluatorTemplateListRequest request) {


        // 计算偏移量
        long offset = (request.getPageNumber() - 1L) * request.getPageSize();
        
        // 查询数据
        List<EvaluatorTemplateDO> templateDOList = evaluatorPromptTemplateMapper.list(offset, (long) request.getPageSize());
        
        // 获取总数
        int totalCount = evaluatorPromptTemplateMapper.count();

        return new PageResult<>(
                (long) totalCount,
                (long) request.getPageNumber(),
                (long)  request.getPageSize(),
                templateDOList.stream()
                        .map(EvaluatorTemplate::fromDO)
                        .toList());
    }
    

}