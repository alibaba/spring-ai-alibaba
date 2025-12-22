package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.DatasetDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;


@Builder
@Data
public class Dataset {

    /**
     * ID
     */
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
     * 数据条数
     */
    private Integer dataCount;

    /**
     * 最新版本
     */
    private String latestVersion;


    /**
     * 最新版本
     */
    private Long latestVersionId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;






    public static Dataset fromDO(DatasetDO DatasetDO){
        return Dataset.builder()
                .id(DatasetDO.getId())
                .name(DatasetDO.getName())
                .description(DatasetDO.getDescription())
                .columnsConfig(DatasetDO.getColumnsConfig())
                .createTime(DatasetDO.getCreateTime())
                .updateTime(DatasetDO.getUpdateTime())
                .build();
    }

}