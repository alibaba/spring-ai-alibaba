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
package com.alibaba.cloud.ai.manus.prompt.controller;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.manus.prompt.service.PromptService;

@RestController
@RequestMapping("/admin/prompts")
public class PromptAdminController {

	@Autowired
	private PromptService promptService;

	@PostMapping("/reinitialize")
	@GetMapping
	public ResponseEntity<String> reinitializePrompts() {
		try {
			promptService.reinitializePrompts();
			return ResponseEntity.ok("Prompts reinitialized successfully. Please restart the application.");
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error reinitializing prompts: " + e.getMessage());
		}
	}

	@PostMapping("/switch-language")
	public ResponseEntity<String> switchLanguage(@RequestParam String language) {
		try {
			String[] supportedLanguages = promptService.getSupportedLanguages();
			boolean isSupported = false;
			for (String supportedLang : supportedLanguages) {
				if (supportedLang.equals(language)) {
					isSupported = true;
					break;
				}
			}

			if (!isSupported) {
				return ResponseEntity.badRequest()
					.body("Unsupported language: " + language + ". Supported languages: "
							+ String.join(", ", supportedLanguages));
			}

			promptService.importAllPromptsFromLanguage(language);
			return ResponseEntity.ok("All prompts switched to language: " + language + " successfully.");
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Error switching language: " + e.getMessage());
		}
	}

	@GetMapping("/supported-languages")
	public ResponseEntity<String[]> getSupportedLanguages() {
		try {
			String[] languages = promptService.getSupportedLanguages();
			return ResponseEntity.ok(languages);
		}
		catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

}
