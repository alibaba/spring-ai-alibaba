package com.alibaba.cloud.ai.studio.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TraceDetailDTO {

    private List<TraceSpanDTO> records;
}