package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.admin.dto.ExperimentEvaluatorResultDetail;
import com.alibaba.cloud.ai.studio.admin.dto.request.ExperimentCreateRequest;
import com.alibaba.cloud.ai.studio.admin.dto.Experiment;
import com.alibaba.cloud.ai.studio.admin.dto.ExperimentEvaluatorResult;
import com.alibaba.cloud.ai.studio.admin.dto.request.ExperimentEvaluatorResultDetailListRequest;
import com.alibaba.cloud.ai.studio.admin.dto.request.ExperimentListRequest;
import com.alibaba.cloud.ai.studio.admin.service.ExperimentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ExperimentController {

    private final ExperimentService experimentService;

    /**
     * 创建实验
     */
    @PostMapping("/experiment")
    public Result<Experiment> create(@RequestBody ExperimentCreateRequest request) {
        log.info("创建实验请求: {}", request);
        try {
            Experiment experiment = experimentService.create(request);
            return Result.success(experiment);
        } catch (Exception e) {
            log.error("创建实验失败", e);
            return Result.error("创建实验失败: " + e.getMessage());
        }
    }

    /**
     * 获取实验列表
     */
    @GetMapping("/experiments")
    public Result<PageResult<Experiment>> list(ExperimentListRequest request) {
        log.info("查询实验列表请求: {}", request);
        try {

            PageResult<Experiment> result = experimentService.list(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询实验列表失败", e);
            return Result.error("查询实验列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取实验详情
     */
    @GetMapping("/experiment")
    public Result<Experiment> get(@RequestParam(value = "experimentId") Long experimentId) {
        log.info("查询实验详情请求: {}", experimentId);
        try {
            Experiment experiment = experimentService.getById(experimentId);
            if (experiment == null) {
                return Result.error(404, "实验不存在");
            }
            return Result.success(experiment);
        } catch (Exception e) {
            log.error("查询实验详情失败", e);
            return Result.error("查询实验详情失败: " + e.getMessage());
        }
    }

    /**
     * 获取实验概览结果
     */
    @GetMapping("/experiment/results")
    public Result<List<ExperimentEvaluatorResult>> getResults(
            @RequestParam(value = "experimentId") Long experimentId) {
        log.info("查询实验结果请求: experimentId={}", experimentId);
        try {

            List<ExperimentEvaluatorResult> result = experimentService.getResults(experimentId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询实验结果失败", e);
            return Result.error("查询实验结果失败: " + e.getMessage());
        }
    }


    /**
     * 获取实验详细结果
     */
    @GetMapping("/experiment/result")
    public Result<PageResult<ExperimentEvaluatorResultDetail>> getResult(@Validated ExperimentEvaluatorResultDetailListRequest request) {
        log.info("查询实验结果请求详情: experimentId={}, evaluatorVersionId={}", request.getExperimentId(), request.getEvaluatorVersionId());
        try {

            PageResult<ExperimentEvaluatorResultDetail> result = experimentService.getResult(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询实验结果失败", e);
            return Result.error("查询实验结果失败: " + e.getMessage());
        }
    }


    /**
     * 停止实验
     */
    @PutMapping("/experiment/stop")
    public Result<Experiment> stop(@RequestParam(value = "experimentId") Long experimentId) {
        log.info("停止实验请求: {}", experimentId);
        try {
            Experiment experiment = experimentService.stop(experimentId);
            return Result.success(experiment);
        } catch (Exception e) {
            log.error("停止实验失败", e);
            return Result.error("停止实验失败: " + e.getMessage());
        }
    }

    /**
     * 删除实验
     */
    @DeleteMapping("/experiment")
    public Result<Void> delete(@RequestParam(value = "experimentId") Long experimentId) {
        log.info("删除实验请求: {}", experimentId);
        try {
            experimentService.deleteById(experimentId);
            return Result.success();
        } catch (Exception e) {
            log.error("删除实验失败", e);
            return Result.error("删除实验失败: " + e.getMessage());
        }
    }

    /**
     * 删除实验
     */
    @PutMapping("/experiment/restart")
    public Result<Void> restart(@RequestParam(value = "experimentId") Long experimentId) {
        log.info("重启实验: {}", experimentId);
        try {
            experimentService.restartById(experimentId);
            return Result.success();
        } catch (Exception e) {
            log.error("重启实验", e);
            return Result.error("重启实验: " + e.getMessage());
        }
    }
}