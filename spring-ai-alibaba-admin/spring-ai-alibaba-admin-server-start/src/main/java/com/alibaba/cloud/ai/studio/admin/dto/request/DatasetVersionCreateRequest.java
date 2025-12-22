package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DatasetVersionCreateRequest {

    /**
     * 数据集Id
     */
    @NotNull
    private Long datasetId;

    /**
     * 数据集版本描述
     */
    private String description;

    /**
     * 列结构配置
     */
    @NotNull
    private List<DatasetColumn> columnsConfig;

    /**
     * 数据集ID
     */
    @NotNull
    private List<Long> datasetItems;


    String status;
} 