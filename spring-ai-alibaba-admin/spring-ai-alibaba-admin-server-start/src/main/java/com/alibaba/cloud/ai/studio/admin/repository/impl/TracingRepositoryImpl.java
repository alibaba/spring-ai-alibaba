package com.alibaba.cloud.ai.studio.admin.repository.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.*;
import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.repository.TracingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.CardinalityAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.ValueCountAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.SumAggregate;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class TracingRepositoryImpl implements TracingRepository {

    private static final String TRACES_INDEX = "loongsuite_traces";
    
    private final ElasticsearchClientWrapper elasticsearchClient;
    private final TracingQueryBuilder queryBuilder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PageResult<TraceSpanDTO> queryTraces(TracesQueryRequest request) {
        log.info("查询Traces列表: {}", request);
        
        SearchRequest searchRequest = queryBuilder.buildTracesQuery(request);
        SearchResponse<Map> response = elasticsearchClient.search(TRACES_INDEX, searchRequest);
        
        List<TraceSpanDTO> spans = elasticsearchClient.extractHits(response).stream()
            .map(this::convertToTraceSpanDTO)
            .collect(Collectors.toList());
        
        return buildPageResult(response, spans, request);
    }

    @Override
    public TraceDetailDTO getTraceDetail(String traceId) {
        log.info("查询Trace详情: {}", traceId);
        
        SearchRequest searchRequest = queryBuilder.buildTraceDetailQuery(traceId);
        SearchResponse<Map> response = elasticsearchClient.search(TRACES_INDEX, searchRequest);
        
        List<TraceSpanDTO> spans = elasticsearchClient.extractHits(response).stream()
            .map(this::convertToTraceSpanDTO)
            .collect(Collectors.toList());
        
        return TraceDetailDTO.builder().records(spans).build();
    }

    @Override
    public ServicesResponseDTO getServices(ServicesQueryRequest request) {
        log.info("查询服务列表: {}", request);
        
        SearchRequest searchRequest = queryBuilder.buildServicesQuery(request);
        SearchResponse<Map> response = elasticsearchClient.search(TRACES_INDEX, searchRequest);
        
        List<ServiceInfoDTO> services = new ArrayList<>();
        
        if (response.aggregations() != null) {
            Aggregate servicesAgg = response.aggregations().get("services");
            if (servicesAgg != null && servicesAgg.isSterms()) {
                // 使用String Terms聚合
                var termsAgg = servicesAgg.sterms();
                for (var bucket : termsAgg.buckets().array()) {
                    String serviceName = bucket.key().stringValue();
                    List<String> operations = new ArrayList<>();
                    
                    Aggregate operationsAgg = bucket.aggregations().get("operations");
                    if (operationsAgg != null && operationsAgg.isSterms()) {
                        var opTermsAgg = operationsAgg.sterms();
                        operations = opTermsAgg.buckets().array().stream()
                            .map(opBucket -> opBucket.key().stringValue())
                            .collect(Collectors.toList());
                    }
                    
                    services.add(ServiceInfoDTO.builder()
                        .name(serviceName)
                        .operations(operations)
                        .build());
                }
            }
        }
        
        return ServicesResponseDTO.builder().services(services).build();
    }

    @Override
    public OverviewStatsDTO getOverview(OverviewQueryRequest request) {
        log.info("查询概览统计: {}", request);
        
        SearchRequest searchRequest = queryBuilder.buildOverviewQuery(request);
        SearchResponse<Map> response = elasticsearchClient.search(TRACES_INDEX, searchRequest);
        
        Map<String, Aggregate> aggregations = response.aggregations();
        
        // 构建统计结果 - 所有查询都采用detail模式
        OverviewStatsDTO.StatDetail operationCount = buildOperationCountStats(aggregations, true);
        OverviewStatsDTO.StatDetail modelCount = buildModelCountStats(aggregations, true);
        OverviewStatsDTO.StatDetail usageTokens = buildUsageTokensStats(aggregations, true);
        
        return OverviewStatsDTO.builder()
            .operationCount(operationCount)
            .modelCount(modelCount)
            .usageTokens(usageTokens)
            .build();
    }

    @Override
    public void saveSpans(List<TraceSpanDTO> spans) {
        log.info("批量保存Span数据: {} 条", spans.size());
        
        List<Map<String, Object>> documents = spans.stream()
            .map(this::convertToElasticsearchDoc)
            .collect(Collectors.toList());
        
        elasticsearchClient.bulkIndex(TRACES_INDEX, documents);
    }

    /**
     * 转换为TraceSpanDTO
     */
    @SuppressWarnings("unchecked")
    private TraceSpanDTO convertToTraceSpanDTO(Map<String, Object> source) {
        // 获取 metadata 对象
        Map<String, Object> metadata = (Map<String, Object>) source.get("metadata");
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        
        // 获取时间戳，转换为 ISO8601 格式
        Long startTimeUs = getLong(metadata, "start");
        String startTimeStr = startTimeUs != null ? convertMicrosecondsToISO8601(startTimeUs) : null;
        
        Long endTimeUs = getLong(metadata, "end");
        String endTimeStr = endTimeUs != null ? convertMicrosecondsToISO8601(endTimeUs) : null;

        Long durationUs = getLong(metadata, "duration");
        
        return TraceSpanDTO.builder()
            .traceId(getString(metadata, "traceID"))
            .spanId(getString(metadata, "spanID"))
            .parentSpanId(getString(metadata, "parentSpanID"))
            .durationNs(durationUs != null ? durationUs * 1000 : null) // 微秒转纳秒
            .spanKind(convertSpanKind(getString(metadata, "kind")))
            .service(getString(metadata, "service"))
            .spanName(getString(metadata, "name"))
            .startTime(startTimeStr)
            .endTime(endTimeStr)
            .status(convertStatusCode(getString(metadata, "statusCode")))
            // FIXME: 暂时设为0，后续可根据需要计算
            .errorCount(0)
            .attributes((Map<String, Object>) source.get("attributes"))
            .resources((Map<String, Object>) source.get("resources"))
            .spanLinks(convertSpanLinks((List<Map<String, Object>>) source.get("spanLinks")))
            .spanEvents(convertSpanEvents((List<Map<String, Object>>) source.get("spanEvents")))
            .build();
    }

    /**
     * 转换为Elasticsearch文档
     */
    private Map<String, Object> convertToElasticsearchDoc(TraceSpanDTO span) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("traceId", span.getTraceId());
        doc.put("spanId", span.getSpanId());
        doc.put("parentSpanId", span.getParentSpanId());
        doc.put("operationName", span.getSpanName());
        doc.put("startTime", span.getStartTime());
        doc.put("endTime", span.getEndTime());
        doc.put("duration", span.getDurationNs());
        doc.put("status", span.getStatus());
        doc.put("spanKind", span.getSpanKind());
        doc.put("serviceName", span.getService());
        doc.put("errorCount", span.getErrorCount());
        doc.put("attributes", span.getAttributes());
        doc.put("resources", span.getResources());
        doc.put("links", span.getSpanLinks());
        doc.put("events", span.getSpanEvents());
        return doc;
    }

    /**
     * 构建分页结果
     */
    private PageResult<TraceSpanDTO> buildPageResult(SearchResponse<Map> response, 
                                                   List<TraceSpanDTO> spans, 
                                                   TracesQueryRequest request) {
        long totalCount = elasticsearchClient.getTotalHits(response);
        long totalPage = (totalCount + request.getPageSize() - 1) / request.getPageSize();
        
        PageResult<TraceSpanDTO> result = new PageResult<>();
        result.setTotalCount(totalCount);
        result.setTotalPage(totalPage);
        result.setPageNumber((long) request.getPageNumber());
        result.setPageSize((long) request.getPageSize());
        result.setPageItems(spans);
        
        return result;
    }

    /**
     * 将微秒时间戳转换为ISO8601格式
     */
    private String convertMicrosecondsToISO8601(Long microseconds) {
        if (microseconds == null) {
            return null;
        }
        // 微秒转毫秒
        long milliseconds = microseconds / 1000;
        return java.time.Instant.ofEpochMilli(milliseconds)
            .atZone(java.time.ZoneOffset.UTC)
            .format(java.time.format.DateTimeFormatter.ISO_INSTANT);
    }

    /**
     * 转换Span类型
     */
    private String convertSpanKind(String kind) {
        if (kind == null) {
            return "SPAN_KIND_INTERNAL";
        }
        switch (kind.toLowerCase()) {
            case "client":
                return "SPAN_KIND_CLIENT";
            case "server":
                return "SPAN_KIND_SERVER";
            case "producer":
                return "SPAN_KIND_PRODUCER";
            case "consumer":
                return "SPAN_KIND_CONSUMER";
            case "internal":
            default:
                return "SPAN_KIND_INTERNAL";
        }
    }

    /**
     * 转换状态码
     */
    private String convertStatusCode(String statusCode) {
        if (statusCode == null) {
            return "UNSET";
        }
        switch (statusCode.toUpperCase()) {
            case "OK":
                return "OK";
            case "ERROR":
                return "ERROR";
            case "UNSET":
            default:
                return "UNSET";
        }
    }



    /**
     * 构建操作统计（operation.count）
     */
    private OverviewStatsDTO.StatDetail buildOperationCountStats(Map<String, Aggregate> aggregations, 
                                                               Boolean detail) {
        Long total = 0L;
        List<OverviewStatsDTO.StatItem> detailList = new ArrayList<>();
        
        if (aggregations != null) {
            Aggregate operationCountAgg = aggregations.get("operation_count");
            if (operationCountAgg != null && operationCountAgg.isSterms()) {
                var termsAgg = operationCountAgg.sterms();
                
                // 计算总操作数
                for (var bucket : termsAgg.buckets().array()) {
                    total += bucket.docCount();
                }
                
                if (detail) {
                    detailList = termsAgg.buckets().array().stream()
                        .map(bucket -> OverviewStatsDTO.StatItem.builder()
                            .operationName(bucket.key().stringValue())
                            .total(bucket.docCount())
                            .build())
                        .collect(Collectors.toList());
                }
            }
        }
        
        return OverviewStatsDTO.StatDetail.builder()
            .total(total)
            .detail(detailList)
            .build();
    }

    /**
     * 构建模型统计（model.count）
     */
    private OverviewStatsDTO.StatDetail buildModelCountStats(Map<String, Aggregate> aggregations, 
                                                           Boolean detail) {
        Long total = 0L;
        List<OverviewStatsDTO.StatItem> detailList = new ArrayList<>();
        
        if (aggregations != null) {
            Aggregate modelCountAgg = aggregations.get("model_count");
            if (modelCountAgg != null && modelCountAgg.isSterms()) {
                var termsAgg = modelCountAgg.sterms();
                
                // 计算总模型数
                for (var bucket : termsAgg.buckets().array()) {
                    total += bucket.docCount();
                }
                
                if (detail) {
                    detailList = termsAgg.buckets().array().stream()
                        .map(bucket -> OverviewStatsDTO.StatItem.builder()
                            .modelName(bucket.key().stringValue())
                            .total(bucket.docCount())
                            .build())
                        .collect(Collectors.toList());
                }
            }
        }
        
        return OverviewStatsDTO.StatDetail.builder()
            .total(total)
            .detail(detailList)
            .build();
    }

    /**
     * 构建Token使用统计
     */
    private OverviewStatsDTO.StatDetail buildUsageTokensStats(Map<String, Aggregate> aggregations, 
                                                            Boolean detail) {
        Long total = 0L;
        List<OverviewStatsDTO.StatItem> detailList = new ArrayList<>();
        
        if (aggregations != null) {
            Aggregate usageTokensAgg = aggregations.get("total_usage_tokens");
            if (usageTokensAgg != null && usageTokensAgg.isSterms()) {
                var termsAgg = usageTokensAgg.sterms();
                
                // 计算总token数
                for (var bucket : termsAgg.buckets().array()) {
                    Aggregate totalTokensAgg = bucket.aggregations().get("total_tokens");
                    if (totalTokensAgg != null && totalTokensAgg.isSum()) {
                        SumAggregate sum = totalTokensAgg.sum();
                        Double value = sum.value();
						total += value.longValue();
					}
                }
                
                if (detail) {
                    detailList = termsAgg.buckets().array().stream()
                        .map(bucket -> {
                            String modelName = bucket.key().stringValue();
                            Long tokens = 0L;
                            Aggregate totalTokensAgg = bucket.aggregations().get("total_tokens");
                            if (totalTokensAgg != null && totalTokensAgg.isSum()) {
                                SumAggregate sum = totalTokensAgg.sum();
                                Double value = sum.value();
								tokens = value.longValue();
							}
                            return OverviewStatsDTO.StatItem.builder()
                                .modelName(modelName)
                                .total(tokens)
                                .build();
                        })
                        .collect(Collectors.toList());
                }
            }
        }
        
        return OverviewStatsDTO.StatDetail.builder()
            .total(total)
            .detail(detailList)
            .build();
    }

    // 辅助方法
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                log.warn("failed to parse Long type: {}", value, e);
                return null;
            }
        }
        log.warn("failed to parse Long type: {}", value);
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<SpanLinkDTO> convertSpanLinks(List<Map<String, Object>> links) {
        if (links == null) return new ArrayList<>();
        
        return links.stream()
            .map(link -> SpanLinkDTO.builder()
                .traceId(getString(link, "traceID"))
                .spanId(getString(link, "spanID"))
                .attributes((Map<String, Object>) link.get("attribute"))
                .build())
            .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<SpanEventDTO> convertSpanEvents(List<Map<String, Object>> events) {
        if (events == null) return new ArrayList<>();
        
        return events.stream()
            .map(event -> SpanEventDTO.builder()
                .name(getString(event, "name"))
                .time(convertMicrosecondsToISO8601(getLong(event, "time")))
                .attributes((Map<String, Object>) event.get("attribute"))
                .build())
            .collect(Collectors.toList());
    }
}