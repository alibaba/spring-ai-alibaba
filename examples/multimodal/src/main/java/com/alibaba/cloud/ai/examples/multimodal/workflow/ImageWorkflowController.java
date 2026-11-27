/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multimodal.workflow;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.UUID;

import com.alibaba.cloud.ai.examples.multimodal.workflow.ImageWorkflow.WorkflowResult;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class ImageWorkflowController {

	private final ImageWorkflow workflow;

	private final ImageArtifactStore store;

	ImageWorkflowController(ImageWorkflow workflow, ImageArtifactStore store) {
		this.workflow = workflow;
		this.store = store;
	}

	@PostMapping(value = "/workflows/agent", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	WorkflowResult agent(@RequestParam MultipartFile image, @RequestParam String question,
			@RequestParam(defaultValue = "true") boolean createIllustration) throws IOException, GraphRunnerException {
		return workflow.runAgent(question(question), ImageArtifactStore.inputMedia(image.getBytes()), createIllustration);
	}

	@PostMapping(value = "/workflows/graph", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	WorkflowResult graph(@RequestParam MultipartFile image, @RequestParam String question,
			@RequestParam(defaultValue = "true") boolean createIllustration) throws IOException, GraphRunnerException {
		return workflow.runGraph(question(question), ImageArtifactStore.inputMedia(image.getBytes()), createIllustration);
	}

	@GetMapping("/artifacts/{id}")
	ResponseEntity<Resource> artifact(@PathVariable UUID id) throws IOException {
		try {
			var artifact = store.describe(id);
			return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(artifact.mimeType()))
				.contentLength(artifact.sizeBytes())
				.body(new FileSystemResource(store.path(id)));
		}
		catch (NoSuchFileException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image artifact not found");
		}
	}

	private static String question(String question) {
		if (question.isBlank()) {
			throw new IllegalArgumentException("Enter a question about the image");
		}
		return question;
	}

}
