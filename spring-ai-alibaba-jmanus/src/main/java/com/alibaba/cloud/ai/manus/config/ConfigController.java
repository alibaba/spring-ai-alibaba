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
package com.alibaba.cloud.ai.manus.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.manus.config.entity.ConfigEntity;
import com.alibaba.cloud.ai.manus.model.entity.DynamicModelEntity;
import com.alibaba.cloud.ai.manus.model.repository.DynamicModelRepository;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

	@Autowired
	private IConfigService configService;

	@Autowired
	private DynamicModelRepository dynamicModelRepository;

	@GetMapping("/group/{groupName}")
	public ResponseEntity<List<ConfigEntity>> getConfigsByGroup(@PathVariable("groupName") String groupName) {
		return ResponseEntity.ok(configService.getConfigsByGroup(groupName));
	}

	@PostMapping("/batch-update")
	public ResponseEntity<Void> batchUpdateConfigs(@RequestBody List<ConfigEntity> configs) {
		configService.batchUpdateConfigs(configs);
		return ResponseEntity.ok().build();
	}

	@PostMapping("/reset-all-defaults")
	public ResponseEntity<Void> resetAllConfigsToDefaults() {
		configService.resetAllConfigsToDefaults();
		return ResponseEntity.ok().build();
	}

	@GetMapping("/available-models")
	public ResponseEntity<Map<String, Object>> getAvailableModels() {
		List<DynamicModelEntity> models = dynamicModelRepository.findAll();

		List<Map<String, Object>> modelOptions = models.stream().map(model -> {
			Map<String, Object> option = new HashMap<>();
			option.put("value", model.getId().toString());
			option.put("label", model.getModelName() + " (" + model.getModelDescription() + ")");
			return option;
		}).collect(Collectors.toList());

		Map<String, Object> response = new HashMap<>();
		response.put("options", modelOptions);
		response.put("total", modelOptions.size());

		return ResponseEntity.ok(response);
	}

}
