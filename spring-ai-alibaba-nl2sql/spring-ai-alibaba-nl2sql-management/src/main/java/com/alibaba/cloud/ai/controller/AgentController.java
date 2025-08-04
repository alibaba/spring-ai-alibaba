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

import com.alibaba.cloud.ai.entity.Agent;
import com.alibaba.cloud.ai.service.AgentService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 智能体管理控制器
 */
@Controller
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

	private final AgentService agentService;

	public AgentController(AgentService agentService) {
		this.agentService = agentService;
	}

	/**
	 * 获取智能体列表
	 */
	@GetMapping
	@ResponseBody
	public ResponseEntity<List<Agent>> list(@RequestParam(required = false) String status,
			@RequestParam(required = false) String keyword) {
		List<Agent> result;
		if (keyword != null && !keyword.trim().isEmpty()) {
			result = agentService.search(keyword);
		}
		else if (status != null && !status.trim().isEmpty()) {
			result = agentService.findByStatus(status);
		}
		else {
			result = agentService.findAll();
		}
		return ResponseEntity.ok(result);
	}

	/**
	 * 根据ID获取智能体详情
	 */
	@GetMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Agent> get(@PathVariable Long id) {
		Agent agent = agentService.findById(id);
		if (agent == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(agent);
	}

	/**
	 * 创建智能体
	 */
	@PostMapping
	@ResponseBody
	public ResponseEntity<Agent> create(@RequestBody Agent agent) {
		// 设置默认状态
		if (agent.getStatus() == null || agent.getStatus().trim().isEmpty()) {
			agent.setStatus("draft");
		}
		Agent saved = agentService.save(agent);
		return ResponseEntity.ok(saved);
	}

	/**
	 * 更新智能体
	 */
	@PutMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Agent> update(@PathVariable Long id, @RequestBody Agent agent) {
		if (agentService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		agent.setId(id);
		Agent updated = agentService.save(agent);
		return ResponseEntity.ok(updated);
	}

	/**
	 * 删除智能体
	 */
	@DeleteMapping("/{id}")
	@ResponseBody
	public ResponseEntity<Void> delete(@PathVariable Long id) {
		if (agentService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		agentService.deleteById(id);
		return ResponseEntity.ok().build();
	}

	/**
	 * 发布智能体
	 */
	@PostMapping("/{id}/publish")
	@ResponseBody
	public ResponseEntity<Agent> publish(@PathVariable Long id) {
		Agent agent = agentService.findById(id);
		if (agent == null) {
			return ResponseEntity.notFound().build();
		}
		agent.setStatus("published");
		Agent updated = agentService.save(agent);
		return ResponseEntity.ok(updated);
	}

	/**
	 * 下线智能体
	 */
	@PostMapping("/{id}/offline")
	@ResponseBody
	public ResponseEntity<Agent> offline(@PathVariable Long id) {
		Agent agent = agentService.findById(id);
		if (agent == null) {
			return ResponseEntity.notFound().build();
		}
		agent.setStatus("offline");
		Agent updated = agentService.save(agent);
		return ResponseEntity.ok(updated);
	}

}
