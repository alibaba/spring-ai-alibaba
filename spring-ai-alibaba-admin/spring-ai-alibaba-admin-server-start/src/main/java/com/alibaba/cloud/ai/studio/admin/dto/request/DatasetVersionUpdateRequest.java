package com.alibaba.cloud.ai.studio.admin.dto.request;

import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import lombok.Data;

import java.util.List;

@Data
public class DatasetVersionUpdateRequest {

    /**
     * 数据集版本Id描述
     */
    private Long datasetVersionId;


    /**
     * 数据集版本描述
     */
    private String description;


    /**
     * 数据集版本状态
     */

    private String status;

} 