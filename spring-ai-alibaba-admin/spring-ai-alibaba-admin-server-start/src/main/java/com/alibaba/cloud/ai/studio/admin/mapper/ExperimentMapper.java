package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.ExperimentDO;
import com.alibaba.cloud.ai.studio.admin.entity.ExperimentResultDO;
import com.alibaba.cloud.ai.studio.admin.enums.ExperimentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ExperimentMapper {

    /**
     * 创建实验
     *
     * @param experiment 实验实体
     * @return 受影响的行数
     */
    int insert(ExperimentDO experiment);

    /**
     * 根据ID删除实验
     *
     * @param id 实验ID
     * @return 受影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID获取实验
     *
     * @param id 实验ID
     * @return 实验实体
     */
    ExperimentDO selectById(@Param("id") Long id);

    /**
     * 分页查询实验列表
     *
     * @param name 实验名称（模糊查询）
     * @param status 实验状态
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 实验列表
     */
    List<ExperimentDO> selectList(@Param("name") String name,
                                  @Param("status") ExperimentStatus status,
                                  @Param("offset") long offset,
                                  @Param("limit") int limit);

    /**
     * 统计实验数量
     *
     * @param name 实验名称（模糊查询）
     * @param status 实验状态
     * @return 实验数量
     */
    int count(@Param("name") String name,
              @Param("status") ExperimentStatus status);

    /**
     * 根据ID更新实验
     *
     * @param experiment 实验实体
     * @return 受影响的行数
     */
    int updateById(ExperimentDO experiment);

    /**
     * 创建实验结果
     *
     * @param experimentResult 实验结果实体
     * @return 受影响的行数
     */
    int insertResult(ExperimentResultDO experimentResult);


    /**
     * 根据datasetID获取实验结果
     *
     * @param datasetId 实验结果ID
     * @return 实验结果实体
     */
    List<ExperimentDO> selectByDatasetId(@Param("datasetId") Long datasetId,
                                               @Param("offset") long offset,
                                               @Param("limit") int limit);

    int selectCountByDatasetId(@Param("datasetId") Long datasetId);

    /**
     * 根据评估器ID分页查询实验列表
     *
     * @param evaluatorId 评估器ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 实验列表
     */
    List<ExperimentDO> selectByEvaluatorId(@Param("evaluatorId") Long evaluatorId,
                                           @Param("offset") long offset,
                                           @Param("limit") int limit);

    /**
     * 根据评估器版本ID分页查询实验列表
     *
     * @param evaluatorVersionId 评估器版本ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 实验列表
     */
    List<ExperimentDO> selectByEvaluatorVersionId(@Param("evaluatorVersionId") Long evaluatorVersionId,
                                                  @Param("offset") long offset,
                                                  @Param("limit") int limit);

    /**
     * 统计使用指定评估器的实验数量
     *
     * @param evaluatorId 评估器ID
     * @return 实验数量
     */
    int selectCountByEvaluatorId(@Param("evaluatorId") Long evaluatorId);

    /**
     * 统计使用指定评估器版本的实验数量
     *
     * @param evaluatorVersionId 评估器版本ID
     * @return 实验数量
     */
    int selectCountByEvaluatorVersionId(@Param("evaluatorVersionId") Long evaluatorVersionId);

}