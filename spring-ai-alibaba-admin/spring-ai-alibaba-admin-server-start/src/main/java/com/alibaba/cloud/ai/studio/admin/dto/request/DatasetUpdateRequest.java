package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class DatasetUpdateRequest {

    /**
     * 数据集名称
     */
    @NotNull
    private String name;

    /**
     * 数据集描述
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
    Long datasetId;
} 