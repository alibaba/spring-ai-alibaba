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
import com.alibaba.cloud.ai.service.SemanticModelService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Semantic Model Configuration Controller
 */
@Controller
@RequestMapping("/api/semantic-model")
@CrossOrigin(origins = "*")
public class SemanticModelController {

	private final SemanticModelService semanticModelService;

	public SemanticModelController(SemanticModelService semanticModelService) {
		this.semanticModelService = semanticModelService;
	}

	@GetMapping
	@ResponseBody
	public ResponseEntity<List<SemanticModel>> list(@RequestParam(value = "keyword", required = false) String keyword,
			@RequestParam(value = "agentId", required = false) Long agentId) {
		List<SemanticModel> result;
		if (keyword != null && !keyword.trim().isEmpty()) {
			result = semanticModelService.search(keyword);
		}
		else if (agentId != null) {
			result = semanticModelService.findByAgentId(agentId);
		}
		else {
			result = semanticModelService.findAll();
		}
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity<SemanticModel> get(@PathVariable(value = "id") Long id) {
		SemanticModel model = semanticModelService.findById(id);
		if (model == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(model);
	}

	@PostMapping
	@ResponseBody
	public ResponseEntity<SemanticModel> create(@RequestBody SemanticModel model) {
		SemanticModel saved = semanticModelService.save(model);
		return ResponseEntity.ok(saved);
	}

	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<SemanticModel> update(@PathVariable(value = "id") Long id, @RequestBody SemanticModel model) {
		if (semanticModelService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		model.setId(id);
		SemanticModel updated = semanticModelService.save(model);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) {
		if (semanticModelService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		semanticModelService.deleteById(id);
		return ResponseEntity.ok().build();
	}

}
