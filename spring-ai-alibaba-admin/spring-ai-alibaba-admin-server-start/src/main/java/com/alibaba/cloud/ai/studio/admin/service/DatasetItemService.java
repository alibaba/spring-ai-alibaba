package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetItem;
import com.alibaba.cloud.ai.studio.admin.dto.request.DataItemCreateFromTraceRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetItemCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetItemListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetItemUpdateRequest;

import java.util.List;

public interface DatasetItemService {

    /**
     * 创建数据项
     */
    List<DatasetItem> create(DatasetItemCreateRequest request);

    /**
     * 从Trace创建数据项
     */
    List<DatasetItem> createFromTrace(DataItemCreateFromTraceRequest request);

    /**
     * 分页查询数据项列表
     */
    PageResult<DatasetItem> list(DatasetItemListRequest request);

    /**
     * 根据ID获取数据项
     */
    DatasetItem getById(Long id);

    /**
     * 更新数据项
     */
    DatasetItem update(DatasetItemUpdateRequest request);

    /**
     * 根据ID删除数据项
     */
    void deleteById(Long id);

    /**
     * 批量删除数据项
     */
    void batchDelete(List<Long> ids);

} 