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
public class UserFileRetrievalStrategy implements RetrievalStrategy {

    private final VectorStore vectorStore;

    public UserFileRetrievalStrategy(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String getStrategyName() {
        return "userFile";
    }

    @Override
    public List<Document> retrieve(String query, Map<String, Object> options) {
        String sessionId = (String) options.get("session_id");
        if (sessionId == null || sessionId.isBlank()) {
            // 如果没有 session_id，此策略不应返回任何内容
            return List.of();
        }

        // 使用 Filter.Expression 构建精确的元数据过滤器
        // 这确保了我们只在当前会话的、用户上传的文件中进行搜索
        var filterBuilder = new FilterExpressionBuilder();
        var filterExpression = filterBuilder.and(
                filterBuilder.eq("source_type", "user_upload"),
                filterBuilder.eq("session_id", sessionId)
        ).build();

        // 创建并执行带有过滤器的相似性搜索请求
        //todo: 添加配置项，例如 topK 和 similarityThreshold
        SearchRequest searchRequest = SearchRequest.builder().query(query)
                .topK(5) // 可配置
                .similarityThreshold(0.7) // 可配置
                .filterExpression(filterExpression).build();

        return vectorStore.similaritySearch(searchRequest);
    }
}
