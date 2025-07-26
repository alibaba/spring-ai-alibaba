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
	public ResponseEntity<Void> addField(@RequestBody BusinessKnowledgeDTO knowledgeDTO) {
		businessKnowledgePersistenceService.addKnowledge(knowledgeDTO);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/addList")
	public ResponseEntity<Void> addFields(@RequestBody List<BusinessKnowledgeDTO> knowledgeDTOs) {
		businessKnowledgePersistenceService.addKnowledgeList(knowledgeDTOs);
		return ResponseEntity.ok().build();
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
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFieldById(@PathVariable long id) {
		businessKnowledgePersistenceService.deleteFieldById(id);
		return ResponseEntity.ok().build();
	}

	// 编辑更新
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateField(@PathVariable long id, @RequestBody BusinessKnowledgeDTO knowledgeDTO) {
		businessKnowledgePersistenceService.updateField(knowledgeDTO, id);
		return ResponseEntity.ok().build();
	}

}
