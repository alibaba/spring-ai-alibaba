package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface EvaluatorMapper {

    /**
     * 创建评估器
     *
     * @param evaluator 评估器实体
     * @return 受影响的行数
     */
    int insert(EvaluatorDO evaluator);

    /**
     * 根据ID删除评估器（逻辑删除）
     *
     * @param id 评估器ID
     * @return 受影响的行数
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据ID获取评估器
     *
     * @param id 评估器ID
     * @return 评估器实体
     */
    EvaluatorDO selectById(@Param("id") Long id);

    /**
     * 分页查询评估器列表
     *
     * @param name 评估器名称（模糊查询）
     * @param offset 偏移量
     * @param limit 限制数量
     * @return 评估器列表
     */
    List<EvaluatorDO> selectList(@Param("name") String name, 
                                @Param("offset") long offset, 
                                @Param("limit") int limit);

    /**
     * 统计评估器数量
     *
     * @param name 评估器名称（模糊查询）
     * @return 评估器数量
     */
    int count(@Param("name") String name);

    /**
     * 更新评估器
     *
     * @param evaluator 评估器实体
     * @return 受影响的行数
     */
    int update(EvaluatorDO evaluator);
}