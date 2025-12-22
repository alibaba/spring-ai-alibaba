package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.admin.dto.*;
import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.service.TracingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/observability")
@RequiredArgsConstructor
public class ObservabilityController {

    private final TracingService tracingService;

    /**
     * 获取Trace列表
     */
    @GetMapping("/traces")
    public Result<PageResult<TraceSpanDTO>> getTraces(@Valid TracesQueryRequest request) {
        log.info("获取Trace列表请求: {}", request);
        PageResult<TraceSpanDTO> result = tracingService.queryTraces(request);
        return Result.success(result);
    }

    /**
     * 获取单个Trace详情
     */
    @GetMapping("/traces/{traceId}")
    public Result<TraceDetailDTO> getTraceDetail(@PathVariable String traceId) {
        log.info("获取Trace详情请求: {}", traceId);
        TraceDetailDTO result = tracingService.getTraceDetail(traceId);
        return Result.success(result);
    }

    /**
     * 获取服务列表
     */
    @GetMapping("/services")
    public Result<ServicesResponseDTO> getServices(@Valid ServicesQueryRequest request) {
        log.info("获取服务列表请求: {}", request);
        ServicesResponseDTO result = tracingService.getServices(request);
        return Result.success(result);
    }

    /**
     * 获取概览信息
     */
    @GetMapping("/overview")
    public Result<OverviewStatsDTO> getOverview(@Valid OverviewQueryRequest request) {
        log.info("获取概览信息请求: {}", request);
        OverviewStatsDTO result = tracingService.getOverview(request);
        return Result.success(result);
    }
}