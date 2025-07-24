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

import com.alibaba.cloud.ai.entity.SemanticModel;
import com.alibaba.cloud.ai.entity.SemanticModelDTO;
import com.alibaba.cloud.ai.service.SemanticModelPersistenceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
public class SemanticModelPersistenceController {

	private final SemanticModelPersistenceService semanticModelPersistenceService;

	public SemanticModelPersistenceController(SemanticModelPersistenceService semanticModelPersistenceService) {
		this.semanticModelPersistenceService = semanticModelPersistenceService;
	}

	// 新增
	@PostMapping("/add")
	public ResponseEntity<Void> addField(@RequestBody SemanticModelDTO semanticModelDTO) {
		semanticModelPersistenceService.addField(semanticModelDTO);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/addList")
	public ResponseEntity<Void> addFields(@RequestBody List<SemanticModelDTO> semanticModelDTOS) {
		semanticModelPersistenceService.addFields(semanticModelDTOS);
		return ResponseEntity.ok().build();
	}

	// 启用
	@PutMapping("/enable")
	public ResponseEntity<Void> enableFields(@RequestBody List<Long> ids) {
		semanticModelPersistenceService.enableFields(ids);
		return ResponseEntity.ok().build();
	}

	// 禁用
	@PutMapping("/disable")
	public ResponseEntity<Void> disableFields(@RequestBody List<Long> ids) {
		semanticModelPersistenceService.disableFields(ids);
		return ResponseEntity.ok().build();
	}

	// 获取数据集id列表
	@GetMapping("/datasetIds")
	public ResponseEntity<List<String>> getDataSetIds() {
		List<String> datasetIds = semanticModelPersistenceService.getDataSetIds();
		return ResponseEntity.ok(datasetIds);
	}

	// 根据datasetId获取数据
	@GetMapping("/dataset/{datasetId}")
	public ResponseEntity<List<SemanticModel>> getDataSetById(@PathVariable String datasetId) {
		List<SemanticModel> fields = semanticModelPersistenceService.getFieldByDataSetId(datasetId);
		return ResponseEntity.ok(fields);
	}

	// 搜索
	@GetMapping("/search")
	public ResponseEntity<List<SemanticModel>> searchFields(@RequestParam String content) {
		List<SemanticModel> fields = semanticModelPersistenceService.searchFields(content);
		return ResponseEntity.ok(fields);
	}

	// 根据id删除
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFieldById(@PathVariable long id) {
		semanticModelPersistenceService.deleteFieldById(id);
		return ResponseEntity.ok().build();
	}

	// 编辑更新
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateField(@PathVariable long id, @RequestBody SemanticModelDTO semanticModelDTO) {
		semanticModelPersistenceService.updateField(semanticModelDTO, id);
		return ResponseEntity.ok().build();
	}

}
