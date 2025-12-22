package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.DatasetVersionDO;
import jakarta.validation.constraints.NotNull;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface DatasetVersionMapper {

    /**
     * Create dataset version
     *
     * @param datasetVersion dataset version entity
     * @return affected rows
     */
    int insert(DatasetVersionDO datasetVersion);

    /**
     * Delete dataset version by ID (logical delete)
     *
     * @param id dataset version ID
     * @return affected rows
     */
    int deleteById(@Param("id") Long id);

    /**
     * Get dataset version by ID
     *
     * @param id dataset version ID
     * @return dataset version entity
     */
    DatasetVersionDO selectById(@Param("id") Long id);

    /**
     * Query dataset version list
     *
     * @param datasetId dataset ID
     * @param offset    offset
     * @param limit     limit
     * @return dataset version list
     */
    List<DatasetVersionDO> selectList(@Param("datasetId") Long datasetId,
                                      @Param("offset") Long offset,
                                      @Param("limit") int limit);

    /**
     * Query dataset version count
     *
     * @param datasetId dataset ID
     * @return total count
     */
    int selectCount(@Param("datasetId") Long datasetId);

    /**
     * Update dataset version
     *
     * @param id          dataset version ID
     * @param description version description
     * @param status      version status
     * @return affected rows
     */
    int update(@Param("id") Long id,
               @Param("description") String description,
               @Param("status") String status);


    /**
     * Update dataset version  dataItemList
     *
     * @param id          dataset version ID
     * @param datasetItems      datasetItems
     * @return affected rows
     */
    int updateDatasetItems(@Param("id") Long id,
               @Param("datasetItems") String datasetItems, @Param("dataCount") Integer dataCount);




    int updateExperiments(@Param("id") Long id,
               @Param("experiments") String experiments);


    /**
     * Delete dataset versions by dataset ID
     *
     * @param datasetId dataset ID
     * @return affected rows
     */
    int deleteByDatasetId(@Param("datasetId") Long datasetId);




    DatasetVersionDO selectLatestVersion(@Param("datasetId") Long datasetId);
}