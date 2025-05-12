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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.model.Tool;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.agent.service.AgentService;

@RestController
@RequestMapping("/api/agents")
@CrossOrigin(origins = "*") // 添加跨域支持
public class AgentController {

	@Autowired
	private AgentService agentService;

	@GetMapping
	public ResponseEntity<List<AgentConfig>> getAllAgents() {
		return ResponseEntity.ok(agentService.getAllAgents());
	}

	@GetMapping("/{id}")
	public ResponseEntity<AgentConfig> getAgentById(@PathVariable("id") String id) {
		return ResponseEntity.ok(agentService.getAgentById(id));
	}

	@PostMapping
	public ResponseEntity<AgentConfig> createAgent(@RequestBody AgentConfig agentConfig) {
		return ResponseEntity.ok(agentService.createAgent(agentConfig));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AgentConfig> updateAgent(@PathVariable("id") String id,
			@RequestBody AgentConfig agentConfig) {
		agentConfig.setId(id);
		return ResponseEntity.ok(agentService.updateAgent(agentConfig));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAgent(@PathVariable("id") String id) {
		try {
			agentService.deleteAgent(id);
			return ResponseEntity.ok().build();
		}
		catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().build();
		}
	}

	@GetMapping("/tools")
	public ResponseEntity<List<Tool>> getAvailableTools() {
		return ResponseEntity.ok(agentService.getAvailableTools());
	}

}
