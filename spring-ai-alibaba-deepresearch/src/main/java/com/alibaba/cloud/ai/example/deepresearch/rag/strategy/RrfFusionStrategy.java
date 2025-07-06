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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.stream.Collectors;

/** RAG中RRF（Reciprocal Rank Fusion）融合策略实现。
 * 该策略根据文档在多个结果列表中的排名，计算其 RRF 分数，并返回融合后的文档列表。
 *
 * @author hupei
 */
@Component
public class RrfFusionStrategy implements FusionStrategy {

    private final int k;

    public RrfFusionStrategy(@Value("${rag.fusion.rrf.k-constant:60}") int k) {
        this.k = k;
    }

    @Override
    public String getStrategyName() {
        return "rrf";
    }

    @Override
    public List<Document> fuse(List<List<Document>> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        if (results.size() == 1) {
            return results.get(0); // 如果只有一个结果列表，无需融合
        }

        // 使用 Map 来存储每个文档的 RRF 分数，以文档ID为键
        Map<String, Double> rrfScores = new HashMap<>();
        // 使用 Map 来存储文档ID到 Document 对象的映射，避免重复存储
        Map<String, Document> documentMap = new HashMap<>();

        for (List<Document> resultList : results) {
            for (int i = 0; i < resultList.size(); i++) {
                Document doc = resultList.get(i);
                int rank = i + 1; // 排名从1开始

                // 更新文档的 RRF 分数
                rrfScores.merge(doc.getId(), 1.0 / (k + rank), Double::sum);
                // 如果是第一次遇到该文档，则存入 map
                documentMap.putIfAbsent(doc.getId(), doc);
            }
        }

        // 根据 RRF 分数对文档ID进行降序排序
        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(entry -> documentMap.get(entry.getKey()))
                .collect(Collectors.toList());
    }
}

