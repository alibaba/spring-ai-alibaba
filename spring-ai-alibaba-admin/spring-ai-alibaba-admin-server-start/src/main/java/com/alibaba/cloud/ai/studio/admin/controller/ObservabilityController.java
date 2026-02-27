package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.OverviewStatsDTO;
import com.alibaba.cloud.ai.studio.admin.dto.ServicesResponseDTO;
import com.alibaba.cloud.ai.studio.admin.dto.TraceDetailDTO;
import com.alibaba.cloud.ai.studio.admin.dto.TraceSpanDTO;
import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.service.TracingService;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {

    private final TracingService tracingService;

    @Autowired
    public ObservabilityController(@Nullable TracingService tracingService) {
        this.tracingService = tracingService;
    }

    private ResponseEntity<Result<?>> checkTracingService() {
        if (tracingService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Result.error("链路追踪服务未启用，请配置 Elasticsearch"));
        }
        return null;
    }

    /**
     * 获取Trace列表
     */
    @GetMapping("/traces")
    public ResponseEntity<Result<?>> getTraces(@Valid TracesQueryRequest request) {
        ResponseEntity<Result<?>> check = checkTracingService();
        if (check != null) {
            return check;
        }
        log.info("获取Trace列表请求: {}", request);
        PageResult<TraceSpanDTO> result = tracingService.queryTraces(request);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 获取单个Trace详情
     */
    @GetMapping("/traces/{traceId}")
    public ResponseEntity<Result<?>> getTraceDetail(@PathVariable String traceId) {
        ResponseEntity<Result<?>> check = checkTracingService();
        if (check != null) {
            return check;
        }
        log.info("获取Trace详情请求: {}", traceId);
        TraceDetailDTO result = tracingService.getTraceDetail(traceId);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 获取服务列表
     */
    @GetMapping("/services")
    public ResponseEntity<Result<?>> getServices(@Valid ServicesQueryRequest request) {
        ResponseEntity<Result<?>> check = checkTracingService();
        if (check != null) {
            return check;
        }
        log.info("获取服务列表请求: {}", request);
        ServicesResponseDTO result = tracingService.getServices(request);
        return ResponseEntity.ok(Result.success(result));
    }

    /**
     * 获取概览信息
     */
    @GetMapping("/overview")
    public ResponseEntity<Result<?>> getOverview(@Valid OverviewQueryRequest request) {
        ResponseEntity<Result<?>> check = checkTracingService();
        if (check != null) {
            return check;
        }
        log.info("获取概览信息请求: {}", request);
        OverviewStatsDTO result = tracingService.getOverview(request);
        return ResponseEntity.ok(Result.success(result));
    }
}
