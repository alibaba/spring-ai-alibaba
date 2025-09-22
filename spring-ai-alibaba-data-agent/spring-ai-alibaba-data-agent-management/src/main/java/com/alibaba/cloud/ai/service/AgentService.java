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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.Agent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.alibaba.cloud.ai.mapper.AgentMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent Service Class
 */
@Service
public class AgentService {

	private static final Logger log = LoggerFactory.getLogger(AgentService.class);

	@Autowired
	private AgentMapper agentMapper;

	@Autowired
	private AgentVectorService agentVectorService;

	public List<Agent> findAll() {
		return agentMapper.findAll();
	}

	public Agent findById(Long id) {
		return agentMapper.findById(id);
	}

	public List<Agent> findByStatus(String status) {
		return agentMapper.findByStatus(status);
	}

	public List<Agent> search(String keyword) {
		return agentMapper.searchByKeyword(keyword);
	}

	public Agent save(Agent agent) {
		LocalDateTime now = LocalDateTime.now();

		if (agent.getId() == null) {
			// Add
			agent.setCreateTime(now);
			agent.setUpdateTime(now);
			// 确保 humanReviewEnabled 不为 null
			if (agent.getHumanReviewEnabled() == null) {
				agent.setHumanReviewEnabled(0);
			}

			agentMapper.insert(agent);
		}
		else {
			// Update
			agent.setUpdateTime(now);
			// 确保 humanReviewEnabled 不为 null
			if (agent.getHumanReviewEnabled() == null) {
				agent.setHumanReviewEnabled(0);
			}
			agentMapper.updateById(agent);
		}

		return agent;
	}

	public void deleteById(Long id) {
		try {
			// Delete agent record from database
			agentMapper.deleteById(id);

			// Also clean up the agent's vector data
			if (agentVectorService != null) {
				try {
					agentVectorService.deleteAllVectorDataForAgent(id);
					log.info("Successfully deleted vector data for agent: {}", id);
				}
				catch (Exception vectorException) {
					log.warn("Failed to delete vector data for agent: {}, error: {}", id, vectorException.getMessage());
					// Vector data deletion failure does not affect the main process
				}
			}

			log.info("Successfully deleted agent: {}", id);
		}
		catch (Exception e) {
			log.error("Failed to delete agent: {}", id, e);
			throw e;
		}
	}

}
