package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetItem;
import com.alibaba.cloud.ai.studio.admin.dto.request.DataItemCreateFromTraceRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetItemCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetItemListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetItemUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.DatasetItemDO;
import com.alibaba.cloud.ai.studio.admin.entity.DatasetVersionDO;
import com.alibaba.cloud.ai.studio.admin.mapper.DatasetItemMapper;
import com.alibaba.cloud.ai.studio.admin.mapper.DatasetVersionMapper;
import com.alibaba.cloud.ai.studio.admin.service.DatasetItemService;
import com.alibaba.cloud.ai.studio.admin.utils.CommonUtils;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service

public class DatasetItemServiceImpl implements DatasetItemService {

    @Resource
    private DatasetItemMapper datasetItemMapper;
    
    @Resource
    private DatasetVersionMapper datasetVersionMapper;


    @Override

    public List<DatasetItem> create(DatasetItemCreateRequest request) {
        log.info("创建数据项: {}", request);

        List<DatasetItem> datasetItemList = request.getDataContent().stream().map(
                dataContent -> {
                    DatasetItemDO datasetItemDO = DatasetItemDO.builder()
                            .datasetId(request.getDatasetId())
                            .dataContent(dataContent)
                            .columnsConfig(JSONObject.toJSONString(request.getColumnsConfig()))
                            .build();
                    datasetItemMapper.insert(datasetItemDO);
                    return DatasetItem.fromDO(datasetItemDO);
                }
        ).toList();

        return  datasetItemList;
    }

    @Override
    public List<DatasetItem> createFromTrace(DataItemCreateFromTraceRequest request) {
        log.info("从Trace创建数据项: {}", request);
        List<Long> itemIds = new ArrayList<>(List.of());

        List<DatasetItem> datasetItemList = request.getDataContent().stream().map(
                dataContent -> {
                    DatasetItemDO datasetItemDO = DatasetItemDO.builder()
                            .datasetId(request.getDatasetId())
                            .dataContent(dataContent)
                            .columnsConfig(JSONObject.toJSONString(request.getColumnsConfig()))
                            .build();
                    datasetItemMapper.insert(datasetItemDO);
                    itemIds.add(datasetItemDO.getId());
                    return DatasetItem.fromDO(datasetItemDO);
                }
        ).toList();

        if (!itemIds.isEmpty()) {
            // 更新数据集版本中的数据项列表
            DatasetVersionDO datasetVersionDO = datasetVersionMapper.selectById(request.getDatasetVersionId());
            if (datasetVersionDO == null) {
                throw new RuntimeException("数据集版本不存在: " + request.getDatasetVersionId());
            }

            List<Long> datasetItems = CommonUtils.parseItemIds(datasetVersionDO.getDatasetItems());
            datasetItems.addAll(itemIds);

            datasetVersionMapper.updateDatasetItems(datasetVersionDO.getId(),
                    datasetItems.toString(),datasetItems.size()
            );

        }

        return  datasetItemList;

    }

    @Override
    public PageResult<DatasetItem> list(DatasetItemListRequest request) {
        log.info("根据数据集版本ID查询数据项列表: datasetVersionId={}, pageNumber={}, pageSize={}", request.getDatasetVersionId(), request.getPageNumber(), request.getPageSize());

        DatasetVersionDO datasetVersion = datasetVersionMapper.selectById(request.getDatasetVersionId());
        if (datasetVersion == null) {
            throw new RuntimeException("数据集版本不存在: " + request.getDatasetVersionId());
        }

        List<Long> itemIds = CommonUtils.parseItemIds(datasetVersion.getDatasetItems());
        if (itemIds.isEmpty()) {
            PageResult<DatasetItem> result = new PageResult<>();
            result.setTotalCount(0L);
            result.setTotalPage(0L);
            result.setPageNumber(request.getPageNumber().longValue());
            result.setPageSize(request.getPageSize().longValue());
            result.setPageItems(new ArrayList<>());
            return result;
        }

        long offset = (request.getPageNumber() - 1L) * request.getPageSize();

        List<DatasetItemDO> datasetItemDOs = datasetItemMapper.selectByDatasetIdAndItemIdsWithPagination(
                datasetVersion.getDatasetId(), itemIds, offset, request.getPageSize());

        List<DatasetItem> datasetItems = datasetItemDOs.stream()
                .map(DatasetItem::fromDO)
                .collect(Collectors.toList());

        return new PageResult<>(
                (long) itemIds.size(),  (long) request.getPageNumber(), (long) request.getPageSize(), datasetItems
        );

    }

    @Override
    public DatasetItem getById(Long id) {
        log.info("查询数据项详情: {}", id);
        DatasetItemDO datasetItemDO = datasetItemMapper.selectById(id);
        return DatasetItem.fromDO(datasetItemDO);
    }

    @Override

    public DatasetItem update(DatasetItemUpdateRequest request) {
        log.info("更新数据项: {}", request);
        
        // 检查数据项是否存在
        DatasetItemDO existingData = datasetItemMapper.selectById(request.getId());

        if(Objects.isNull(existingData)){
            log.warn("尝试更新不存在的数据项: {}", request.getId());
        }


        datasetItemMapper.update(request.getId(),request.getDataContent());

        return getById(request.getId());
    }

    @Override

    public void deleteById(Long id) {
        log.info("删除数据项: {}", id);
        
        // 数据验证
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("数据项ID不能为空且必须大于0");
        }
        
        // 检查数据项是否存在
        DatasetItemDO existingItem = datasetItemMapper.selectById(id);
        if (existingItem == null) {
            log.warn("尝试删除不存在的数据项: {}", id);
            throw new RuntimeException("数据项不存在: " + id);
        }
        

        int result = datasetItemMapper.deleteById(id);
        if (result <= 0) {
            log.error("数据项删除失败: {}", id);
            throw new RuntimeException("数据项删除失败: " + id);
        }
        
        log.info("数据项删除成功: {}", id);
    }

    @Override
    public void batchDelete(List<Long> ids) {
        log.info("批量删除数据项: {}", JSONObject.toJSONString(ids));

        datasetItemMapper.batchDeleteByIds(ids);

        log.info("批量删除数据项完成");
    }


}