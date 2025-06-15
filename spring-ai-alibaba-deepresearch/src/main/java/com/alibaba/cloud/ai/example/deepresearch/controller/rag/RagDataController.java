package com.alibaba.cloud.ai.example.deepresearch.controller.rag;

import com.alibaba.cloud.ai.example.deepresearch.config.rag.RagProperties;
import com.alibaba.cloud.ai.example.deepresearch.service.VectorStoreDataIngestionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX, name = "enabled", havingValue = "true")
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