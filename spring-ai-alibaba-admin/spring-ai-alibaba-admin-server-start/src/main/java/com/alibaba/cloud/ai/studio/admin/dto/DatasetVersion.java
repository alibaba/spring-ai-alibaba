package com.alibaba.cloud.ai.studio.admin.dto;

import com.alibaba.cloud.ai.studio.admin.entity.DatasetVersionDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DatasetVersion {

    /**
     * 主键ID
     */
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
     * 创建时间
     */
    private LocalDateTime createTime;



    /**
     * 列结构列表（非数据库字段）
     */
    private List<DatasetColumn> columnsConfig;


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
     * 从DO对象转换为DTO对象
     *
     * @param datasetVersionDO DO对象
     * @return DTO对象
     */
    public static DatasetVersion fromDO(DatasetVersionDO datasetVersionDO) {
        if (datasetVersionDO == null) {
            return null;
        }
        return DatasetVersion.builder()
                .id(datasetVersionDO.getId())
                .datasetId(datasetVersionDO.getDatasetId())
                .version(datasetVersionDO.getVersion())
                .description(datasetVersionDO.getDescription())
                .dataCount(datasetVersionDO.getDataCount())
                .createTime(datasetVersionDO.getCreateTime())
                .status(datasetVersionDO.getStatus())
                .experiments(datasetVersionDO.getExperiments())
                .datasetItems(datasetVersionDO.getDatasetItems())
                .build();
    }
} 