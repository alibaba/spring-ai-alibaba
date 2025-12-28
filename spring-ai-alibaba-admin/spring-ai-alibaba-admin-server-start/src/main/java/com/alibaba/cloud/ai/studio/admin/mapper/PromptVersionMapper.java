package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.PromptVersionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PromptVersionMapper {

    /**
     * 创建Prompt版本
     *
     * @param promptVersion Prompt版本实体
     * @return 影响的行数
     */
    int insert(PromptVersionDO promptVersion);

    /**
     * 根据Prompt Key和版本获取Prompt版本
     *
     * @param promptKey Prompt Key
     * @param version   版本号
     * @return Prompt版本实体
     */
    PromptVersionDO selectByPromptKeyAndVersion(@Param("promptKey") String promptKey,
                                                @Param("version") String version);

    /**
     * 查询Prompt版本列表
     *
     * @param promptKey Prompt Key
     * @param status    版本状态
     * @param offset    偏移量
     * @param limit     数量限制
     * @return Prompt版本列表
     */
    List<PromptVersionDO> selectListByPromptKey(@Param("promptKey") String promptKey,
                                                @Param("status") String status,
                                                @Param("offset") int offset,
                                                @Param("limit") int limit);

    /**
     * 查询Prompt版本总数
     *
     * @param promptKey Prompt Key
     * @param status    版本状态
     * @return 总数
     */
    int selectCountByPromptKey(@Param("promptKey") String promptKey,
                               @Param("status") String status);

    /**
     * 获取最新版本号
     *
     * @param promptKey Prompt Key
     * @return 最新版本号
     */
    String selectLatestVersion(@Param("promptKey") String promptKey);

    /**
     * 检查版本是否存在
     *
     * @param promptKey Prompt Key
     * @param version   版本号
     * @return 是否存在
     */
    boolean existsByPromptKeyAndVersion(@Param("promptKey") String promptKey,
                                        @Param("version") String version);

    /**
     * 根据Prompt Key和版本获取状态
     *
     * @param promptKey Prompt Key
     * @param version   版本号
     * @return 版本状态
     */
    String selectStatusByPromptKeyAndVersion(@Param("promptKey") String promptKey,
                                           @Param("version") String version);

    /**
     * 更新Prompt版本
     *
     * @param promptVersion Prompt版本实体
     * @return 影响的行数
     */
    int updateByPromptKeyAndVersion(PromptVersionDO promptVersion);

    /**
     * 根据Prompt Key删除所有版本
     *
     * @param promptKey Prompt Key
     * @return 影响的行数
     */
    int deleteByPromptKey(@Param("promptKey") String promptKey);
}
