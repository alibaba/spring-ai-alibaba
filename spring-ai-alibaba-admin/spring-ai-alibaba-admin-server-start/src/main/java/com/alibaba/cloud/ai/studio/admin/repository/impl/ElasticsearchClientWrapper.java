package com.alibaba.cloud.ai.studio.admin.repository.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchClientWrapper {

    private final ElasticsearchClient elasticsearchClient;

    /**
     * 执行搜索查询
     */
    public SearchResponse<Map> search(String index, SearchRequest searchRequest) {
        try {
            return elasticsearchClient.search(searchRequest, Map.class);
        } catch (IOException e) {
            throw new RuntimeException("搜索请求执行失败", e);
        }
    }

    /**
     * 批量索引文档
     */
    public void bulkIndex(String index, List<Map<String, Object>> documents) {
        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            
            for (Map<String, Object> doc : documents) {
                bulkBuilder.operations(op -> op
                    .index(idx -> idx
                        .index(index)
                        .document(doc)
                    )
                );
            }
            
            BulkResponse result = elasticsearchClient.bulk(bulkBuilder.build());
            
            if (result.errors()) {
                log.error("批量索引部分失败: {}", result.items());
            } else {
                log.info("批量索引成功: {} 条文档", documents.size());
            }
            
        } catch (IOException e) {
            throw new RuntimeException("批量索引失败", e);
        }
    }

    /**
     * 检查索引是否存在
     */
    public boolean indexExists(String indexName) {
        try {
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(indexName));
            return elasticsearchClient.indices().exists(existsRequest).value();
        } catch (IOException e) {
            log.error("检查索引是否存在失败: {}", indexName, e);
            return false;
        }
    }

    /**
     * 转换SearchResponse到Map列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> extractHits(SearchResponse<Map> response) {
        return response.hits().hits().stream()
            .map(hit -> (Map<String, Object>) hit.source())
            .collect(Collectors.toList());
    }

    /**
     * 获取总命中数
     */
    public long getTotalHits(SearchResponse<Map> response) {
        return response.hits().total().value();
    }
}