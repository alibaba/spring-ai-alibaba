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

import com.alibaba.cloud.ai.dto.AgentField;
import com.alibaba.cloud.ai.dto.schema.AgentFieldDTO;
import com.alibaba.cloud.ai.service.AgentFieldService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/fields")
public class AgentFieldController {

	private final AgentFieldService agentFieldService;

	public AgentFieldController(AgentFieldService agentFieldService) {
		this.agentFieldService = agentFieldService;
	}

	// 新增
	@PostMapping("/add")
	public ResponseEntity<Void> addField(@RequestBody AgentFieldDTO agentField) {
		agentFieldService.addField(agentField);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/addList")
	public ResponseEntity<Void> addFields(@RequestBody List<AgentFieldDTO> agentFields) {
		agentFieldService.addFields(agentFields);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	// 启用
	@PutMapping("/enable")
	public ResponseEntity<Void> enableFields(@RequestBody List<Integer> ids) {
		agentFieldService.enableFields(ids);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// 禁用
	@PutMapping("/disable")
	public ResponseEntity<Void> disableFields(@RequestBody List<Integer> ids) {
		agentFieldService.disableFields(ids);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// 获取数据集id列表
	@GetMapping("/datasetIds")
	public ResponseEntity<List<String>> getDataSetIds() {
		List<String> datasetIds = agentFieldService.getDataSetIds();
		return new ResponseEntity<>(datasetIds, HttpStatus.OK);
	}

	// 根据datasetId获取数据
	@GetMapping("/dataset/{datasetId}")
	public ResponseEntity<List<AgentField>> getDataSetById(@PathVariable String datasetId) {
		List<AgentField> fields = agentFieldService.getFieldByDataSetId(datasetId);
		return new ResponseEntity<>(fields, HttpStatus.OK);
	}

	// 搜索
	@GetMapping("/search")
	public ResponseEntity<List<AgentField>> searchFields(@RequestParam String content) {
		List<AgentField> fields = agentFieldService.searchFields(content);
		return new ResponseEntity<>(fields, HttpStatus.OK);
	}

	// 根据id删除
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFieldById(@PathVariable int id) {
		agentFieldService.deleteFieldById(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// 编辑更新
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateField(@PathVariable int id, @RequestBody AgentFieldDTO agentField) {
		agentFieldService.updateField(agentField, id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
