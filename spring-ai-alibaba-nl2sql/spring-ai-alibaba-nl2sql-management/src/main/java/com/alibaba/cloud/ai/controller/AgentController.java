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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent Management Controller
 */
@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

	private final AgentService agentService;

	@Autowired(required = false)
	public AgentController(AgentService agentService) {
		this.agentService = agentService;
	}

	/**
	 * Get agent list
	 */
	@GetMapping("/list")
	public ResponseEntity<List<Agent>> list(@RequestParam(value = "status", required = false) String status,
			@RequestParam(value = "keyword", required = false) String keyword) {
		List<Agent> result = null;
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
	 * Get agent details by ID
	 */
	@GetMapping("/{id}")
	public ResponseEntity<Agent> get(@PathVariable(value = "id") Long id) {
		Agent agent = agentService.findById(id);
		if (agent == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(agent);
	}

	/**
	 * Create agent
	 */
	@PostMapping("/create")
	public ResponseEntity<Agent> create(@RequestBody Agent agent) {
		// Set default status
		if (agent.getStatus() == null || agent.getStatus().trim().isEmpty()) {
			agent.setStatus("draft");
		}
		Agent saved = agentService.save(agent);
		return ResponseEntity.ok(saved);
	}

	/**
	 * Update agent
	 */
	@PutMapping("/{id}")
	public ResponseEntity<Agent> update(@PathVariable(value = "id") Long id, @RequestBody Agent agent) {
		if (agentService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		agent.setId(id);
		Agent updated = agentService.save(agent);
		return ResponseEntity.ok(updated);
	}

	/**
	 * Delete agent
	 */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable(value = "id") Long id) {
		if (agentService.findById(id) == null) {
			return ResponseEntity.notFound().build();
		}
		agentService.deleteById(id);
		return ResponseEntity.ok().build();
	}

	/**
	 * Publish agent
	 */
	@PostMapping("/{id}/publish")
	public ResponseEntity<Agent> publish(@PathVariable(value = "id") Long id) {
		Agent agent = agentService.findById(id);
		if (agent == null) {
			return ResponseEntity.notFound().build();
		}
		agent.setStatus("published");
		Agent updated = agentService.save(agent);
		return ResponseEntity.ok(updated);
	}

	/**
	 * Offline agent
	 */
	@PostMapping("/{id}/offline")
	public ResponseEntity<Agent> offline(@PathVariable(value = "id") Long id) {
		Agent agent = agentService.findById(id);
		if (agent == null) {
			return ResponseEntity.notFound().build();
		}
		agent.setStatus("offline");
		Agent updated = agentService.save(agent);
		return ResponseEntity.ok(updated);
	}

}
