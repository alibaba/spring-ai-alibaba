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

import com.alibaba.cloud.ai.dto.Knowledge;
import com.alibaba.cloud.ai.dto.schema.KnowledgeDTO;
import com.alibaba.cloud.ai.service.KnowledgeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class knowledgeController {

	private final KnowledgeService knowledgeService;

	public knowledgeController(KnowledgeService knowledgeService) {
		this.knowledgeService = knowledgeService;
	}

	// 新增
	@PostMapping("/add")
	public ResponseEntity<Void> addField(@RequestBody KnowledgeDTO knowledgeDTO) {
		knowledgeService.addKnowledge(knowledgeDTO);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/addList")
	public ResponseEntity<Void> addFields(@RequestBody List<KnowledgeDTO> knowledgeDTOs) {
		knowledgeService.addKnowledgeList(knowledgeDTOs);
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	// 获取数据集id列表
	@GetMapping("/datasetIds")
	public ResponseEntity<List<String>> getDataSetIds() {
		List<String> datasetIds = knowledgeService.getDataSetIds();
		return new ResponseEntity<>(datasetIds, HttpStatus.OK);
	}

	// 根据datasetId获取数据
	@GetMapping("/dataset/{datasetId}")
	public ResponseEntity<List<Knowledge>> getDataSetById(@PathVariable String datasetId) {
		List<Knowledge> knowledge = knowledgeService.getFieldByDataSetId(datasetId);
		return new ResponseEntity<>(knowledge, HttpStatus.OK);
	}

	// 搜索
	@GetMapping("/search")
	public ResponseEntity<List<Knowledge>> searchFields(@RequestParam String content) {
		List<Knowledge> knowledge = knowledgeService.searchFields(content);
		return new ResponseEntity<>(knowledge, HttpStatus.OK);
	}

	// 根据id删除
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteFieldById(@PathVariable int id) {
		knowledgeService.deleteFieldById(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

	// 编辑更新
	@PutMapping("/{id}")
	public ResponseEntity<Void> updateField(@PathVariable int id, @RequestBody KnowledgeDTO knowledgeDTO) {
		knowledgeService.updateField(knowledgeDTO, id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}

}
