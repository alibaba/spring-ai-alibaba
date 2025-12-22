package com.alibaba.cloud.ai.studio.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OverviewQueryRequest {

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;
}