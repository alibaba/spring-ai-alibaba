/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.entity.PromptTemplate;
import com.alibaba.cloud.ai.service.PromptTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 提示词模板管理控制器
 */
@Controller
@RequestMapping("/api/prompt-templates")
public class PromptTemplateController {

	@Autowired
	private PromptTemplateService promptTemplateService;

	@GetMapping
	@ResponseBody
	public ResponseEntity<List<PromptTemplate>> list(@RequestParam(required = false) String templateType,
			@RequestParam(required = false) String keyword) {
		List<PromptTemplate> result;
		if (keyword != null && !keyword.trim().isEmpty()) {
			result = promptTemplateService.search(keyword);
		}
		else if (templateType != null && !templateType.trim().isEmpty()) {
			result = promptTemplateService.findByType(templateType);
		}
		else {
			result = promptTemplateService.findAll();
		}
		return ResponseEntity.ok(result);
	}

	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity<PromptTemplate> get(@PathVariable Long id) {
		PromptTemplate template = promptTemplateService.findById(id);
		if (template == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(template);
	}

	@GetMapping("/by-name/{templateName}")
	@ResponseBody
	public ResponseEntity<PromptTemplate> getByName(@PathVariable String templateName) {
		PromptTemplate template = promptTemplateService.findByName(templateName);
		if (template == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(template);
	}

	@PostMapping
	@ResponseBody
	public ResponseEntity<PromptTemplate> create(@RequestBody PromptTemplate template) {
		if (promptTemplateService.findByName(template.getTemplateName()) != null) {
			return ResponseEntity.badRequest().build();
		}

		PromptTemplate saved = promptTemplateService.save(template);
		return ResponseEntity.ok(saved);
	}

	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<PromptTemplate> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
		if (promptTemplateService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		template.setId(id);
		PromptTemplate updated = promptTemplateService.save(template);
		return ResponseEntity.ok(updated);
	}

	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		if (promptTemplateService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		promptTemplateService.deleteById(id);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/batch-enable")
	@ResponseBody
	public ResponseEntity<Void> batchEnable(@RequestParam String templateType, @RequestParam boolean enabled) {
		promptTemplateService.batchUpdateEnabled(templateType, enabled);
		return ResponseEntity.ok().build();
	}

	@PutMapping("/hot-update/{templateName}")
	@ResponseBody
	public ResponseEntity<Map<String, Object>> hotUpdate(@PathVariable String templateName,
			@RequestBody Map<String, String> request) {
		String newContent = request.get("content");
		if (newContent == null || newContent.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		boolean success = promptTemplateService.hotUpdateTemplate(templateName, newContent);
		Map<String, Object> response = Map.of("success", success, "message", success ? "模板更新成功" : "模板不存在或未启用",
				"templateName", templateName);

		return ResponseEntity.ok(response);
	}

	/**
	 * 获取模板内容（用于前端编辑器）
	 */
	@GetMapping("/content/{templateName}")
	@ResponseBody
	public ResponseEntity<Map<String, String>> getTemplateContent(@PathVariable String templateName) {
		String content = promptTemplateService.getEnabledTemplateContent(templateName);
		if (content == null) {
			return ResponseEntity.notFound().build();
		}

		Map<String, String> response = Map.of("templateName", templateName, "content", content);

		return ResponseEntity.ok(response);
	}

	@PostMapping("/preview")
	@ResponseBody
	public ResponseEntity<Map<String, String>> previewTemplate(@RequestBody Map<String, Object> request) {
		String templateContent = (String) request.get("templateContent");
		Map<String, String> parameters = (Map<String, String>) request.get("parameters");

		if (templateContent == null || templateContent.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		try {
			String previewContent = templateContent;
			if (parameters != null) {
				for (Map.Entry<String, String> entry : parameters.entrySet()) {
					String placeholder = "{" + entry.getKey() + "}";
					previewContent = previewContent.replace(placeholder, entry.getValue());
				}
			}

			Map<String, String> response = Map.of("preview", previewContent, "status", "success");

			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			Map<String, String> response = Map.of("preview", "", "status", "error", "message", e.getMessage());

			return ResponseEntity.ok(response);
		}
	}

}
