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

	// 新增智能体字段
	public void addField(SemanticModelDTO semanticModelDTO) {
		SemanticModel semanticModel = new SemanticModel();
		BeanUtils.copyProperties(semanticModelDTO, semanticModel);
		semanticModelMapper.insert(semanticModel);
	}

	// 批量新增智能体字段
	public void addFields(List<SemanticModelDTO> semanticModelDTOS) {
		for (SemanticModelDTO dto : semanticModelDTOS) {
			addField(dto);
		}
	}

	// 批量启用
	public void enableFields(List<Long> ids) {
		for (Long id : ids) {
			semanticModelMapper.enableById(id);
		}
	}

	// 批量禁用
	public void disableFields(List<Long> ids) {
		for (Long id : ids) {
			semanticModelMapper.disableById(id);
		}
	}

	// 根据智能体ID获取语义模型
	public List<SemanticModel> getFieldByAgentId(Long agentId) {
		return semanticModelMapper.selectByAgentId(agentId);
	}

	// 搜索
	public List<SemanticModel> searchFields(String keyword) {
		Objects.requireNonNull(keyword, "searchKeyword cannot be null");
		return semanticModelMapper.searchByKeyword(keyword);
	}

	// 根据id删除智能体字段
	public void deleteFieldById(long id) {
		semanticModelMapper.deleteById(id);
	}

	// 更新智能体字段
	public void updateField(SemanticModelDTO semanticModelDTO, long id) {
		SemanticModel semanticModel = new SemanticModel();
		BeanUtils.copyProperties(semanticModelDTO, semanticModel);
		semanticModel.setId(id);
		semanticModelMapper.updateById(semanticModel);
	}

}
