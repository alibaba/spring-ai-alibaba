package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.OverviewStatsDTO;
import com.alibaba.cloud.ai.studio.admin.dto.ServicesResponseDTO;
import com.alibaba.cloud.ai.studio.admin.dto.TraceDetailDTO;
import com.alibaba.cloud.ai.studio.admin.dto.TraceSpanDTO;
import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.repository.TracingRepository;
import com.alibaba.cloud.ai.studio.admin.service.TracingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(TracingRepository.class)
public class TracingServiceImpl implements TracingService {

    private final TracingRepository tracingRepository;

    @Override
    public PageResult<TraceSpanDTO> queryTraces(TracesQueryRequest request) {
        log.info("查询Traces列表: {}", request);
        return tracingRepository.queryTraces(request);
    }

    @Override
    public TraceDetailDTO getTraceDetail(String traceId) {
        log.info("查询Trace详情: {}", traceId);
        return tracingRepository.getTraceDetail(traceId);
    }

    @Override
    public ServicesResponseDTO getServices(ServicesQueryRequest request) {
        log.info("查询服务列表: {}", request);
        return tracingRepository.getServices(request);
    }

    @Override
    public OverviewStatsDTO getOverview(OverviewQueryRequest request) {
        log.info("查询概览信息: {}", request);
        return tracingRepository.getOverview(request);
    }
}
