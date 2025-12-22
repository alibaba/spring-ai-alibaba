package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import lombok.Data;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
public class DatasetItemCreateRequest {

    /**
     * 测评集ID
     */
    @NotNull(message = "测评集ID不能为空")
    private Long datasetId;

    private List<String> dataContent;

    /**
     * 列结构配置（JSON格式）
     */

    private List<DatasetColumn> columnsConfig;

} 