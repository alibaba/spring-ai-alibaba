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

	// Add agent field
	public void addKnowledge(BusinessKnowledgeDTO knowledgeDTO) {
		BusinessKnowledge knowledge = new BusinessKnowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		businessKnowledgeMapper.insert(knowledge);
	}

	// Batch add agent fields
	public void addKnowledgeList(List<BusinessKnowledgeDTO> knowledgeDTOList) {
		for (BusinessKnowledgeDTO dto : knowledgeDTOList) {
			addKnowledge(dto);
		}
	}

	// Get dataset list
	public List<String> getDataSetIds() {
		return businessKnowledgeMapper.selectDistinctDatasetIds();
	}

	// Get agent fields by data_set_id
	public List<BusinessKnowledge> getFieldByDataSetId(String dataSetId) {
		return businessKnowledgeMapper.selectByDatasetId(dataSetId);
	}

	// Search
	public List<BusinessKnowledge> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return businessKnowledgeMapper.searchByKeyword(keyword);
	}

	// Delete agent field by id
	public void deleteFieldById(long id) {
		businessKnowledgeMapper.deleteById(id);
	}

	// Update agent field
	public void updateField(BusinessKnowledgeDTO knowledgeDTO, long id) {
		BusinessKnowledge knowledge = new BusinessKnowledge();
		BeanUtils.copyProperties(knowledgeDTO, knowledge);
		knowledge.setId(id);
		businessKnowledgeMapper.updateById(knowledge);
	}

	// Get business knowledge list by agent ID
	public List<BusinessKnowledge> getKnowledgeByAgentId(String agentId) {
		return businessKnowledgeMapper.selectByAgentId(agentId);
	}

	// Delete all business knowledge by agent ID
	public void deleteKnowledgeByAgentId(String agentId) {
		businessKnowledgeMapper.delete(com.baomidou.mybatisplus.core.toolkit.Wrappers.<BusinessKnowledge>lambdaQuery()
			.eq(BusinessKnowledge::getAgentId, agentId));
	}

	// Search business knowledge within agent scope
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
