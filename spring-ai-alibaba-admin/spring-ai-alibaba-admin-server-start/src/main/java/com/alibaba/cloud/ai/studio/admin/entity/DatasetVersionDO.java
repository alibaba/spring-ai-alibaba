package com.alibaba.cloud.ai.studio.admin.entity;

import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DatasetVersionDO {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 评测集ID
     */

    private Long datasetId;

    /**
     * 版本号
     */
    private String version;

    /**
     * 版本描述
     */
    private String description;

    /**
     * 该版本的数据总量
     */
    private Integer dataCount;


    /**
     * 版本状态  versionStatus
     */
    private String status;

    /**
     * 实验集合（一对多关系）
     */
    private String experiments;

    /**
     * 数据项集合（一对多关系）
     */
    private String datasetItems;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;





}