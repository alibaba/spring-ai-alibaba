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
package com.alibaba.cloud.ai.manus.memory.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.alibaba.cloud.ai.manus.memory.entity.MemoryEntity;
import com.alibaba.cloud.ai.manus.memory.service.MemoryService;

import java.util.List;

/**
 * @author dahua
 * @time 2025/8/5
 * @desc memory controller
 */
@RestController
@RequestMapping("/api/memories")
@CrossOrigin(origins = "*") // Add cross-origin support
public class MemoryController {

	@Autowired
	private MemoryService memoryService;

	@GetMapping
	public ResponseEntity<List<MemoryEntity>> getAllModels() {
		return ResponseEntity.ok(memoryService.getMemories());
	}

	@GetMapping("/single")
	public ResponseEntity<MemoryEntity> singleMemory(String memoryId) {
		return ResponseEntity.ok(memoryService.singleMemory(memoryId));
	}

	@PostMapping("/update")
	public ResponseEntity<MemoryEntity> updateMemory(@RequestBody MemoryEntity memoryEntity) {
		return ResponseEntity.ok(memoryService.updateMemory(memoryEntity));
	}

	@GetMapping("/delete")
	public ResponseEntity<Void> deleteMemory(String memoryId) {
		memoryService.deleteMemory(memoryId);
		return ResponseEntity.ok().build();
	}

}
