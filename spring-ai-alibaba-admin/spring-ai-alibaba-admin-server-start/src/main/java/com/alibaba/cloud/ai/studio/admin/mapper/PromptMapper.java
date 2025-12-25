package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.PromptDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface PromptMapper {

    /**
     * 创建Prompt
     *
     * @param prompt Prompt实体
     * @return 影响的行数
     */
    int insert(PromptDO prompt);

    /**
     * 根据Prompt Key删除Prompt（逻辑删除）
     *
     * @param promptKey Prompt Key
     * @return 影响的行数
     */
    int deleteByPromptKey(@Param("promptKey") String promptKey);

    /**
     * 根据Prompt Key获取Prompt
     *
     * @param promptKey Prompt Key
     * @return Prompt实体
     */
    PromptDO selectByPromptKey(@Param("promptKey") String promptKey);

    /**
     * 查询Prompt列表
     *
     * @param search    查询模式
     * @param tag       标签
     * @param promptKey Prompt Key
     * @param offset    偏移量
     * @param limit     数量限制
     * @return Prompt列表
     */
    List<PromptDO> selectList(@Param("search") String search,
                              @Param("tag") String tag,
                              @Param("promptKey") String promptKey,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 查询Prompt总数
     *
     * @param search    查询模式
     * @param tag       标签
     * @param promptKey Prompt Key
     * @return 总数
     */
    int selectCount(@Param("search") String search,
                    @Param("tag") String tag,
                    @Param("promptKey") String promptKey);

    /**
     * 更新Prompt
     *
     * @param prompt Prompt实体
     * @return 影响的行数
     */
    int update(PromptDO prompt);

    /**
     * 更新最新版本
     *
     * @param promptKey     Prompt Key
     * @param latestVersion 最新版本
     * @return 影响的行数
     */
    int updateLatestVersion(@Param("promptKey") String promptKey,
                            @Param("latestVersion") String latestVersion);

    /**
     * 根据Prompt Key获取Prompt及其最新版本状态
     *
     * @param promptKey Prompt Key
     * @return Map包含Prompt信息和最新版本状态
     */
    Map<String, Object> selectByPromptKeyWithLatestVersionStatus(@Param("promptKey") String promptKey);

    /**
     * 查询Prompt列表及其最新版本状态
     *
     * @param search    查询模式
     * @param tag       标签
     * @param promptKey Prompt Key
     * @param offset    偏移量
     * @param limit     数量限制
     * @return Map列表包含Prompt信息和最新版本状态
     */
    List<Map<String, Object>> selectListWithLatestVersionStatus(@Param("search") String search,
                                                                                     @Param("tag") String tag,
                                                                                     @Param("promptKey") String promptKey,
                                                                                     @Param("offset") int offset,
                                                                                     @Param("limit") int limit);
}
