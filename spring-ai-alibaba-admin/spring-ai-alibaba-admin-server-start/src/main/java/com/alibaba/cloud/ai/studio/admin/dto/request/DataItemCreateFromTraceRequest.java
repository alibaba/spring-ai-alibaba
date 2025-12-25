package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DataItemCreateFromTraceRequest {
    /**
     * 测评集ID
     */
    @NotNull(message = "测评集ID不能为空")
    private Long datasetId;


    @NotNull(message = "测评集版本ID不能为空")
    private Long datasetVersionId;

    private List<String> dataContent;

    /**
     * 列结构配置（JSON格式）
     */

    private List<DatasetColumn> columnsConfig;

}
