package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.ExperimentResultDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface ExperimentResultMapper {

    /**
     * 批量创建实验结果
     *
     * @param experimentResults 实验结果实体列表
     * @return 受影响的行数
     */
    int batchInsert(@Param("experimentResults") List<ExperimentResultDO> experimentResults);

    /**
     * 根据实验ID删除实验结果
     *
     * @param experimentId 实验ID
     * @return 受影响的行数
     */
    int deleteByExperimentId(@Param("experimentId") Long experimentId);

    /**
     * 根据ID获取实验结果
     *
     * @param id 实验结果ID
     * @return 实验结果实体
     */
    ExperimentResultDO selectById(@Param("id") Long id);



    /**
     * 根据实验ID获取实验结果数量
     *
     * @param experimentId 实验ID
     * @return 实验结果数量
     */
    int selectCountByExperimentIdAndEvaluator(@Param("experimentId") Long experimentId,
                                              @Param("evaluatorVersionId") Long evaluatorVersionId);

    /**
     * 查询实验结果
     *
     * @param experimentId 实验ID
     * @return 实验结果列表
     */
    List<ExperimentResultDO> selectByExperimentAndEvaluator(
            @Param("experimentId") Long experimentId,
            @Param("evaluatorVersionId") Long evaluatorVersionId);



    /**
     * 分页查询实验结果
     *
     * @param experimentId 实验ID
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 实验结果列表
     */
    List<ExperimentResultDO> selectByExperimentAndEvaluatorWithPageble(
            @Param("experimentId") Long experimentId,
            @Param("evaluatorVersionId") Long evaluatorVersionId,
            @Param("offset") long offset,
            @Param("limit") int limit);




    /**
     * 根据ID更新实验结果
     *
     * @param experimentResult 实验结果实体
     * @return 受影响的行数
     */
    int updateById(ExperimentResultDO experimentResult);


}