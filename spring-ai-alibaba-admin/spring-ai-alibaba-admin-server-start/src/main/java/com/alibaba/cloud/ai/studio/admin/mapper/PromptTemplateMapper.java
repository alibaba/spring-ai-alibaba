package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.PromptTemplateDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromptTemplateMapper {

    /**
     * 根据模板Key获取Prompt模板
     *
     * @param promptTemplateKey 模板Key
     * @return Prompt模板实体
     */
    PromptTemplateDO selectByPromptTemplateKey(@Param("promptTemplateKey") String promptTemplateKey);

    /**
     * 查询Prompt模板列表
     *
     * @param search            查询模式
     * @param tag               标签
     * @param promptTemplateKey 模板Key
     * @param offset            偏移量
     * @param limit             数量限制
     * @return Prompt模板列表
     */
    List<PromptTemplateDO> selectList(@Param("search") String search,
                                      @Param("tag") String tag,
                                      @Param("promptTemplateKey") String promptTemplateKey,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /**
     * 查询Prompt模板总数
     *
     * @param search            查询模式
     * @param tag               标签
     * @param promptTemplateKey 模板Key
     * @return 总数
     */
    int selectCount(@Param("search") String search,
                    @Param("tag") String tag,
                    @Param("promptTemplateKey") String promptTemplateKey);
}
