/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.example.dotprompt.controller;

import com.alibaba.cloud.ai.example.dotprompt.model.TextRequest;
import com.alibaba.cloud.ai.example.dotprompt.model.TextResponse;
import com.alibaba.cloud.ai.example.dotprompt.service.TextProcessingService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/text")
public class TextProcessingController {

	private final TextProcessingService textProcessingService;

	public TextProcessingController(TextProcessingService textProcessingService) {
		this.textProcessingService = textProcessingService;
	}

	@PostMapping("/translate")
	public TextResponse translate(@RequestBody TextRequest request) {
		return textProcessingService.translate(request);
	}

	@PostMapping("/summarize")
	public TextResponse summarize(@RequestBody TextRequest request) {
		return textProcessingService.summarize(request);
	}

	@PostMapping("/analyze")
	public TextResponse analyze(@RequestBody TextRequest request) {
		return textProcessingService.analyze(request);
	}

}
