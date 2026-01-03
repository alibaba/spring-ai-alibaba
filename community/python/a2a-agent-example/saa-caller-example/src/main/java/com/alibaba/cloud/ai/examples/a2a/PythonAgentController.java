/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.examples.a2a;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for calling Python A2A agents.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /api/translate?text=... - Translate text using Python agent</li>
 *   <li>POST /api/translate - Translate text (body: {"text": "..."})</li>
 *   <li>GET /api/translate/streaming?text=... - Translate with streaming</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class PythonAgentController {

	private final PythonAgentCaller pythonAgentCaller;

	public PythonAgentController(PythonAgentCaller pythonAgentCaller) {
		this.pythonAgentCaller = pythonAgentCaller;
	}

	/**
	 * Translate text using Python agent (GET).
	 * @param text Text to translate
	 * @return Translation result
	 */
	@GetMapping("/translate")
	public TranslationResponse translate(@RequestParam String text) {
		String result = pythonAgentCaller.callTranslator(text);
		return new TranslationResponse(text, result);
	}

	/**
	 * Translate text using Python agent (POST).
	 * @param request Translation request
	 * @return Translation result
	 */
	@PostMapping("/translate")
	public TranslationResponse translatePost(@RequestBody TranslationRequest request) {
		String result = pythonAgentCaller.callTranslator(request.text());
		return new TranslationResponse(request.text(), result);
	}

	/**
	 * Translate text using Python agent with streaming.
	 * @param text Text to translate
	 * @return Translation result
	 */
	@GetMapping("/translate/streaming")
	public TranslationResponse translateStreaming(@RequestParam String text) {
		String result = pythonAgentCaller.callTranslatorStreaming(text);
		return new TranslationResponse(text, result);
	}

	/**
	 * Health check endpoint.
	 */
	@GetMapping("/health")
	public String health() {
		return "OK";
	}

	public record TranslationRequest(String text) {
	}

	public record TranslationResponse(String original, String translated) {
	}

}
