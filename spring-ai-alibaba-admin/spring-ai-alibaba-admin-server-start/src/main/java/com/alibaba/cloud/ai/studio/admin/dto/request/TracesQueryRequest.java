package com.alibaba.cloud.ai.studio.admin.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TracesQueryRequest {

    private String serviceName;

    private String traceId;

    private String spanName;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    @Min(value = 1, message = "页码最小值为1")
    private Integer pageNumber = 1;

    @Min(value = 1, message = "每页大小最小值为1")
    @Max(value = 200, message = "每页大小最大值为200")
    private Integer pageSize = 50;

    private String attributes;
}