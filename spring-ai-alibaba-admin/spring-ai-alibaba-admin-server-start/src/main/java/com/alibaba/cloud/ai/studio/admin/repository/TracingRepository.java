package com.alibaba.cloud.ai.studio.admin.repository;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.*;
import com.alibaba.cloud.ai.studio.admin.dto.request.OverviewQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ServicesQueryRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.TracesQueryRequest;

public interface TracingRepository {

    /**
     * 查询Traces列表
     */
    PageResult<TraceSpanDTO> queryTraces(TracesQueryRequest request);

    /**
     * 根据TraceId查询Trace详情
     */
    TraceDetailDTO getTraceDetail(String traceId);

    /**
     * 查询服务列表
     */
    ServicesResponseDTO getServices(ServicesQueryRequest request);

    /**
     * 查询概览统计信息
     */
    OverviewStatsDTO getOverview(OverviewQueryRequest request);

    /**
     * 批量保存Span数据
     */
    void saveSpans(java.util.List<TraceSpanDTO> spans);
}