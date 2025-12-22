package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.*;
import com.alibaba.cloud.ai.studio.admin.dto.request.*;
import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorDO;
import com.alibaba.cloud.ai.studio.admin.entity.EvaluatorVersionDO;
import com.alibaba.cloud.ai.studio.admin.mapper.EvaluatorMapper;
import com.alibaba.cloud.ai.studio.admin.mapper.EvaluatorVersionMapper;
import com.alibaba.cloud.ai.studio.admin.service.ChatSessionService;
import com.alibaba.cloud.ai.studio.admin.service.EvaluatorService;
import com.alibaba.cloud.ai.studio.admin.utils.ModelConfigParser;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;

import static com.alibaba.cloud.ai.studio.admin.utils.CommonUtils.extractRawText;


@Slf4j
@Service
@RequiredArgsConstructor
public class EvaluatorServiceImpl implements EvaluatorService {

    private final EvaluatorMapper evaluatorMapper;

    private final EvaluatorVersionMapper evaluatorVersionMapper;

    private final ChatSessionService chatSessionService;

    private final ModelConfigParser modelConfigParser;

    private final String SYSTEM_PROMPT = """
            按照Json格式返回评估结果。例如
            {"score":"0.85","reason":"回答基本正确，准确回答了用户关于人工智能的问题。"}
            只返回Json字符串，不要有其他任何内容。
            """;

    @Override
    public Evaluator create(EvaluatorCreateRequest request) {
        log.info("创建评估器: {}", request);

        // 构建DO对象
        EvaluatorDO evaluatorDO = EvaluatorDO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 插入数据库
        int result = evaluatorMapper.insert(evaluatorDO);
        if (result > 0) {
            log.info("评估器创建成功: {}", evaluatorDO.getId());
            return Evaluator.fromDO(evaluatorDO);
        } else {
            throw new RuntimeException("创建评估器失败");
        }
    }

    @Override
    public PageResult<Evaluator> list(EvaluatorListRequest request) {
        log.info("查询评估器列表: {}", request);

        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
        long offset = (pageNumber - 1L) * pageSize;

        List<EvaluatorDO> evaluatorDOList = evaluatorMapper.selectList(request.getName(), offset, pageSize);

        List<Evaluator> evaluatorList = evaluatorDOList.stream()
                .map(Evaluator::fromDO)
                .map(evaluator -> {
                    EvaluatorVersionDO evaluatorVersionDO = evaluatorVersionMapper.selectLatestVersionByEvaluatorId(evaluator.getId());
                    if (evaluatorVersionDO != null) {
                        evaluator.setModelConfig(evaluatorVersionDO.getModelConfig());
                        evaluator.setLatestVersion(evaluatorVersionDO.getVersion());
                    }
                    return evaluator;
                })
                .toList();

        int total = evaluatorMapper.count(request.getName());

        return new PageResult<>(
                (long) pageNumber,
                (long) total,
                (long) pageSize,
                evaluatorList
        );
    }

    @Override
    public Evaluator getById(Long id) {
        log.info("查询评估器详情: {}", id);

        EvaluatorDO evaluatorDO = evaluatorMapper.selectById(id);
        if (evaluatorDO == null) {
            return null;
        }

        EvaluatorVersionDO evaluatorVersionDO = evaluatorVersionMapper.selectLatestVersionByEvaluatorId(id);
        Evaluator evaluator = Evaluator.fromDO(evaluatorDO);
        if (Objects.nonNull(evaluatorVersionDO)) {
            evaluator.setModelConfig(evaluatorVersionDO.getModelConfig());
            evaluator.setLatestVersion(evaluatorVersionDO.getVersion());
            evaluator.setPrompt(evaluatorVersionDO.getPrompt());
            evaluator.setVariables(evaluatorVersionDO.getVariables());
        }

        return evaluator;
    }

    @Override
    public Evaluator update(EvaluatorUpdateRequest request) {
        log.info("更新评估器: {}", request);

        // 构建DO对象
        EvaluatorDO evaluatorDO = EvaluatorDO.builder()
                .id(request.getId())
                .name(request.getName())
                .description(request.getDescription())
                .build();

        // 更新数据库
        int result = evaluatorMapper.update(evaluatorDO);
        if (result > 0) {
            log.info("评估器更新成功: {}", request.getId());
            // 重新查询获取最新数据
            return Evaluator.fromDO(evaluatorMapper.selectById(request.getId()));
        } else {
            throw new RuntimeException("更新评估器失败");
        }
    }

    @Override
    public void deleteById(Long id) {
        log.info("删除评估器: {}", id);

        int result = evaluatorMapper.deleteById(id);
        if (result > 0) {
            log.info("评估器删除成功: {}", id);
        } else {
            throw new RuntimeException("删除评估器失败");
        }
    }

    @Override
    public EvaluatorDebugResult debug(EvaluatorTestRequest request) {
        log.info("调试评估器: {}", request);

        EvaluatorDebugResult result = evaluatorTest(request);

        return result;
    }


    /**
     * 调试模型调用
     */
    public EvaluatorDebugResult evaluatorTest(EvaluatorTestRequest request) {
        ChatSession session = chatSessionService.createEvaluatorSession(request.getPrompt(), request.getVariables(), request.getModelConfig());
        Map<String, String> observationMetadata = new HashMap<>();
        observationMetadata.put("studioSource", "evaluator");
        ChatClient client = chatSessionService.getOrCreateSessionChatClient(session.getSessionId(), observationMetadata);

        String userPrompt = modelConfigParser.replaceVariables(request.getPrompt(), request.getVariables());

        String prompt = userPrompt.concat(SYSTEM_PROMPT);

        log.info("evaluatorTest:prompt,{}", prompt);

        String response = Objects.requireNonNull(client.prompt(prompt).call().content()).trim();

        log.info("模型返回值:{}", response);

        String formatedResponse = extractRawText(response);
        log.info("模型返回值:{},格式化后模型返回值:{}.", response, formatedResponse);

        try {
            return JSONObject.parseObject(formatedResponse, EvaluatorDebugResult.class);
        } catch (Exception e) {
            log.info("解析失败: {}", formatedResponse, e);
            throw new RuntimeException("解析模型调用结果出错，请重试");
        }

    }

} 