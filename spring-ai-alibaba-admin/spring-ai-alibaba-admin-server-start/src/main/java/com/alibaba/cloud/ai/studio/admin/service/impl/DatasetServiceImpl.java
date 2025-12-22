package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.Dataset;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetColumn;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.DatasetDO;
import com.alibaba.cloud.ai.studio.admin.entity.DatasetVersionDO;
import com.alibaba.cloud.ai.studio.admin.mapper.DatasetMapper;
import com.alibaba.cloud.ai.studio.admin.mapper.DatasetVersionMapper;
import com.alibaba.cloud.ai.studio.admin.service.DatasetService;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class DatasetServiceImpl implements DatasetService {

    @Resource
    private DatasetMapper datasetMapper;

    @Resource
    private DatasetVersionMapper datasetVersionMapper;




    private static final String INPUT_COLUMN_TYPE = "input";
    private static final String REFERENCE_OUTPUT_COLUMN_TYPE = "reference_output";


    @Override
    @Transactional
    public Dataset create(DatasetCreateRequest request) {
        log.info("创建评测集: {}", request);

        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("评测集名称不能为空");
        }


        if (request == null || request.getColumnsConfig() == null ||
                !hasRequiredColumns(request.getColumnsConfig())) {
            throw new IllegalArgumentException("评测集列配置错误，必须包含input和reference_output两列");
        }

        DatasetDO datasetDO = DatasetDO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .columnsConfig(JSONObject.toJSONString(request.getColumnsConfig()))
                .build();

        datasetMapper.insert(datasetDO);
        log.info("评测集创建成功: {}", datasetDO);
        return Dataset.fromDO(datasetDO);
    }

    @Override
    public PageResult<Dataset> list(DatasetListRequest request) {
        log.info("查询评测集列表: {}", request);

        // 计算分页参数
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        long offset = (pageNumber - 1L) * pageSize;
        
        // 获取搜索条件
        String name = request.getDatasetName();
        
        // 查询数据
        List<DatasetDO> datasetDOList = datasetMapper.selectList(name, offset, pageSize);

        List<Dataset> datasetList = datasetDOList.stream()
                .map(Dataset::fromDO)
                .peek(dataset -> {
                    DatasetVersionDO datasetVersionDO = datasetVersionMapper.selectLatestVersion(dataset.getId());
                    if (Objects.nonNull(datasetVersionDO)) {
                        dataset.setDataCount(datasetVersionDO.getDataCount());
                        dataset.setLatestVersion(datasetVersionDO.getVersion());
                    }
                })
                .toList();
        int total = datasetMapper.selectCount(name);
        

        PageResult<Dataset> result = new PageResult<>(
                (long) total, 
                (long) pageNumber, 
                (long) pageSize,
                datasetList
        );
        
        return result;
    }

    @Override
    public Dataset getById(Long id) {
        log.info("查询评测集详情: {}", id);
        DatasetDO datasetDO = datasetMapper.selectById(id);
            
        if (datasetDO == null) {
            log.warn("未找到ID为{}的评测集", id);
            return null;
        }
        Dataset dataset = Dataset.fromDO(datasetDO);

        DatasetVersionDO datasetVersionDO = datasetVersionMapper.selectLatestVersion(dataset.getId());

        if(Objects.nonNull(datasetVersionDO)){
            dataset.setDataCount(datasetVersionDO.getDataCount());
            dataset.setLatestVersion(datasetVersionDO.getVersion());
            dataset.setLatestVersionId(datasetVersionDO.getId());
        }
        
        return dataset;
    }

    @Override
    public Dataset update(DatasetUpdateRequest request) {
        log.info("更新评测集: {}", request);

         DatasetDO existingDataset = datasetMapper.selectById(request.getDatasetId());
         if (existingDataset == null) {
             throw new IllegalArgumentException("评测集不存在: " + request.getDatasetId());
         }


        datasetMapper.update(request.getDatasetId(),request.getName(),request.getDescription());
        existingDataset.setName(request.getName());
        existingDataset.setDescription(request.getDescription());

        return Dataset.fromDO(existingDataset);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("删除评测集: {}", id);
        datasetMapper.deleteById(id);
        log.info("评测集删除成功: {}", id);
    }


    private boolean hasRequiredColumns(List<DatasetColumn> columns) {
        if (columns == null) {
            return false;
        }

        return columns.stream()
                .filter(Objects::nonNull)
                .anyMatch(column -> INPUT_COLUMN_TYPE.equals(column.getName())) &&
                columns.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(column -> REFERENCE_OUTPUT_COLUMN_TYPE.equals(column.getName()));
    }
}