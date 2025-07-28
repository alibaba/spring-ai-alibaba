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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.entity.ApiResponse;
import com.alibaba.cloud.ai.entity.BusinessKnowledge;
import com.alibaba.cloud.ai.entity.BusinessKnowledgeDTO;
import com.alibaba.cloud.ai.service.BusinessKnowledgePersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class BusinessKnowledgePersistenceController {

	private final BusinessKnowledgePersistenceService businessKnowledgePersistenceService;

	public BusinessKnowledgePersistenceController(
			BusinessKnowledgePersistenceService businessKnowledgePersistenceService) {
		this.businessKnowledgePersistenceService = businessKnowledgePersistenceService;
	}

	// 新增
	@PostMapping("/add")
	public ResponseEntity<ApiResponse> addField(@RequestBody BusinessKnowledgeDTO knowledgeDTO) {
		businessKnowledgePersistenceService.addKnowledge(knowledgeDTO);
		return ResponseEntity.ok(ApiResponse.success("业务知识添加成功"));
	}

	@PostMapping("/addList")
	public ResponseEntity<ApiResponse> addFields(@RequestBody List<BusinessKnowledgeDTO> knowledgeDTOs) {
		businessKnowledgePersistenceService.addKnowledgeList(knowledgeDTOs);
		return ResponseEntity.ok(ApiResponse.success("批量业务知识添加成功"));
	}

	// 获取数据集id列表
	@GetMapping("/datasetIds")
	public ResponseEntity<List<String>> getDataSetIds() {
		List<String> datasetIds = businessKnowledgePersistenceService.getDataSetIds();
		return ResponseEntity.ok(datasetIds);
	}

	// 根据datasetId获取数据
	@GetMapping("/dataset/{datasetId}")
	public ResponseEntity<List<BusinessKnowledge>> getDataSetById(@PathVariable String datasetId) {
		List<BusinessKnowledge> knowledge = businessKnowledgePersistenceService.getFieldByDataSetId(datasetId);
		return ResponseEntity.ok(knowledge);
	}

	// 搜索
	@GetMapping("/search")
	public ResponseEntity<List<BusinessKnowledge>> searchFields(@RequestParam String content) {
		List<BusinessKnowledge> knowledge = businessKnowledgePersistenceService.searchFields(content);
		return ResponseEntity.ok(knowledge);
	}

	// 根据id删除
	@DeleteMapping("/delete/{id}")
	public ResponseEntity<ApiResponse> deleteFieldById(@PathVariable long id) {
		businessKnowledgePersistenceService.deleteFieldById(id);
		return ResponseEntity.ok(ApiResponse.success("业务知识删除成功"));
	}

	// 编辑更新
	@PutMapping("/update/{id}")
	public ResponseEntity<ApiResponse> updateField(@PathVariable long id,
			@RequestBody BusinessKnowledgeDTO knowledgeDTO) {
		businessKnowledgePersistenceService.updateField(knowledgeDTO, id);
		return ResponseEntity.ok(ApiResponse.success("业务知识更新成功"));
	}

	// 根据智能体ID获取业务知识列表
	@GetMapping("/agent/{agentId}")
	public ResponseEntity<List<BusinessKnowledge>> getKnowledgeByAgentId(@PathVariable String agentId) {
		List<BusinessKnowledge> knowledge = businessKnowledgePersistenceService.getKnowledgeByAgentId(agentId);
		return ResponseEntity.ok(knowledge);
	}

	// 为智能体添加业务知识
	@PostMapping("/agent/{agentId}/add")
	public ResponseEntity<ApiResponse> addKnowledgeForAgent(@PathVariable String agentId,
			@RequestBody BusinessKnowledgeDTO knowledgeDTO) {
		knowledgeDTO.setAgentId(agentId);
		businessKnowledgePersistenceService.addKnowledge(knowledgeDTO);
		return ResponseEntity.ok(ApiResponse.success("业务知识添加成功"));
	}

	// 批量为智能体添加业务知识
	@PostMapping("/agent/{agentId}/addList")
	public ResponseEntity<ApiResponse> addKnowledgeListForAgent(@PathVariable String agentId,
			@RequestBody List<BusinessKnowledgeDTO> knowledgeDTOs) {
		knowledgeDTOs.forEach(dto -> dto.setAgentId(agentId));
		businessKnowledgePersistenceService.addKnowledgeList(knowledgeDTOs);
		return ResponseEntity.ok(ApiResponse.success("批量业务知识添加成功"));
	}

	// 根据智能体ID删除所有业务知识
	@DeleteMapping("/agent/{agentId}")
	public ResponseEntity<ApiResponse> deleteKnowledgeByAgentId(@PathVariable String agentId) {
		businessKnowledgePersistenceService.deleteKnowledgeByAgentId(agentId);
		return ResponseEntity.ok(ApiResponse.success("智能体业务知识删除成功"));
	}

	// 在智能体范围内搜索业务知识
	@GetMapping("/agent/{agentId}/search")
	public ResponseEntity<List<BusinessKnowledge>> searchKnowledgeInAgent(@PathVariable String agentId,
			@RequestParam String content) {
		List<BusinessKnowledge> knowledge = businessKnowledgePersistenceService.searchKnowledgeInAgent(agentId,
				content);
		return ResponseEntity.ok(knowledge);
	}

}
