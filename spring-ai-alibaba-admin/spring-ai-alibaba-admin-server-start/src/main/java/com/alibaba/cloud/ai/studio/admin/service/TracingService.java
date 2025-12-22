package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.*;
import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;

public interface TracingService {

    /**
     * 分页查询追踪列表
     */
    PageResult<TraceSpanDTO> queryTraces(TracesQueryRequest request);

    /**
     * 根据TraceId获取追踪详情
     */
    TraceDetailDTO getTraceDetail(String traceId);

    /**
     * 获取服务列表
     */
    ServicesResponseDTO getServices(ServicesQueryRequest request);

    /**
     * 获取概览信息
     */
    OverviewStatsDTO getOverview(OverviewQueryRequest request);
}