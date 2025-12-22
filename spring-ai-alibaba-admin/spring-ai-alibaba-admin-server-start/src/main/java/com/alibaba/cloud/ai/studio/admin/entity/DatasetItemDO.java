package com.alibaba.cloud.ai.studio.admin.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DatasetItemDO {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 数据集ID
     */
    private Long datasetId;

    /**
     * 列结构配置（JSON格式）
     */

    private String columnsConfig;

    /**
     * 数据内容（JSON格式）
     */

    private String dataContent;


    /**
     * 创建时间
     */

    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}