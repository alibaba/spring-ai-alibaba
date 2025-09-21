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

	// Add
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

	// Enable
	@PutMapping("/enable")
	public ResponseEntity<Void> enableFields(@RequestBody List<Long> ids) {
		semanticModelPersistenceService.enableFields(ids);
		return ResponseEntity.ok().build();
	}

	// Disable
	@PutMapping("/disable")
	public ResponseEntity<Void> disableFields(@RequestBody List<Long> ids) {
		semanticModelPersistenceService.disableFields(ids);
		return ResponseEntity.ok().build();
	}

	// Get data by agentId
	@GetMapping("/agent/{agentId}")
	public ResponseEntity<List<SemanticModel>> getFieldsByAgentId(@PathVariable(value = "agentId") Long agentId) {
		List<SemanticModel> fields = semanticModelPersistenceService.getFieldByAgentId(agentId);
		return ResponseEntity.ok(fields);
	}

	// Search
	@GetMapping("/search")
	public ResponseEntity<List<SemanticModel>> searchFields(@RequestParam(value = "content") String content) {
		List<SemanticModel> fields = semanticModelPersistenceService.searchFields(content);
		return ResponseEntity.ok(fields);
	}

	// Delete by id
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFieldById(@PathVariable(value = "id") long id) {
		semanticModelPersistenceService.deleteFieldById(id);
		return ResponseEntity.ok().build();
	}

	// Edit update
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateField(@PathVariable(value = "id") long id,
			@RequestBody SemanticModelDTO semanticModelDTO) {
		semanticModelPersistenceService.updateField(semanticModelDTO, id);
		return ResponseEntity.ok().build();
	}

}
