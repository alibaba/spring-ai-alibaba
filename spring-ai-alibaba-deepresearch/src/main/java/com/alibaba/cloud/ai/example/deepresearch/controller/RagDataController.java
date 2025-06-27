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

package com.alibaba.cloud.ai.example.deepresearch.controller;

import com.alibaba.cloud.ai.example.deepresearch.service.VectorStoreDataIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;

/**
 * Controller for managing RAG data, including document uploads.
 *
 * @author hupei
 */
@RestController
@RequestMapping("/api/rag/data")
public class RagDataController {

	private final VectorStoreDataIngestionService ingestionService;

	public RagDataController(VectorStoreDataIngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@PostMapping("/upload")
	public ResponseEntity<Map<String, String>> handleFileUpload(@RequestParam("file") MultipartFile file) {
		if (file.isEmpty()) {
			return ResponseEntity.badRequest().body(Map.of("message", "File is empty."));
		}

		try {
			// MultipartFile.getResource() is a convenient way to pass it to the service
			ingestionService.ingest(file.getResource());
			String message = "File '" + file.getOriginalFilename() + "' uploaded and ingested successfully.";
			return ResponseEntity.ok(Map.of("message", message));
		}
		catch (Exception e) {
			String message = "Failed to upload and ingest '" + file.getOriginalFilename() + "'.";
			return ResponseEntity.internalServerError().body(Map.of("message", message, "error", e.getMessage()));
		}
	}

}
