package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.admin.dto.ChatSession;
import com.alibaba.cloud.ai.studio.admin.dto.Prompt;
import com.alibaba.cloud.ai.studio.admin.dto.PromptRunResponse;
import com.alibaba.cloud.ai.studio.admin.dto.PromptTemplate;
import com.alibaba.cloud.ai.studio.admin.dto.PromptTemplateDetail;
import com.alibaba.cloud.ai.studio.admin.dto.PromptVersion;
import com.alibaba.cloud.ai.studio.admin.dto.PromptVersionDetail;
import com.alibaba.cloud.ai.studio.admin.dto.request.*;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.service.PromptRunService;
import com.alibaba.cloud.ai.studio.admin.service.PromptService;
import com.alibaba.cloud.ai.studio.admin.service.PromptTemplateService;
import com.alibaba.cloud.ai.studio.admin.service.PromptVersionService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PromptController {

    private final PromptService promptService;
    private final PromptVersionService promptVersionService;
    private final PromptTemplateService promptTemplateService;
    private final PromptRunService promptRunService;

    // ==================== Prompt基础管理接口 ====================

    /**
     * 创建Prompt
     */
    @PostMapping("/prompt")
    public Result<Prompt> createPrompt(@Validated @RequestBody PromptCreateRequest request) throws StudioException {
        log.info("创建Prompt请求: {}", request);
        Prompt prompt = promptService.create(request);
        return Result.success(prompt);
    }

    /**
     * 获取Prompt详情
     */
    @GetMapping("/prompt")
    public Result<Prompt> getPrompt(@RequestParam @NotBlank String promptKey) throws StudioException {
        log.info("查询Prompt详情请求: {}", promptKey);
        Prompt prompt = promptService.getByPromptKey(promptKey);
        return Result.success(prompt);
    }

    /**
     * 获取Prompt列表
     */
    @GetMapping("/prompts")
    public Result<PageResult<Prompt>> listPrompts(@Validated @ModelAttribute PromptListRequest request) throws StudioException {
        log.info("查询Prompt列表请求: {}", request);
        PageResult<Prompt> result = promptService.list(request);
        return Result.success(result);
    }

    /**
     * 更新Prompt
     */
    @PutMapping("/prompt")
    public Result<Prompt> updatePrompt(@Validated @RequestBody PromptUpdateRequest request) throws StudioException {
        log.info("更新Prompt请求: {}", request);
        Prompt prompt = promptService.update(request);
        return Result.success(prompt);
    }

    /**
     * 删除Prompt
     */
    @DeleteMapping("/prompt")

    public Result<Boolean> deletePrompt(@RequestParam @NotBlank String promptKey) throws StudioException {
        log.info("删除Prompt请求: {}", promptKey);
        promptService.deleteByPromptKey(promptKey);
        return Result.success(true);
    }

    // ==================== Prompt版本管理接口 ====================

    /**
     * 创建Prompt版本
     */
    @PostMapping("/prompt/version")
    public Result<PromptVersion> createPromptVersion(@Validated @RequestBody PromptVersionCreateRequest request)
            throws StudioException {
        log.info("创建Prompt版本请求: {}", request);
        PromptVersion promptVersion = promptVersionService.create(request);
        return Result.success(promptVersion);
    }

    /**
     * 获取Prompt版本详情
     */
    @GetMapping("/prompt/version")
    public Result<PromptVersionDetail> getPromptVersion(@RequestParam @NotBlank String promptKey,
                                                        @RequestParam @NotBlank String version) throws StudioException {
        log.info("查询Prompt版本详情请求: promptKey={}, version={}", promptKey, version);
        PromptVersionDetail promptVersionDetail = promptVersionService.getByPromptKeyAndVersion(promptKey, version);
        return Result.success(promptVersionDetail);
    }

    /**
     * 获取Prompt版本列表
     */
    @GetMapping("/prompt/versions")
    public Result<PageResult<PromptVersion>> listPromptVersions(@Validated @ModelAttribute PromptVersionListRequest request) {
        log.info("查询Prompt版本列表请求: {}", request);
        PageResult<PromptVersion> result = promptVersionService.list(request);
        return Result.success(result);
    }

    // ==================== Prompt模板管理接口 ====================

    /**
     * 获取Prompt模板详情
     */
    @GetMapping("/prompt/template")
    public Result<PromptTemplateDetail> getPromptTemplate(@RequestParam @NotBlank String promptTemplateKey)
            throws StudioException {
        log.info("查询Prompt模板详情请求: {}", promptTemplateKey);
        PromptTemplateDetail promptTemplateDetail = promptTemplateService.getByPromptTemplateKey(promptTemplateKey);
        return Result.success(promptTemplateDetail);
    }

    /**
     * 获取Prompt模板列表
     */
    @GetMapping("/prompt/templates")
    public Result<PageResult<PromptTemplate>> listPromptTemplates(@Validated PromptTemplateListRequest request)
            throws StudioException {
        log.info("查询Prompt模板列表请求: {}", request);
        PageResult<PromptTemplate> result = promptTemplateService.list(request);
        return Result.success(result);
    }

    // ==================== Prompt调试接口 ====================

    /**
     * 运行Prompt调试（支持持续交互）
     */
    @PostMapping(value = "/prompt/run", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<PromptRunResponse> runPrompt(@Validated @RequestBody PromptRunRequest request) {
        log.info("运行Prompt调试请求: {}", request);
        try {
            return promptRunService.run(request);
        } catch (Exception e) {
            log.error("运行Prompt调试失败", e);
            return Flux.just(PromptRunResponse.createErrorResponse(null, e.getMessage()));
        }
    }

    /**
     * 获取会话信息
     */
    @GetMapping("/prompt/session")
    public Result<ChatSession> getSession(@RequestParam @NotBlank String sessionId) {
        log.info("获取会话信息: {}", sessionId);
        ChatSession session = promptRunService.getSession(sessionId);
        if (session == null) {
            return Result.error("会话不存在或已过期");
        }
        return Result.success(session);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/prompt/session")
    public Result<Void> deleteSession(@RequestParam @NotBlank String sessionId) {
        log.info("删除会话: {}", sessionId);
        promptRunService.deleteSession(sessionId);
        return Result.success(null);
    }
}
