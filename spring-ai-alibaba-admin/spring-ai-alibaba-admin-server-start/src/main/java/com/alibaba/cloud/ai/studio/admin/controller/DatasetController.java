package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.admin.dto.Dataset;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetItem;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetVersion;
import com.alibaba.cloud.ai.studio.admin.dto.Experiment;
import com.alibaba.cloud.ai.studio.admin.dto.request.*;
import com.alibaba.cloud.ai.studio.admin.service.DatasetService;
import com.alibaba.cloud.ai.studio.admin.service.DatasetItemService;
import com.alibaba.cloud.ai.studio.admin.service.DatasetVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@Slf4j
@RestController
@RequestMapping("/api/dataset")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final DatasetItemService datasetItemService;
    private final DatasetVersionService datasetVersionService;

    /**
     * 创建测评集
     */
    @PostMapping("/dataset")
    public Result<Dataset> createDataSet(@Validated @RequestBody DatasetCreateRequest datasetCreateRequest) {
        log.info("创建测评集请求: {}", datasetCreateRequest);
        try {
            Dataset dataset = datasetService.create(datasetCreateRequest);
            return Result.success(dataset);
        } catch (Exception e) {
            log.error("创建测评集失败", e);
            return Result.error("创建测评集失败: " + e.getMessage());
        }
    }


    /**
     * 创建测评集版本
     */
    @PostMapping("/datasetVersion")
    public Result<DatasetVersion> createDatasetVersion(@Validated @RequestBody DatasetVersionCreateRequest datasetVersionCreateRequest) {
        log.info("创建测评集新版本请求: {}", datasetVersionCreateRequest);
        try {
            DatasetVersion dataset = datasetVersionService.create(datasetVersionCreateRequest);
            return Result.success(dataset);
        } catch (Exception e) {
            log.error("创建测评集新版本失败", e);
            return Result.error("创建测评集新版本失败: " + e.getMessage());
        }
    }

    /**
     * 获取测评集列表
     */
    @GetMapping("/datasets")
    public Result<PageResult<Dataset>> listDataSet(@Validated  DatasetListRequest request) {
        log.info("查询测评集列表请求: {}", request);
        try {
            PageResult<Dataset> result = datasetService.list(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询测评集列表失败", e);
            return Result.error("查询测评集列表失败: " + e.getMessage());
        }
    }


    /**
     * 获取测评集详情
     */
    @GetMapping("/dataset")
    public Result<Dataset> getDataSet(@RequestParam Long datasetId) {
        log.info("查询测评集详情请求: {}", datasetId);
        try {
            Dataset dataset = datasetService.getById(datasetId);
            return Result.success(dataset);
        } catch (Exception e) {
            log.error("查询测评集详情失败", e);
            return Result.error("查询测评集详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新测评集
     */
    @PutMapping("/dataset")
    public Result<Dataset> updateDataSet(@Validated @RequestBody DatasetUpdateRequest datasetUpdateRequest) {
        log.info("更新测评集请求: {}", datasetUpdateRequest);
        try {
            //todo 从requets中获取dataset
            Dataset updatedDataset = datasetService.update(datasetUpdateRequest);
            return Result.success(updatedDataset);
        } catch (Exception e) {
            log.error("更新测评集失败", e);
            return Result.error("更新测评集失败: " + e.getMessage());
        }
    }

    /**
     * 删除测评集
     */
    @DeleteMapping("/dataset")
    public Result<Void> delete(@RequestParam Long datasetId) {
        log.info("删除测评集请求: {}", datasetId);
        try {
            datasetService.deleteById(datasetId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除测评集失败", e);
            return Result.error("删除测评集失败: " + e.getMessage());
        }
    }

    // ==================== 数据项管理接口 ====================

    /**
     * 创建数据项
     */
    @PostMapping("/dataItem")
    public Result<List<DatasetItem>> createItem(@Validated @RequestBody DatasetItemCreateRequest datasetItemCreateRequest ) {
        log.info("创建数据项请求: {}", datasetItemCreateRequest);
        try {
            List<DatasetItem> dataItem = datasetItemService.create(datasetItemCreateRequest);
            return Result.success(dataItem);
        } catch (Exception e) {
            log.error("创建数据项失败", e);
            return Result.error("创建数据项失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据项列表
     */
    @GetMapping("/dataItems")
    public Result<PageResult<DatasetItem>> listItems(@Validated  DatasetItemListRequest request) {
        log.info("查询数据项列表请求: {}", request);
        try {
            PageResult<DatasetItem> result = datasetItemService.list(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询数据项列表失败", e);
            return Result.error("查询数据项列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取数据项详情
     */
    @GetMapping("/dataItem")
    public Result<DatasetItem> getItem(@PathVariable Long id) {
        log.info("查询数据项详情请求: {}", id);
        try {
            DatasetItem dataItem = datasetItemService.getById(id);
            return Result.success(dataItem);
        } catch (Exception e) {
            log.error("查询数据项详情失败", e);
            return Result.error("查询数据项详情失败: " + e.getMessage());
        }
    }

    /**
     * 更新数据项
     */
    @PutMapping("/dataItem")
    public Result<DatasetItem> updateItem(@RequestBody  DatasetItemUpdateRequest request) {
        log.info("更新数据项请求: {}", request);
        try {
            DatasetItem updatedDataItem = datasetItemService.update(request);
            return Result.success(updatedDataItem);
        } catch (Exception e) {
            log.error("更新数据项失败", e);
            return Result.error("更新数据项失败: " + e.getMessage());
        }
    }

    /**
     * 删除数据项
     */
    @DeleteMapping("/dataItem")
    public Result<Void> deleteItem(@RequestParam Long id) {
        log.info("删除数据项请求: {}", id);
        try {
            datasetItemService.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除数据项失败", e);
            return Result.error("删除数据项失败: " + e.getMessage());
        }
    }



    /**
     * 获取测评集列表
     */
    @GetMapping("/datasetVersions")
    public Result<PageResult<DatasetVersion>> listDataSetVersion(@Validated  DatasetVersionListRequest request) {
        log.info("查询测评集列表请求: {}", request);
        try {
            PageResult<DatasetVersion> result = datasetVersionService.list(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询测评集列表失败", e);
            return Result.error("查询测评集列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新测评集
     */
    @PutMapping("/datasetVersion")
    public Result<DatasetVersion> updateDataSetVersion(@Validated @RequestBody DatasetVersionUpdateRequest datasetVersionUpdateRequest) {
        log.info("更新测评集请求: {}", datasetVersionUpdateRequest);
        try {
            DatasetVersion updatedDataset = datasetVersionService.update(datasetVersionUpdateRequest);
            return Result.success(updatedDataset);
        } catch (Exception e) {
            log.error("更新测评集失败", e);
            return Result.error("更新测评集失败: " + e.getMessage());
        }
    }

    /**
     * 获取关联实验
     */
    @GetMapping("/experiments")
    public Result<PageResult<Experiment>> getExperiments(DatasetExperimentsListRequest datasetExperimentsListRequest) {
        log.info("获取数据集关联的实验: {}", datasetExperimentsListRequest);
        try {
            PageResult<Experiment> experiments = datasetVersionService.getExperiments(datasetExperimentsListRequest);
            return Result.success(experiments);
        } catch (Exception e) {
            log.error("更新测评集失败", e);
            return Result.error("更新测评集失败: " + e.getMessage());
        }
    }



    /**
     * 创建数据项
     */
    @PostMapping("/dataItemFromTrace")
    public Result<List<DatasetItem>> createItem(@Validated @RequestBody DataItemCreateFromTraceRequest dataItemCreateFromTraceRequest ) {
        log.info("从trace创建数据项: {}", dataItemCreateFromTraceRequest);
        try {
            List<DatasetItem> dataItem = datasetItemService.createFromTrace(dataItemCreateFromTraceRequest);
            return Result.success(dataItem);
        } catch (Exception e) {
            log.error("创建数据项失败", e);
            return Result.error("创建数据项失败: " + e.getMessage());
        }
    }



} 