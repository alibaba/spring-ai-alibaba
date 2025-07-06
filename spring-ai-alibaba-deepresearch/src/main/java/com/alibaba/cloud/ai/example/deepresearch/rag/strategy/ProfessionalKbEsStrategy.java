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
package com.alibaba.cloud.ai.example.deepresearch.rag.strategy;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ProfessionalKbEsStrategy implements RetrievalStrategy {

    private final VectorStore vectorStore;

    public ProfessionalKbEsStrategy(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String getStrategyName() {
        return "professionalKbEs";
    }

    @Override
    public List<Document> retrieve(String query, Map<String, Object> options) {
        // 使用 Filter.Expression 构建精确的元数据过滤器
        // 只需按 source_type 过滤，查询所有标记为专业知识库的文档
        var filterBuilder = new FilterExpressionBuilder();
        var filterExpression = filterBuilder.eq("session_id", "professional_kb_es").build();
        //todo: 添加配置项，例如 topK 和 similarityThreshold
        SearchRequest searchRequest = SearchRequest.builder().query(query)
                .topK(5) // 可配置
                .similarityThreshold(0.7) // 可配置
                .filterExpression(filterExpression).build();

        return vectorStore.similaritySearch(searchRequest);
    }
}