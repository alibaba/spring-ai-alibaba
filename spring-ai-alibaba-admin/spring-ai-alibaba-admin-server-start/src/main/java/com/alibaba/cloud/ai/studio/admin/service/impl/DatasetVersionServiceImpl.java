package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetVersion;
import com.alibaba.cloud.ai.studio.admin.dto.Experiment;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetExperimentsListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetVersionCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetVersionListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetVersionUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.DatasetVersionDO;
import com.alibaba.cloud.ai.studio.admin.entity.ExperimentDO;
import com.alibaba.cloud.ai.studio.admin.mapper.DatasetMapper;
import com.alibaba.cloud.ai.studio.admin.mapper.DatasetVersionMapper;
import com.alibaba.cloud.ai.studio.admin.mapper.ExperimentMapper;
import com.alibaba.cloud.ai.studio.admin.service.DatasetVersionService;
import com.alibaba.cloud.ai.studio.admin.utils.VersionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetVersionServiceImpl implements DatasetVersionService {

    private final DatasetVersionMapper datasetVersionMapper;
    private final DatasetMapper datasetMapper;
    private final ExperimentMapper experimentMapper;

    @Override
    @Transactional
    public DatasetVersion create(DatasetVersionCreateRequest request) {
        log.info("Creating dataset version: {}", request);

        // Validate request
        if (request.getDatasetId() == null) {
            throw new IllegalArgumentException("Dataset ID cannot be null");
        }

        //检查 dataset是否存在
        if (datasetMapper.selectById(request.getDatasetId()) == null) {
            throw new IllegalArgumentException("Dataset not found: " + request.getDatasetId());
        }

        // 获取当前最大的version
        DatasetVersionDO latestDatasetVersionDO = datasetVersionMapper.selectLatestVersion(request.getDatasetId());

        String currentVersion = Objects.isNull(latestDatasetVersionDO)?null:latestDatasetVersionDO.getVersion();

        // Generate version number
        String versionNumber = VersionUtils.generateVersionNumber(currentVersion);


        // Build entity
        DatasetVersionDO datasetVersionDO = DatasetVersionDO.builder()
                .datasetId(request.getDatasetId())
                .version(versionNumber)
                .description(request.getDescription())
                .dataCount(request.getDatasetItems().size())
                .createTime(LocalDateTime.now())
                .status(request.getStatus() != null ? request.getStatus() : "DRAFT")
                .experiments("[]")
                .datasetItems(request.getDatasetItems() != null ? request.getDatasetItems().toString() : "[]")
                .build();

        // Insert into database
        int result = datasetVersionMapper.insert(datasetVersionDO);
        if (result <= 0) {
            throw new RuntimeException("Failed to create dataset version");
        }

        log.info("Dataset version created successfully: {}", datasetVersionDO);
        return DatasetVersion.fromDO(datasetVersionDO);
    }

    @Override
    public PageResult<DatasetVersion> list(DatasetVersionListRequest request) {
        log.info("Querying dataset version list: {}", request);

        // Extract search parameters from request
        Long datasetId = request.getDatasetId();

        // Calculate offset
        long offset = (request.getPageNumber() - 1L) * request.getPageSize();

        // Query data
        List<DatasetVersionDO> versionList = datasetVersionMapper.selectList(
                datasetId, offset, request.getPageSize()
        );

        // Convert DO to DTO
        List<DatasetVersion> datasetVersions = versionList.stream()
                .map(DatasetVersion::fromDO)
                .collect(Collectors.toList());

        // Get total count
        int totalCount = datasetVersionMapper.selectCount(datasetId);

        // Calculate total pages
        long totalPages = (totalCount + request.getPageSize() - 1) / request.getPageSize();

        // Build result
        PageResult<DatasetVersion> result = new PageResult<>();
        result.setTotalCount((long) totalCount);
        result.setTotalPage(totalPages);
        result.setPageNumber((long) request.getPageNumber());
        result.setPageSize((long) request.getPageSize());
        result.setPageItems(datasetVersions);

        log.info("Dataset version list query completed, total: {}, current page: {}", totalCount, request.getPageNumber());
        return result;
    }

    @Override
    @Transactional
    public DatasetVersion update(DatasetVersionUpdateRequest request) {
        log.info("Updating dataset version: {}", request);

        // Validate request
        if (request.getDatasetVersionId() == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }


        // Note: Version number updates are not supported in this implementation
        // to maintain data integrity and prevent conflicts

        // Update fields - only update available fields
        int result = datasetVersionMapper.update(
                request.getDatasetVersionId(),
                request.getDescription(),
                request.getStatus()
        );

        if (result <= 0) {
            throw new RuntimeException("Failed to update dataset version");
        }

        // Get updated version
        DatasetVersionDO updatedVersionDO = datasetVersionMapper.selectById(request.getDatasetVersionId());
        DatasetVersion updatedVersion = DatasetVersion.fromDO(updatedVersionDO);
        log.info("Dataset version updated successfully: {}", updatedVersion);
        return updatedVersion;
    }


    /**
     * Get dataset version by ID
     */
    public DatasetVersion getById(Long id) {
        log.info("Getting dataset version by ID: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }

        DatasetVersionDO versionDO = datasetVersionMapper.selectById(id);
        if (versionDO == null) {
            log.warn("Dataset version not found: {}", id);
            return null;
        }

        return DatasetVersion.fromDO(versionDO);
    }


    /**
     * Delete dataset version by ID
     */
    @Transactional
    public void deleteById(Long id) {
        log.info("Deleting dataset version: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Version ID cannot be null");
        }

        DatasetVersionDO existingVersion = datasetVersionMapper.selectById(id);
        if (existingVersion == null) {
            throw new IllegalArgumentException("Dataset version not found: " + id);
        }

        if ("PUBLISHED".equals(existingVersion.getStatus())) {
            throw new IllegalStateException("Cannot delete published version: " + id);
        }

        int result = datasetVersionMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("Failed to delete dataset version");
        }

        log.info("Dataset version deleted successfully: {}", id);
    }

    @Override
    public PageResult<Experiment> getExperiments(DatasetExperimentsListRequest request) {
        long offset = (request.getPageNumber() - 1L) * request.getPageSize();

        List<ExperimentDO> experimentDOList = experimentMapper.selectByDatasetId(request.getDatasetId(), offset, request.getPageSize());

        Integer totalCount = experimentMapper.selectCountByDatasetId(request.getDatasetId());

        return new PageResult<>(
                (long) totalCount,
                (long) request.getPageNumber(),
                (long) request.getPageSize(),
                experimentDOList.stream()
                        .map(Experiment::fromDO)
                        .toList());

        }

    /**
     * Delete all versions for a dataset
     */
    @Transactional
    public void deleteByDatasetId(Long datasetId) {
        log.info("Deleting all versions for dataset: {}", datasetId);
        
        if (datasetId == null) {
            throw new IllegalArgumentException("Dataset ID cannot be null");
        }

        int result = datasetVersionMapper.deleteByDatasetId(datasetId);
        log.info("Deleted {} versions for dataset: {}", result, datasetId);
    }
}
