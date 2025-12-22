package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorDebugResult;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorTemplate;
import com.alibaba.cloud.ai.studio.admin.dto.EvaluatorVersion;
import com.alibaba.cloud.ai.studio.admin.dto.Experiment;
import com.alibaba.cloud.ai.studio.admin.dto.request.*;
import com.alibaba.cloud.ai.studio.admin.dto.Evaluator;
import com.alibaba.cloud.ai.studio.admin.service.EvaluatorTemplateService;
import com.alibaba.cloud.ai.studio.admin.service.EvaluatorService;
import com.alibaba.cloud.ai.studio.admin.service.EvaluatorVersionService;
import com.alibaba.cloud.ai.studio.admin.service.ExperimentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/evaluator")
@RequiredArgsConstructor
public class EvaluatorController {

    private final EvaluatorService evaluatorService;

    private final EvaluatorVersionService evaluatorVersionService;

    private final EvaluatorTemplateService evaluatorPromptTemplateService;

    private final ExperimentService experimentService;

    /**
     * 创建评估器
     */
    @PostMapping("/evaluator")
    public Result<Evaluator> create(@Validated @RequestBody EvaluatorCreateRequest request) {
        log.info("创建评估器请求: {}", request);
        try {
            Evaluator evaluator = evaluatorService.create(request);
            return Result.success(evaluator);
        } catch (Exception e) {
            log.error("创建评估器失败", e);
            return Result.error("创建评估器失败: " + e.getMessage());
        }
    }

    /**
     * 创建评估器版本
     */
    @PostMapping("/evaluatorVersion")
    public Result<EvaluatorVersion> createVersion(@RequestBody EvaluatorVersionCreateRequest request) {
        log.info("创建评估器版本请求: {}", request);
        try {
            EvaluatorVersion evaluatorVersion = evaluatorVersionService.create(request);
            return Result.success(evaluatorVersion);
        } catch (Exception e) {
            log.error("创建评估器版本失败", e);
            return Result.error("创建评估器版本失败: " + e.getMessage());
        }
    }

    /**
     * 获取评估器列表
     */
    @GetMapping("/evaluators")
    public Result<PageResult<Evaluator>> list(EvaluatorListRequest evaluatorListRequest){
        log.info("查询评估器列表请求: {}", evaluatorListRequest);
        try {
            PageResult<Evaluator> result = evaluatorService.list(evaluatorListRequest);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询评估器列表失败", e);
            return Result.error("查询评估器列表失败: " + e.getMessage());
        }
    }

    /**
     * 获取评估器详情
     */
    @GetMapping("/evaluator")
    public Result<Evaluator> get(Long id) {
        log.info("查询评估器详情请求: {}", id);
        try {
            Evaluator evaluator = evaluatorService.getById(id);
            if (evaluator == null) {
                return Result.error(404, "评估器不存在");
            }
            return Result.success(evaluator);
        } catch (Exception e) {
            log.error("查询评估器详情失败", e);
            return Result.error("查询评估器详情失败: " + e.getMessage());
        }
    }

    //获取评估器版本列表
    @GetMapping("/evaluatorVersions")
    public Result<PageResult<EvaluatorVersion>> listVersions(EvaluatorVersionListRequest request) {
        log.info("查询评估器版本列表请求: {}", request);
        try {
            PageResult<EvaluatorVersion> result = evaluatorVersionService.list(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询评估器版本列表失败", e);
            return Result.error("查询评估器版本列表失败: " + e.getMessage());
        }
    }

    /**
     * 更新评估器
     */
    @PutMapping("/evaluator")
    public Result<Evaluator> update(@RequestBody EvaluatorUpdateRequest request) {
        log.info("更新评估器请求: {}", request);
        try {
            Evaluator updatedEvaluator = evaluatorService.update(request);
            return Result.success(updatedEvaluator);
        } catch (Exception e) {
            log.error("更新评估器失败", e);
            return Result.error("更新评估器失败: " + e.getMessage());
        }
    }

    /**
     * 删除评估器
     */
    @DeleteMapping("/evaluator")
    public Result<Void> delete(@RequestParam Long id) {
        log.info("删除评估器请求: {}", id);
        try {
            evaluatorService.deleteById(id);
            return Result.success();
        } catch (Exception e) {
            log.error("删除评估器失败", e);
            return Result.error("删除评估器失败: " + e.getMessage());
        }
    }

    /**
     * 调试评估器
     */
    @PostMapping("/debug")
    public Result<EvaluatorDebugResult> debug(@RequestBody EvaluatorTestRequest request) {
        log.info("调试评估器请求: {}", request);
        try {
            EvaluatorDebugResult result = evaluatorService.debug(request);
            return Result.success(result);
        } catch (Exception e) {
            log.error("调试评估器失败", e);
            return Result.error("调试评估器失败: " + e.getMessage());
        }
    }

    /**
     * 获取评估模板列表
     */
    @GetMapping("/templates")
    public Result<PageResult<EvaluatorTemplate>> getTemplates(EvaluatorTemplateListRequest request) {
        log.info("获取评估模板列表请求");
        try {
            PageResult<EvaluatorTemplate> templates = evaluatorPromptTemplateService.list(request);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取评估模板列表失败", e);
            return Result.error("获取评估模板列表失败: " + e.getMessage());
        }
    }


    /**
     * 获取评估模板列表
     */
    @GetMapping("/template")
    public Result<EvaluatorTemplate> getTemplate(Long templateId) {
        log.info("获取评估模板列表请求");
        try {
            EvaluatorTemplate templates = evaluatorPromptTemplateService.get(templateId);
            return Result.success(templates);
        } catch (Exception e) {
            log.error("获取评估模板详细信息", e);
            return Result.error("获取评估模板详细信息失败: " + e.getMessage());
        }
    }

    /**
     * 获取评估器关联的实验
     */
    @GetMapping("/experiments")
    public Result<PageResult<Experiment>> getExperiments(EvaluatorExperimentsListRequest request) {
        log.info("获取评估器关联的实验: {}", request);
        try {
            PageResult<Experiment> experiments = experimentService.getExperimentsByEvaluator(request);
            return Result.success(experiments);
        } catch (Exception e) {
            log.error("获取评估器关联的实验失败", e);
            return Result.error("获取评估器关联的实验失败: " + e.getMessage());
        }
    }

} 