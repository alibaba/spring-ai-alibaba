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
package com.alibaba.cloud.ai.examples.multimodal;

import com.alibaba.cloud.ai.examples.multimodal.audio.AudioService;
import com.alibaba.cloud.ai.examples.multimodal.creative.CreativeService;
import com.alibaba.cloud.ai.examples.multimodal.image.ImageService;
import com.alibaba.cloud.ai.graph.agent.tool.multimodal.MultimodalToolCallResultConverter;
import com.alibaba.cloud.ai.graph.agent.tool.multimodal.OutputFormat;
import com.alibaba.cloud.ai.graph.agent.tool.multimodal.ToolMultimodalResult;
import org.springframework.ai.util.json.JsonParser;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.URI;
import java.util.Map;

/**
 * REST API for multimodal scenarios: image from URL, image from resource, vision agent,
 * and creative agent.
 */
@RestController
@RequestMapping("/api")
public class MultimodalController {

	private final ImageService imageService;

	private final CreativeService creativeService;

	private final AudioService audioService;


	public MultimodalController(ImageService imageService, CreativeService creativeService,
			@Autowired(required = false) AudioService audioService) {
		this.imageService = imageService;
		this.creativeService = creativeService;
		this.audioService = audioService;
	}

	/**
	 * Scene 1: Describe image from a public URL (ChatModel).
	 * POST /api/image/from-url
	 */
	@PostMapping(value = "/image/from-url", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> runImageFromUrl(@RequestBody ImageUrlRequest request) {
		String result = imageService.describeImageFromUrl(request.imageUrl(), request.question());
		return ResponseEntity.ok(Map.of("answer", result));
	}

	/**
	 * Scene 2: Describe image from uploaded file or classpath resource.
	 * POST /api/image/from-resource (multipart: image file + question)
	 * POST /api/image/from-resource?resourcePath=images/sample.png (classpath resource)
	 */
	@PostMapping(value = "/image/from-resource", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<Map<String, String>> runImageFromResource(
			@RequestParam("image") MultipartFile image,
			@RequestParam("question") String question) throws Exception {
		String contentType = image.getContentType();
		var mimeType = (contentType != null && !contentType.isBlank())
				? org.springframework.util.MimeTypeUtils.parseMimeType(contentType)
				: MimeTypeUtils.IMAGE_PNG;

		Resource resource = new ByteArrayResource(image.getBytes()) {
			@Override
			public String getFilename() {
				return image.getOriginalFilename();
			}
		};
		String result = imageService.describeImageFromResource(resource, question, mimeType);
		return ResponseEntity.ok(Map.of("answer", result));
	}

	/**
	 * Scene 2 (alternative): Describe image from classpath resource.
	 * POST /api/image/from-resource (JSON body with resourcePath)
	 */
	@PostMapping(value = "/image/from-resource", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> runImageFromClasspathResource(
			@RequestBody ImageResourceRequest request) {
		Resource resource = new ClassPathResource(request.resourcePath());
		if (!resource.exists()) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Resource not found: " + request.resourcePath()));
		}
		String result = imageService.describeImageFromResource(resource, request.question());
		return ResponseEntity.ok(Map.of("answer", result));
	}

	/**
	 * Scene 3: Vision agent with multimodal input (ReactAgent).
	 * POST /api/vision/agent
	 */
	@PostMapping(value = "/vision/agent", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, String>> runVisionAgent(@RequestBody ImageUrlRequest request)
			throws GraphRunnerException {
		UserMessage userMessage = UserMessage.builder()
				.text(request.question())
				.media(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(request.imageUrl())))
				.build();
		var response = imageService.visionAgentCall(userMessage);
		return ResponseEntity.ok(Map.of("answer", response.getText()));
	}

	/**
	 * Scene 4: Creative agent (image generation via tools).
	 * POST /api/creative/agent
	 */
	@PostMapping(value = "/creative/agent", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Map<String, Object>> runCreativeAgent(@RequestBody CreativeRequest request)
			throws GraphRunnerException {
		if (!creativeService.isCreativeAgentAvailable()) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Creative agent not available (ImageModel may not be configured)"));
		}
		var response = creativeService.creativeAgentCall(request.userRequest());
		return ResponseEntity.ok(Map.of("answer", response.getText()));
	}

	/**
	 * TTS: Direct synthesis using DashScopeAudioSpeechModel.
	 * POST /api/audio/tts
	 * Uses call() or stream() based on model support; outputFormat controls url vs base64.
	 */
	@PostMapping(value = "/audio/tts", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> runTts(@RequestBody TtsRequest request) {
		if (audioService == null) {
			return ResponseEntity.badRequest()
					.body(Map.of("error", "Audio service not available (DashScopeAudioSpeechModel may not be configured)"));
		}
		OutputFormat format = OutputFormat.from(request.outputFormat());
		ToolMultimodalResult result = audioService.synthesize(request.text(), request.voice(), format);
		MultimodalToolCallResultConverter converter = new MultimodalToolCallResultConverter(format);
		String json = converter.convert(result, null);
		@SuppressWarnings("unchecked")
		Map<String, Object> body = (Map<String, Object>) JsonParser.fromJson(json, Map.class);
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(body);
	}

	/**
	 * Serve the index page (redirect to static/index.html).
	 */
	@GetMapping("/")
	public String index() {
		return "redirect:/index.html";
	}

	public record ImageUrlRequest(String imageUrl, String question) {
	}

	public record ImageResourceRequest(String resourcePath, String question) {
	}

	public record CreativeRequest(String userRequest) {
	}

	public record TtsRequest(String text, String voice, String outputFormat) {
	}
}
