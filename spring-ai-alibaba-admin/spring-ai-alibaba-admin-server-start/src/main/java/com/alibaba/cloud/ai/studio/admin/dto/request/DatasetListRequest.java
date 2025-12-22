package com.alibaba.cloud.ai.studio.admin.dto.request;

import lombok.Data;


@Data
public class DatasetListRequest {

    /**
     * 页码
     */
    private Integer pageNumber = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;

    /**
     * 按照datasetName查询
     */
    private String datasetName;


} 