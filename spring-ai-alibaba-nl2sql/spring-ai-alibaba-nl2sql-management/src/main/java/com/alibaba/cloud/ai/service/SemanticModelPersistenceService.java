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

import com.alibaba.cloud.ai.entity.SemanticModel;
import com.alibaba.cloud.ai.entity.SemanticModelDTO;
import com.alibaba.cloud.ai.mapper.SemanticModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SemanticModelPersistenceService {

	@Autowired
	private SemanticModelMapper semanticModelMapper;

	// Add agent field
	public void addField(SemanticModelDTO semanticModelDTO) {
		SemanticModel semanticModel = new SemanticModel();
		BeanUtils.copyProperties(semanticModelDTO, semanticModel);
		semanticModelMapper.insert(semanticModel);
	}

	// Batch add agent fields
	public void addFields(List<SemanticModelDTO> semanticModelDTOS) {
		for (SemanticModelDTO dto : semanticModelDTOS) {
			addField(dto);
		}
	}

	// Batch enable
	public void enableFields(List<Long> ids) {
		for (Long id : ids) {
			semanticModelMapper.enableById(id);
		}
	}

	// Batch disable
	public void disableFields(List<Long> ids) {
		for (Long id : ids) {
			semanticModelMapper.disableById(id);
		}
	}

	// Get semantic model by agent ID
	public List<SemanticModel> getFieldByAgentId(Long agentId) {
		return semanticModelMapper.selectByAgentId(agentId);
	}

	// Search
	public List<SemanticModel> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return semanticModelMapper.searchByKeyword(keyword);
	}

	// Delete agent field by id
	public void deleteFieldById(long id) {
		semanticModelMapper.deleteById(id);
	}

	// Update agent field
	public void updateField(SemanticModelDTO semanticModelDTO, long id) {
		SemanticModel semanticModel = new SemanticModel();
		BeanUtils.copyProperties(semanticModelDTO, semanticModel);
		semanticModel.setId(id);
		semanticModelMapper.updateById(semanticModel);
	}

}
