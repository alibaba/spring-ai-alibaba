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

import com.alibaba.cloud.ai.entity.AgentKnowledge;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.simple.SimpleVectorStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 智能体向量存储服务
 * 专门处理智能体相关的向量存储操作，确保数据隔离
 */
@Service
public class AgentVectorService {

    private static final Logger log = LoggerFactory.getLogger(AgentVectorService.class);

    @Autowired
    private SimpleVectorStoreService vectorStoreService;

    /**
     * 为智能体初始化数据库Schema
     * @param agentId 智能体ID
     * @param schemaInitRequest Schema初始化请求
     * @return 是否成功
     */
    public Boolean initializeSchemaForAgent(Long agentId, SchemaInitRequest schemaInitRequest) {
        try {
            String agentIdStr = String.valueOf(agentId);
            log.info("Initializing schema for agent: {}", agentIdStr);
            
            return vectorStoreService.schemaForAgent(agentIdStr, schemaInitRequest);
        } catch (Exception e) {
            log.error("Failed to initialize schema for agent: {}", agentId, e);
            throw new RuntimeException("Failed to initialize schema for agent " + agentId + ": " + e.getMessage(), e);
        }
    }

    /**
     * 为智能体添加知识文档到向量库
     * @param agentId 智能体ID
     * @param knowledge 知识内容
     */
    public void addKnowledgeToVector(Long agentId, AgentKnowledge knowledge) {
        try {
            String agentIdStr = String.valueOf(agentId);
            log.info("Adding knowledge to vector store for agent: {}, knowledge ID: {}", agentIdStr, knowledge.getId());

            // 创建文档
            Document document = createDocumentFromKnowledge(agentIdStr, knowledge);
            
            // 添加到向量库
            vectorStoreService.getAgentVectorStoreManager().addDocuments(agentIdStr, List.of(document));
            
            log.info("Successfully added knowledge to vector store for agent: {}", agentIdStr);
        } catch (Exception e) {
            log.error("Failed to add knowledge to vector store for agent: {}, knowledge ID: {}", 
                    agentId, knowledge.getId(), e);
            throw new RuntimeException("Failed to add knowledge to vector store: " + e.getMessage(), e);
        }
    }

    /**
     * 为智能体批量添加知识文档到向量库
     * @param agentId 智能体ID
     * @param knowledgeList 知识列表
     */
    public void addKnowledgeListToVector(Long agentId, List<AgentKnowledge> knowledgeList) {
        if (knowledgeList == null || knowledgeList.isEmpty()) {
            log.warn("No knowledge to add for agent: {}", agentId);
            return;
        }

        try {
            String agentIdStr = String.valueOf(agentId);
            log.info("Adding {} knowledge items to vector store for agent: {}", knowledgeList.size(), agentIdStr);

            // 创建文档列表
            List<Document> documents = knowledgeList.stream()
                    .map(knowledge -> createDocumentFromKnowledge(agentIdStr, knowledge))
                    .toList();
            
            // 批量添加到向量库
            vectorStoreService.getAgentVectorStoreManager().addDocuments(agentIdStr, documents);
            
            log.info("Successfully added {} knowledge items to vector store for agent: {}", 
                    documents.size(), agentIdStr);
        } catch (Exception e) {
            log.error("Failed to add knowledge list to vector store for agent: {}", agentId, e);
            throw new RuntimeException("Failed to add knowledge list to vector store: " + e.getMessage(), e);
        }
    }

    /**
     * 从向量库中搜索相关知识
     * @param agentId 智能体ID
     * @param query 查询文本
     * @param topK 返回结果数量
     * @return 相关文档列表
     */
    public List<Document> searchKnowledge(Long agentId, String query, int topK) {
        try {
            String agentIdStr = String.valueOf(agentId);
            log.debug("Searching knowledge for agent: {}, query: {}, topK: {}", agentIdStr, query, topK);

            List<Document> results = vectorStoreService.getAgentVectorStoreManager()
                    .similaritySearch(agentIdStr, query, topK);
            
            log.info("Found {} knowledge documents for agent: {}", results.size(), agentIdStr);
            return results;
        } catch (Exception e) {
            log.error("Failed to search knowledge for agent: {}", agentId, e);
            throw new RuntimeException("Failed to search knowledge: " + e.getMessage(), e);
        }
    }

