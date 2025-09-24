/**
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.entity.AgentKnowledge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.cloud.ai.mapper.AgentKnowledgeMapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent Knowledge Service Class
 */
@Service
public class AgentKnowledgeService {

	@Autowired
	private AgentKnowledgeMapper agentKnowledgeMapper;

	/**
	 * Query knowledge list by agent ID
	 */
    public List<AgentKnowledge> getKnowledgeByAgentId(Integer agentId) {
        return agentKnowledgeMapper.selectByAgentId(agentId);
    }

	/**
	 * Query knowledge details by ID
	 */
    public AgentKnowledge getKnowledgeById(Integer id) {
        return agentKnowledgeMapper.selectById(id);
    }

	/**
	 * Create knowledge
	 */
	public AgentKnowledge createKnowledge(AgentKnowledge knowledge) {
	    LocalDateTime now = LocalDateTime.now();

        // Set default values
        if (knowledge.getType() == null) {
            knowledge.setType("document");
        }
        if (knowledge.getStatus() == null) {
            knowledge.setStatus("active");
        }
        if (knowledge.getEmbeddingStatus() == null) {
            knowledge.setEmbeddingStatus("pending");
        }

        // Set creation and update time
        knowledge.setCreateTime(now);
        knowledge.setUpdateTime(now);

        // Insert into database, the ID will be auto-filled by MyBatis
        agentKnowledgeMapper.insert(knowledge);

		return knowledge;
	}

	/**
	 * Update knowledge
	 */
	public AgentKnowledge updateKnowledge(Integer id, AgentKnowledge knowledge) {
        LocalDateTime now = LocalDateTime.now();

        // Ensure the knowledge object has the correct ID
        knowledge.setId(id);
        knowledge.setUpdateTime(now);

        int updatedRows = agentKnowledgeMapper.update(knowledge);
        if (updatedRows > 0) {
            return knowledge;
        } else {
            return null; // No matching record found
        }
	}

	/**
	 * Delete knowledge
	 */
	public boolean deleteKnowledge(Integer id) {
        int deletedRows = agentKnowledgeMapper.deleteById(id);
        return deletedRows > 0;
	}

	/**
	 * Query knowledge list by type
	 */
	public List<AgentKnowledge> getKnowledgeByType(Integer agentId, String type) {
		return agentKnowledgeMapper.selectByAgentIdAndType(agentId, type);
	}

	/**
	 * Query knowledge list by status
	 */
	public List<AgentKnowledge> getKnowledgeByStatus(Integer agentId, String status) {
		return agentKnowledgeMapper.selectByAgentIdAndStatus(agentId, status);
	}

	/**
	 * Search knowledge
	 */
	public List<AgentKnowledge> searchKnowledge(Integer agentId, String keyword) {
		 return agentKnowledgeMapper.searchByAgentIdAndKeyword(agentId, keyword);
	}

	/**
	 * Batch update knowledge status
	 */
	public int batchUpdateStatus(List<Integer> ids, String status) {
		String sql = "UPDATE agent_knowledge SET status = ?, update_time = ? WHERE id = ?";
		LocalDateTime now = LocalDateTime.now();

		int totalUpdated = 0;
		for (Integer id : ids) {
			totalUpdated += agentKnowledgeMapper.updateStatus(id, status, now);
		}
		return totalUpdated;
	}

	/**
	 * Count agent knowledge
	 */
	public int countKnowledgeByAgent(Integer agentId) {
		return agentKnowledgeMapper.countByAgentId(agentId);
	}

	/**
	 * Count knowledge by types
	 */
	public List<Object[]> countKnowledgeByType(Integer agentId) {
		return agentKnowledgeMapper.countByType(agentId);
	}

}
