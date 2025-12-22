package com.alibaba.cloud.ai.studio.admin.repository.impl;

import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.HashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class TracingQueryBuilder {

    private static final String TRACES_INDEX = "loongsuite_traces";
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 构建Traces查询请求
     */
    public SearchRequest buildTracesQuery(TracesQueryRequest request) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        // 时间范围过滤 - 使用微秒时间戳
        if (StringUtils.hasText(request.getStartTime()) && StringUtils.hasText(request.getEndTime())) {
            try {
                // 将ISO8601时间转换为微秒时间戳
                Long startTimeMicros = convertISO8601ToMicroseconds(request.getStartTime());
                Long endTimeMicros = convertISO8601ToMicroseconds(request.getEndTime());
                
                if (startTimeMicros != null && endTimeMicros != null) {
                    // 使用 withJson 方法构建查询
                    String rangeQueryJson = String.format(
                        "{\"range\":{\"metadata.start\":{\"gte\":%d,\"lte\":%d}}}",
                        startTimeMicros, endTimeMicros
                    );
                    
                    Query timeRangeQuery = Query.of(q -> q
                        .withJson(new java.io.ByteArrayInputStream(rangeQueryJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    );
                    boolQueryBuilder.filter(timeRangeQuery);
                    log.debug("添加时间范围过滤: {} - {} (微秒: {} - {})", 
                        request.getStartTime(), request.getEndTime(), startTimeMicros, endTimeMicros);
                }
            } catch (Exception e) {
                log.error("构建时间范围查询失败", e);
            }
        }

        // 服务名过滤
        if (StringUtils.hasText(request.getServiceName())) {
            Query serviceQuery = Query.of(q -> q.term(t -> t
                .field("metadata.service")
                .value(request.getServiceName())
            ));
            boolQueryBuilder.filter(serviceQuery);
        }

        // Trace ID过滤
        if (StringUtils.hasText(request.getTraceId())) {
            Query traceIdQuery = Query.of(q -> q.term(t -> t
                .field("metadata.traceID")
                .value(request.getTraceId())
            ));
            boolQueryBuilder.filter(traceIdQuery);
        }

        // Span名称过滤
        if (StringUtils.hasText(request.getSpanName())) {
            Query spanNameQuery = Query.of(q -> q.term(t -> t
                .field("metadata.name")
                .value(request.getSpanName())
            ));
            boolQueryBuilder.filter(spanNameQuery);
        }

        // 属性过滤
        if (StringUtils.hasText(request.getAttributes())) {
            addAttributesFilter(boolQueryBuilder, request.getAttributes());
        }

        // 构建搜索请求
        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(TRACES_INDEX)
            .query(Query.of(q -> q.bool(boolQueryBuilder.build())))
            .from((request.getPageNumber() - 1) * request.getPageSize())
            .size(request.getPageSize())
            .sort(s -> s.field(f -> f.field("metadata.start").order(SortOrder.Desc)));

        return searchBuilder.build();
    }

    /**
     * 构建Trace详情查询请求
     */
    public SearchRequest buildTraceDetailQuery(String traceId) {
        // 修正：使用 metadata.traceID 字段查询
        Query traceQuery = Query.of(q -> q.term(t -> t
            .field("metadata.traceID")
            .value(traceId)
        ));
        
        return SearchRequest.of(s -> s
            .index(TRACES_INDEX)
            .query(traceQuery)
            .size(1000)
            .sort(sort -> sort.field(f -> f.field("metadata.start").order(SortOrder.Asc)))
        );
    }

    /**
     * 构建服务查询请求
     */
    public SearchRequest buildServicesQuery(ServicesQueryRequest request) {
        // 构建聚合
        Map<String, Aggregation> aggregations = new HashMap<>();
        aggregations.put("services", Aggregation.of(a -> a
            .terms(t -> t.field("metadata.service").size(1000))
            .aggregations("operations", Aggregation.of(sub -> sub
                .terms(subTerms -> subTerms.field("metadata.name").size(1000))
            ))
        ));

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(TRACES_INDEX)
            .size(0)
            .aggregations(aggregations);

        // 时间范围过滤 - 使用微秒时间戳
        if (StringUtils.hasText(request.getStartTime()) && StringUtils.hasText(request.getEndTime())) {
            try {
                // 将ISO8601时间转换为微秒时间戳
                Long startTimeMicros = convertISO8601ToMicroseconds(request.getStartTime());
                Long endTimeMicros = convertISO8601ToMicroseconds(request.getEndTime());
                
                if (startTimeMicros != null && endTimeMicros != null) {
                    // 使用 withJson 方法构建查询
                    String rangeQueryJson = String.format(
                        "{\"range\":{\"metadata.start\":{\"gte\":%d,\"lte\":%d}}}",
                        startTimeMicros, endTimeMicros
                    );
                    
                    Query timeRangeQuery = Query.of(q -> q
                        .withJson(new java.io.ByteArrayInputStream(rangeQueryJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    );
                    
                    searchBuilder.query(timeRangeQuery);
                    log.debug("添加服务查询时间范围过滤: {} - {} (微秒: {} - {})", 
                        request.getStartTime(), request.getEndTime(), startTimeMicros, endTimeMicros);
                }
            } catch (Exception e) {
                log.error("构建服务查询时间范围失败", e);
            }
        }

        return searchBuilder.build();
    }

    /**
     * 构建概览查询请求
     */
    public SearchRequest buildOverviewQuery(OverviewQueryRequest request) {
        Map<String, Aggregation> aggregations = new HashMap<>();

        // 根据API文档修正聚合字段
        // 1. 操作类型统计
        aggregations.put("operation_count", Aggregation.of(a -> a
            .terms(t -> t.field("attributes.gen_ai.operation.name").size(1000).missing("generic"))
        ));

        // 2. 模型统计
        aggregations.put("model_count", Aggregation.of(a -> a
            .terms(t -> t.field("attributes.gen_ai.request.model").size(1000))
        ));

        // 3. Token使用统计 - 按模型分组
        aggregations.put("total_usage_tokens", Aggregation.of(a -> a
            .terms(t -> t.field("attributes.gen_ai.request.model").size(1000))
            .aggregations("total_tokens", Aggregation.of(sub -> sub
                .sum(s -> s.field("usage.total_tokens"))
            ))
            .aggregations("input_tokens", Aggregation.of(sub -> sub
                .sum(s -> s.field("usage.input_tokens"))
            ))
            .aggregations("output_tokens", Aggregation.of(sub -> sub
                .sum(s -> s.field("usage.output_tokens"))
            ))
        ));

        SearchRequest.Builder searchBuilder = new SearchRequest.Builder()
            .index(TRACES_INDEX)
            .size(0)
            .aggregations(aggregations);

        // 时间范围过滤 - 使用微秒时间戳
        if (StringUtils.hasText(request.getStartTime()) && StringUtils.hasText(request.getEndTime())) {
            try {
                // 将ISO8601时间转换为微秒时间戳
                Long startTimeMicros = convertISO8601ToMicroseconds(request.getStartTime());
                Long endTimeMicros = convertISO8601ToMicroseconds(request.getEndTime());
                
                if (startTimeMicros != null && endTimeMicros != null) {
                    // 使用 withJson 方法构建查询
                    String rangeQueryJson = String.format(
                        "{\"range\":{\"metadata.start\":{\"gte\":%d,\"lte\":%d}}}",
                        startTimeMicros, endTimeMicros
                    );
                    
                    Query timeRangeQuery = Query.of(q -> q
                        .withJson(new java.io.ByteArrayInputStream(rangeQueryJson.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                    );
                    
                    searchBuilder.query(timeRangeQuery);
                    log.debug("添加概览查询时间范围过滤: {} - {} (微秒: {} - {})", 
                        request.getStartTime(), request.getEndTime(), startTimeMicros, endTimeMicros);
                }
            } catch (Exception e) {
                log.error("构建概览查询时间范围失败", e);
            }
        }

        return searchBuilder.build();
    }

    /**
     * 添加属性过滤条件
     */
    private void addAttributesFilter(BoolQuery.Builder boolQuery, String attributesJson) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> attributesMap = objectMapper.readValue(attributesJson, Map.class);
            
            for (Map.Entry<String, Object> entry : attributesMap.entrySet()) {
                String field = "attributes." + entry.getKey();
                Query attrQuery = Query.of(q -> q.term(t -> t
                    .field(field)
                    .value(String.valueOf(entry.getValue()))
                ));
                boolQuery.filter(attrQuery);
            }
        } catch (Exception e) {
            log.warn("解析属性过滤条件失败: {}", attributesJson, e);
        }
    }



    /**
     * 将ISO8601时间字符串转换为微秒时间戳
     */
    private Long convertISO8601ToMicroseconds(String iso8601Time) {
        try {
            java.time.Instant instant = java.time.Instant.parse(iso8601Time);
            // 转换为微秒
            return instant.toEpochMilli() * 1000;
        } catch (Exception e) {
            log.error("时间转换失败: {}", iso8601Time, e);
            return null;
        }
    }
}