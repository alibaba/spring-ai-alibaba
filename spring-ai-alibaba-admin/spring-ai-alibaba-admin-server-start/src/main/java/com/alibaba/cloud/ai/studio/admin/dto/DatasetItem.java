package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.DatasetItemDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DatasetItem {

    /**
     * 主键ID
     */
    private Long id;

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

    /**
     * 从DO对象转换为DTO对象
     *
     * @param datasetItemDO DO对象
     * @return DTO对象
     */
    public static DatasetItem fromDO(DatasetItemDO datasetItemDO) {
        if (datasetItemDO == null) {
            return null;
        }
        return DatasetItem.builder()
                .id(datasetItemDO.getId())
                .columnsConfig(datasetItemDO.getColumnsConfig())
                .dataContent(datasetItemDO.getDataContent())
                .createTime(datasetItemDO.getCreateTime())
                .updateTime(datasetItemDO.getUpdateTime())
                .build();
    }
} 