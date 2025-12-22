package com.alibaba.cloud.ai.studio.admin.mapper;

import com.alibaba.cloud.ai.studio.admin.entity.DatasetItemDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Mapper

public interface DatasetItemMapper {

    /**
     * Create dataset item
     *
     * @param datasetItem dataset item entity
     * @return affected rows
     */
    int insert(DatasetItemDO datasetItem);

    /**
     * Delete dataset item by ID (logical delete)
     *
     * @param id dataset item ID
     * @return affected rows
     */
    int deleteById(@Param("id") Long id);

    /**
     * Get dataset item by ID
     *
     * @param id dataset item ID
     * @return dataset item entity
     */
    DatasetItemDO selectById(@Param("id") Long id);

    /**
     * Query dataset item list
     *
     * @param datasetId dataset ID
     * @param offset    offset
     * @param limit     limit
     * @return dataset item list
     */
    List<DatasetItemDO> list(@Param("datasetId") Long datasetId,
                                   @Param("offset") Long offset,
                                   @Param("limit") int limit);

    /**
     * Query dataset item count
     *
     * @param datasetId dataset ID
     * @return total count
     */
    int selectCount(@Param("datasetId") Long datasetId);

    /**
     * Update dataset item
     *
     * @param id           dataset item ID
     * @param dataContent   data content
     * @return affected rows
     */
    int update(@Param("id") Long id,
               @Param("dataContent") String dataContent);

    /**
     * Find dataset items by dataset ID
     *
     * @param datasetVersionId dataset ID
     * @return dataset item list
     */
    List<DatasetItemDO> selectByDatasetVersionId(@Param("datasetVersionId") Long datasetVersionId);

    /**
     * Find dataset items by dataset ID with pagination
     *
     * @param datasetId dataset ID
     * @param offset    offset
     * @param limit     limit
     * @return dataset item list
     */
    List<DatasetItemDO> selectByDatasetIdWithPagination(@Param("datasetId") Long datasetId,
                                                        @Param("offset") Long offset,
                                                        @Param("limit") int limit);









    /**
     * Find dataset items by dataset ID and item IDs with pagination
     *
     * @param datasetId dataset ID
     * @param itemIds   item ID list
     * @param offset    offset
     * @param limit     limit
     * @return dataset item list
     */
    List<DatasetItemDO> selectByDatasetIdAndItemIdsWithPagination(@Param("datasetId") Long datasetId,
                                                                  @Param("itemIds") List<Long> itemIds,
                                                                  @Param("offset") Long offset,
                                                                  @Param("limit") int limit);



    /**
     * Find dataset items by dataset ID and item IDs with pagination
     *
     * @param datasetId dataset ID
     * @param itemIds   item ID list
     * @return dataset item list
     */
    List<DatasetItemDO> selectByDatasetIdAndItemIds(@Param("datasetId") Long datasetId,
                                                                  @Param("itemIds") List<Long> itemIds);


    /**
     * Batch delete dataset items by IDs
     *
     * @param ids list of dataset item IDs
     * @return affected rows
     */
    int batchDeleteByIds(@Param("ids") List<Long> ids);


} 