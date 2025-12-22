package com.alibaba.cloud.ai.studio.admin.entity;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Builder
@Data
public class DatasetDO {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 评测集名称
     */
    private String name;

    /**
     * 评测集描述
     */
    private String description;

    /**
     * 列结构配置（JSON格式）
     */
    private String columnsConfig;


    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 逻辑删除标识：0-未删除，1-已删除
     */
    private Integer deleted;

}