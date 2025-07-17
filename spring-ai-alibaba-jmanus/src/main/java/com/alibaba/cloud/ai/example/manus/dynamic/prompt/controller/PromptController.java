/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.dynamic.prompt.controller;

import com.alibaba.cloud.ai.example.manus.dynamic.prompt.model.vo.PromptVO;
import com.alibaba.cloud.ai.example.manus.dynamic.prompt.service.PromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prompt")
public class PromptController {

	private final PromptService promptService;

	public PromptController(PromptService promptService) {
		this.promptService = promptService;
	}

	@GetMapping
	public ResponseEntity<List<PromptVO>> getAll() {
		return ResponseEntity.ok(promptService.getAll());
	}

	@GetMapping("/namespace/{namespace}")
	public ResponseEntity<List<PromptVO>> getAllByNamespace(@PathVariable("namespace") String namespace) {
		return ResponseEntity.ok(promptService.getAllByNamespace(namespace));
	}

	@GetMapping("/{id}")
	public ResponseEntity<PromptVO> getById(@PathVariable("id") Long id) {
		return ResponseEntity.ok(promptService.getById(id));
	}

	@PostMapping
	public ResponseEntity<PromptVO> create(@RequestBody PromptVO prompt) {
		return ResponseEntity.ok(promptService.create(prompt));
	}

	@PutMapping("/{id}")
	public ResponseEntity<PromptVO> update(@PathVariable("id") Long id, @RequestBody PromptVO prompt) {
		prompt.setId(id);
		return ResponseEntity.ok(promptService.update(prompt));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
		try {
			promptService.delete(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

}
