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
import com.alibaba.cloud.ai.service.BusinessKnowledgeService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Business Knowledge Management Controller
 */
@Controller
@RequestMapping("/api/business-knowledge")
@CrossOrigin(origins = "*")
public class BusinessKnowledgeController {

	private final BusinessKnowledgeService businessKnowledgeService;

	public BusinessKnowledgeController(BusinessKnowledgeService businessKnowledgeService) {
		this.businessKnowledgeService = businessKnowledgeService;
	}

	@GetMapping
	@ResponseBody
	public ResponseEntity<List<BusinessKnowledge>> list(
			@RequestParam(value = "datasetId", required = false) String datasetId,
			@RequestParam(required = false) String keyword) {
		List<BusinessKnowledge> result;
		if (keyword != null && !keyword.trim().isEmpty()) {
			result = businessKnowledgeService.search(keyword);
		}
		else if (datasetId != null && !datasetId.trim().isEmpty()) {
			result = businessKnowledgeService.findByDatasetId(datasetId);
		}
		else {
			result = businessKnowledgeService.findAll();
		}
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity<BusinessKnowledge> get(@PathVariable(value = "id") Long id) {
		BusinessKnowledge knowledge = businessKnowledgeService.findById(id);
		if (knowledge == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(knowledge);
	}

	@PostMapping
	@ResponseBody
	public ResponseEntity<BusinessKnowledge> create(@RequestBody BusinessKnowledge knowledge) {
		BusinessKnowledge saved = businessKnowledgeService.save(knowledge);
		return ResponseEntity.ok(saved);
	}

	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<BusinessKnowledge> update(@PathVariable(value = "id") Long id,
			@RequestBody BusinessKnowledge knowledge) {
		if (businessKnowledgeService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		knowledge.setId(id);
		BusinessKnowledge updated = businessKnowledgeService.save(knowledge);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) {
		if (businessKnowledgeService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		businessKnowledgeService.deleteById(id);
		return ResponseEntity.ok().build();
	}

}
