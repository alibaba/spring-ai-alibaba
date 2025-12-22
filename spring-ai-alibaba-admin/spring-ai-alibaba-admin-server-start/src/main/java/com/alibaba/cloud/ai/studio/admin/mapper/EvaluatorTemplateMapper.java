package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EvaluatorTemplateMapper {


    EvaluatorTemplateDO selectById(Long id);
    
    List<EvaluatorTemplateDO> list(@Param("offset") Long offset, @Param("limit") Long limit);
    
    int count();
}