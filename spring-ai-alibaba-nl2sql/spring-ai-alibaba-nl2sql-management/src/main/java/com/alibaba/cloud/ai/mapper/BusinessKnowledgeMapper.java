/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.mapper;

import com.alibaba.cloud.ai.entity.BusinessKnowledge;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 业务知识 Mapper 接口
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface BusinessKnowledgeMapper extends BaseMapper<BusinessKnowledge> {

	/**
	 * 根据数据集ID查询业务知识列表
	 */
	@Select("SELECT * FROM business_knowledge WHERE dataset_id = #{datasetId} ORDER BY created_time DESC")
	List<BusinessKnowledge> selectByDatasetId(@Param("datasetId") String datasetId);

	/**
	 * 获取所有数据集ID列表
	 */
	@Select("SELECT DISTINCT dataset_id FROM business_knowledge WHERE dataset_id IS NOT NULL ORDER BY dataset_id")
	List<String> selectDistinctDatasetIds();

	/**
	 * 根据关键词搜索业务知识
	 */
	@Select("SELECT * FROM business_knowledge WHERE " + "business_term LIKE CONCAT('%', #{keyword}, '%') OR "
			+ "description LIKE CONCAT('%', #{keyword}, '%') OR " + "synonyms LIKE CONCAT('%', #{keyword}, '%') "
			+ "ORDER BY created_time DESC")
	List<BusinessKnowledge> searchByKeyword(@Param("keyword") String keyword);

	/**
	 * 根据智能体ID查询业务知识列表
	 */
	@Select("SELECT * FROM business_knowledge WHERE agent_id = #{agentId} ORDER BY created_time DESC")
	List<BusinessKnowledge> selectByAgentId(@Param("agentId") String agentId);

	/**
	 * 根据数据集ID和默认召回状态查询业务知识
	 */
	@Select("SELECT * FROM business_knowledge WHERE dataset_id = #{datasetId} AND default_recall = #{defaultRecall} ORDER BY created_time DESC")
	List<BusinessKnowledge> selectByDatasetIdAndDefaultRecall(@Param("datasetId") String datasetId,
			@Param("defaultRecall") Boolean defaultRecall);

}
