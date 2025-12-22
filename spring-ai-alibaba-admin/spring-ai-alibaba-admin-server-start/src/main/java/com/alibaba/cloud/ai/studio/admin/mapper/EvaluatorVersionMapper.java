package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorVersionDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface EvaluatorVersionMapper {

    /**
     * 创建评估器版本
     *
     * @param evaluatorVersion 评估器版本实体
     * @return 受影响的行数
     */
    int insert(EvaluatorVersionDO evaluatorVersion);

    /**
     * 根据ID删除评估器版本（逻辑删除）
     *
     * @param id 评估器版本ID
     * @return 受影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID获取评估器版本
     *
     * @param id 评估器版本ID
     * @return 评估器版本实体
     */
    EvaluatorVersionDO selectById(@Param("id") Long id);

    /**
     * 根据评估器ID获取评估器版本列表
     *
     * @param evaluatorId 评估器ID
     * @param name 评估器版本名称（模糊查询）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 评估器版本列表
     */
    List<EvaluatorVersionDO> selectListByEvaluatorId(@Param("evaluatorId") Long evaluatorId,
                                                     @Param("name") String name,
                                                     @Param("offset") long offset,
                                                     @Param("limit") int limit);


    /**
     * 根据评估器ID获取评估器版本列表
     *
     * @param evaluatorId 评估器ID
     * @return 评估器版本列表
     */
    EvaluatorVersionDO selectLatestVersionByEvaluatorId(@Param("evaluatorId") Long evaluatorId);

    /**
     * 根据评估器ID统计评估器版本数量
     *
     * @param evaluatorId 评估器ID
     * @param name 评估器版本名称（模糊查询）
     * @return 评估器版本数量
     */
    int countByEvaluatorId(@Param("evaluatorId") Long evaluatorId,
                           @Param("name") String name);



    int update(@Param("id") Long id,
               @Param("description") String description,
               @Param("status") String status);


}