    /**
     * 从向量库中搜索特定类型的知识
     * @param agentId 智能体ID
     * @param query 查询文本
     * @param topK 返回结果数量
     * @param knowledgeType 知识类型
     * @return 相关文档列表
     */
    public List<Document> searchKnowledgeByType(Long agentId, String query, int topK, String knowledgeType) {
        try {
            String agentIdStr = String.valueOf(agentId);
            log.debug("Searching knowledge by type for agent: {}, query: {}, topK: {}, type: {}", 
                    agentIdStr, query, topK, knowledgeType);

            List<Document> results = vectorStoreService.getAgentVectorStoreManager()
                    .similaritySearchWithFilter(agentIdStr, query, topK, "knowledge:" + knowledgeType);
            
            log.info("Found {} knowledge documents of type '{}' for agent: {}", 
                    results.size(), knowledgeType, agentIdStr);
            return results;
        } catch (Exception e) {
            log.error("Failed to search knowledge by type for agent: {}", agentId, e);
            throw new RuntimeException("Failed to search knowledge by type: " + e.getMessage(), e);
        }
    }

    /**
     * 删除智能体的特定知识文档
     * @param agentId 智能体ID
     * @param knowledgeId 知识ID
     */
    public void deleteKnowledgeFromVector(Long agentId, Integer knowledgeId) {
        try {
            String agentIdStr = String.valueOf(agentId);
            String documentId = agentIdStr + ":knowledge:" + knowledgeId;
            
            log.info("Deleting knowledge from vector store for agent: {}, knowledge ID: {}", agentIdStr, knowledgeId);
            
            vectorStoreService.getAgentVectorStoreManager()
                    .deleteDocuments(agentIdStr, List.of(documentId));
            
            log.info("Successfully deleted knowledge from vector store for agent: {}", agentIdStr);
        } catch (Exception e) {
            log.error("Failed to delete knowledge from vector store for agent: {}, knowledge ID: {}", 
                    agentId, knowledgeId, e);
            throw new RuntimeException("Failed to delete knowledge from vector store: " + e.getMessage(), e);
        }
    }

    /**
     * 删除智能体的所有向量数据
     * @param agentId 智能体ID
     */
    public void deleteAllVectorDataForAgent(Long agentId) {
        try {
            String agentIdStr = String.valueOf(agentId);
            log.info("Deleting all vector data for agent: {}", agentIdStr);
            
            vectorStoreService.getAgentVectorStoreManager().deleteAgentData(agentIdStr);
            
            log.info("Successfully deleted all vector data for agent: {}", agentIdStr);
        } catch (Exception e) {
            log.error("Failed to delete all vector data for agent: {}", agentId, e);
            throw new RuntimeException("Failed to delete all vector data: " + e.getMessage(), e);
        }
    }

    /**
     * 获取智能体向量存储统计信息
     * @param agentId 智能体ID
     * @return 统计信息
     */
    public Map<String, Object> getVectorStatistics(Long agentId) {
        try {
            String agentIdStr = String.valueOf(agentId);
            Map<String, Object> stats = new HashMap<>();
            
            boolean hasData = vectorStoreService.getAgentVectorStoreManager().hasAgentData(agentIdStr);
            int documentCount = vectorStoreService.getAgentVectorStoreManager().getDocumentCount(agentIdStr);
            
            stats.put("agentId", agentId);
            stats.put("hasData", hasData);
            stats.put("documentCount", documentCount);
            
            log.debug("Vector statistics for agent {}: hasData={}, documentCount={}", 
                    agentIdStr, hasData, documentCount);
            
            return stats;
        } catch (Exception e) {
            log.error("Failed to get vector statistics for agent: {}", agentId, e);
            throw new RuntimeException("Failed to get vector statistics: " + e.getMessage(), e);
        }
    }

    /**
     * 从AgentKnowledge创建Document
     */
    private Document createDocumentFromKnowledge(String agentId, AgentKnowledge knowledge) {
        String documentId = agentId + ":knowledge:" + knowledge.getId();
        String content = knowledge.getContent();
        if (content == null || content.trim().isEmpty()) {
            content = knowledge.getTitle(); // 如果内容为空，使用标题
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("agentId", agentId);
        metadata.put("knowledgeId", knowledge.getId());
        metadata.put("title", knowledge.getTitle());
        metadata.put("type", knowledge.getType());
        metadata.put("category", knowledge.getCategory());
        metadata.put("tags", knowledge.getTags());
        metadata.put("status", knowledge.getStatus());
        metadata.put("vectorType", "knowledge:" + knowledge.getType());
        metadata.put("sourceUrl", knowledge.getSourceUrl());
        metadata.put("fileType", knowledge.getFileType());
        metadata.put("embeddingStatus", knowledge.getEmbeddingStatus());
        metadata.put("createTime", knowledge.getCreateTime());

        return new Document(documentId, content, metadata);
    }
}