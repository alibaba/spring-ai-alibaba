package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.admin.dto.ModelConfigResponse;
import com.alibaba.cloud.ai.studio.admin.dto.request.ModelConfigQueryRequest;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.service.ModelConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ModelConfigController {
    
    private final ModelConfigService modelConfigService;
    
    @GetMapping("/model/supported")
    public Result<List<String>> getSupportedProviders() {
        log.info("获取支持的模型提供商请求");
        List<String> providers = modelConfigService.getSupportedProviders();
        return Result.success(providers);
    }
    
    @GetMapping("/models")
    public Result<PageResult<ModelConfigResponse>> list(@Valid ModelConfigQueryRequest request) {
        log.info("查询模型配置列表请求: {}", request);
        PageResult<ModelConfigResponse> result = modelConfigService.list(request);
        return Result.success(result);
    }
    
    @GetMapping("/model")
    public Result<ModelConfigResponse> getById(@RequestParam Long id) throws StudioException {
        log.info("获取模型配置详情请求，ID: {}", id);
        ModelConfigResponse response = modelConfigService.getById(id);
        return Result.success(response);
    }
    
    @GetMapping("/models/enabled")
    public Result<List<ModelConfigResponse>> getEnabledConfigs() {
        log.info("获取启用的模型配置列表请求");
        List<ModelConfigResponse> configs = modelConfigService.getEnabledConfigs();
        return Result.success(configs);
    }
}
