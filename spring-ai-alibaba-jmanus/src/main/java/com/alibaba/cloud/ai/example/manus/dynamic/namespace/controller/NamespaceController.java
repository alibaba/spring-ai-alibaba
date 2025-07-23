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
package com.alibaba.cloud.ai.example.manus.dynamic.namespace.controller;

import com.alibaba.cloud.ai.example.manus.dynamic.namespace.namespace.vo.NamespaceConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.namespace.service.NamespaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/namespaces")
@CrossOrigin(origins = "*") // Add cross-origin support
public class NamespaceController {

	@Autowired
	private NamespaceService namespaceService;

	@GetMapping
	public ResponseEntity<List<NamespaceConfig>> getAllNamespaces() {
		return ResponseEntity.ok(namespaceService.getAllNamespaces());
	}

	@GetMapping("/{id}")
	public ResponseEntity<NamespaceConfig> getNamespaceById(@PathVariable("id") String id) {
		return ResponseEntity.ok(namespaceService.getNamespaceById(id));
	}

	@PostMapping
	public ResponseEntity<NamespaceConfig> createNamespace(@RequestBody NamespaceConfig namespaceConfig) {
		return ResponseEntity.ok(namespaceService.createNamespace(namespaceConfig));
	}

	@PutMapping("/{id}")
	public ResponseEntity<NamespaceConfig> updateNamespace(@PathVariable("id") Long id,
			@RequestBody NamespaceConfig namespaceConfig) {
		namespaceConfig.setId(id);
		return ResponseEntity.ok(namespaceService.updateNamespace(namespaceConfig));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteNamespace(@PathVariable("id") String id) {
		try {
			namespaceService.deleteNamespace(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

}
