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

import com.alibaba.cloud.ai.entity.AgentPresetQuestion;
import com.alibaba.cloud.ai.mapper.AgentPresetQuestionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AgentPresetQuestion Service Class
 */
@Service
public class AgentPresetQuestionService {

    @Autowired
    private AgentPresetQuestionMapper agentPresetQuestionMapper;

    /**
     * Get the list of preset questions by agent ID (only active ones, ordered by sort_order and id)
     */
    public List<AgentPresetQuestion> findByAgentId(Long agentId) {
        return agentPresetQuestionMapper.selectByAgentId(agentId);
    }

    /**
     * Create a new preset question
     */
    public AgentPresetQuestion create(AgentPresetQuestion question) {
        // Ensure default values
        if (question.getSortOrder() == null) {
            question.setSortOrder(0);
        }
        if (question.getIsActive() == null) {
            question.setIsActive(true);
        }

        agentPresetQuestionMapper.insert(question);
        return question; // ID will be auto-filled by MyBatis
    }

    /**
     * Update an existing preset question
     */
    public void update(Long id, AgentPresetQuestion question) {
        question.setId(id); // Ensure the ID is set
        agentPresetQuestionMapper.update(question);
    }

    /**
     * Delete a preset question by ID
     */
    public void deleteById(Long id) {
        agentPresetQuestionMapper.deleteById(id);
    }

    /**
     * Delete all preset questions for a given agent
     */
    public void deleteByAgentId(Long agentId) {
        agentPresetQuestionMapper.deleteByAgentId(agentId);
    }

    /**
     * Batch save preset questions: delete all existing ones for the agent, then insert the new list
     */
    public void batchSave(Long agentId, List<AgentPresetQuestion> questions) {
        // Step 1: Delete all existing preset questions for the agent
        deleteByAgentId(agentId);

        // Step 2: Insert new questions with proper order and active status
        for (int i = 0; i < questions.size(); i++) {
            AgentPresetQuestion question = questions.get(i);
            question.setAgentId(agentId);
            question.setSortOrder(i);
            question.setIsActive(true);
            create(question); // Reuses create() which sets defaults and inserts
        }
    }
}
