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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.BusinessKnowledge;
import com.alibaba.cloud.ai.entity.BusinessKnowledgeDTO;
import com.alibaba.cloud.ai.mapper.BusinessKnowledgeMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class BusinessKnowledgePersistenceService {

	@Autowired
	private BusinessKnowledgeMapper businessKnowledgeMapper;

	// 新增智能体字段
	public void addKnowledge(BusinessKnowledgeDTO knowledgeDTO) {
		BusinessKnowledge knowledge = new BusinessKnowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		businessKnowledgeMapper.insert(knowledge);
	}

	// 批量新增智能体字段
	public void addKnowledgeList(List<BusinessKnowledgeDTO> knowledgeDTOList) {
		for (BusinessKnowledgeDTO dto : knowledgeDTOList) {
			addKnowledge(dto);
		}
	}

	// 获取数据集列表
	public List<String> getDataSetIds() {
		return businessKnowledgeMapper.selectDistinctDatasetIds();
	}

	// 根据data_set_id获取智能体字段
	public List<BusinessKnowledge> getFieldByDataSetId(String dataSetId) {
		return businessKnowledgeMapper.selectByDatasetId(dataSetId);
	}

	// 搜索
	public List<BusinessKnowledge> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return businessKnowledgeMapper.searchByKeyword(keyword);
	}

	// 根据id删除智能体字段
	public void deleteFieldById(long id) {
		businessKnowledgeMapper.deleteById(id);
	}

	// 更新智能体字段
	public void updateField(BusinessKnowledgeDTO knowledgeDTO, long id) {
		BusinessKnowledge knowledge = new BusinessKnowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		knowledge.setId(id);
		businessKnowledgeMapper.updateById(knowledge);
	}

	// 根据智能体ID获取业务知识列表
	public List<BusinessKnowledge> getKnowledgeByAgentId(String agentId) {
		return businessKnowledgeMapper.selectByAgentId(agentId);
	}

	// 根据智能体ID删除所有业务知识
	public void deleteKnowledgeByAgentId(String agentId) {
		businessKnowledgeMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<BusinessKnowledge>lambdaQuery()
			.eq(BusinessKnowledge::getAgentId, agentId));
	}

	// 在智能体范围内搜索业务知识
	public List<BusinessKnowledge> searchKnowledgeInAgent(String agentId, String keyword) {
		Objects.requireNonNull(agentId, "agentId cannot be null");
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return businessKnowledgeMapper
			.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<BusinessKnowledge>lambdaQuery()
				.eq(BusinessKnowledge::getAgentId, agentId)
				.and(wrapper -> wrapper.like(BusinessKnowledge::getBusinessTerm, keyword)
					.or()
					.like(BusinessKnowledge::getDescription, keyword)
					.or()
					.like(BusinessKnowledge::getSynonyms, keyword)));
	}

}
