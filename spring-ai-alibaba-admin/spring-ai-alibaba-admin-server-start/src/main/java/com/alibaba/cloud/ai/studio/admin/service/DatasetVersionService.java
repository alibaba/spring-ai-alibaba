package com.alibaba.cloud.ai.studio.admin.service;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.DatasetVersion;
import com.alibaba.cloud.ai.studio.admin.dto.Experiment;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetExperimentsListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetVersionCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetVersionListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.DatasetVersionUpdateRequest;
import com.alibaba.cloud.ai.studio.admin.entity.DatasetVersionDO;
import org.springframework.stereotype.Service;


@Service

public interface DatasetVersionService {

    DatasetVersion create(DatasetVersionCreateRequest request);

    PageResult<DatasetVersion> list(DatasetVersionListRequest request);

    DatasetVersion update(DatasetVersionUpdateRequest request);
    
    DatasetVersion getById(Long id);
    
    void deleteById(Long id);

    PageResult<Experiment> getExperiments(DatasetExperimentsListRequest datasetExperimentsListRequest);
}