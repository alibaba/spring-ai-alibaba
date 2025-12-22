package com.alibaba.cloud.ai.studio.admin.service.impl;

import com.alibaba.cloud.ai.studio.admin.common.PageResult;
import com.alibaba.cloud.ai.studio.admin.dto.*;
import com.alibaba.cloud.ai.studio.admin.dto.request.*;
import com.alibaba.cloud.ai.studio.admin.entity.*;
import com.alibaba.cloud.ai.studio.admin.enums.ExperimentStatus;
import com.alibaba.cloud.ai.studio.admin.exception.StudioException;
import com.alibaba.cloud.ai.studio.admin.mapper.*;
import com.alibaba.cloud.ai.studio.admin.service.*;
import com.alibaba.cloud.ai.studio.admin.utils.CommonUtils;
import com.alibaba.cloud.ai.studio.admin.utils.ModelConfigParser;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.studio.admin.utils.SessionUtils.convertChatMessages;


@Slf4j
@Service
@RequiredArgsConstructor
public class ExperimentServiceImpl implements ExperimentService {

    private final ExperimentMapper experimentMapper;
    private final ExperimentResultMapper experimentResultMapper;
    private final DatasetVersionMapper datasetVersionMapper;
    private final EvaluatorMapper evaluatorMapper;
    private final EvaluatorVersionMapper evaluatorVersionMapper;
    private final DatasetItemMapper datasetItemMapper;
    private final ModelConfigParser modelConfigParser;


    @Autowired
    private PromptVersionService promptVersionService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private EvaluatorServiceImpl evaluatorServiceImpl;

    // 创建线程池用于异步执行实验
    private final ExecutorService experimentExecutor = Executors.newFixedThreadPool(5);


    @Override
    @Transactional
    public Experiment create(ExperimentCreateRequest request) {
        log.info("创建实验: {}", request);


        // 构建实验实体
        ExperimentDO experimentDO = ExperimentDO.builder()
                .name(request.getName())
                .description(request.getDescription())
                .datasetId(request.getDatasetId())
                .datasetVersionId(request.getDatasetVersionId())
                .datasetVersion(request.getDatasetVersion())
                .evaluationObjectConfig(request.getEvaluationObjectConfig())
                .evaluatorConfig(request.getEvaluatorConfig())
                .status(String.valueOf(ExperimentStatus.RUNNING))
                .progress(0)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 插入数据库
        int result = experimentMapper.insert(experimentDO);
        if (result <= 0) {
            throw new RuntimeException("Failed to create experiment");
        }

        log.info("实验创建成功: {}", experimentDO.getId());
        
        // 异步启动实验执行
        startExperimentExecution(experimentDO);
        
        return Experiment.fromDO(experimentDO);
    }

    @Override
    public PageResult<Experiment> list(ExperimentListRequest request) {
        log.info("查询实验列表: {}", request);


        ExperimentStatus status = null;
        if (StringUtils.hasText(request.getStatus())) {
            try {
                status = ExperimentStatus.fromCode(request.getStatus());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid experiment status: {}", request.getStatus());
            }
        }
        
        // 计算偏移量
        long offset = (request.getPageNumber() - 1L) * request.getPageSize();
        
        // 查询数据
        List<ExperimentDO> experimentDOList = experimentMapper.selectList(request.getName(), status, offset, request.getPageSize());

        // 获取总数
        int totalCount = experimentMapper.count(request.getName(), status);

        return new PageResult<>(
                (long) totalCount,
                (long) request.getPageNumber(),
                (long) request.getPageSize(),
                experimentDOList.stream()
                        .map(Experiment::fromDO)
                        .toList());

    }

    @Override
    public Experiment getById(Long id) {
        log.info("查询实验详情: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Experiment ID cannot be null");
        }
        
        ExperimentDO experimentDO = experimentMapper.selectById(id);
        if (experimentDO == null) {
            log.warn("未找到ID为{}的实验", id);
            return null;
        }

        Experiment experiment = Experiment.fromDO(experimentDO);

        List<EvaluatorConfig> evaluatorConfigList = JSON.parseArray(experiment.getEvaluatorConfig(), EvaluatorConfig.class);

        evaluatorConfigList.stream()
                .filter(Objects::nonNull)
                .forEach(evaluatorConfig -> {
                    try {
                        EvaluatorDO evaluatorDO = evaluatorMapper.selectById(evaluatorConfig.getEvaluatorId());
                        evaluatorConfig.setEvaluatorName(evaluatorDO != null ? evaluatorDO.getName() : "Unknown Evaluator");
                    } catch (Exception e) {
                        log.warn("Failed to fetch evaluator name for id: {}", evaluatorConfig.getEvaluatorId(), e);
                        evaluatorConfig.setEvaluatorName("Error Fetching Name");
                    }
                });
        experiment.setEvaluatorConfig(JSON.toJSONString(evaluatorConfigList));
        return experiment;
    }

    @Override
    public List<ExperimentEvaluatorResult> getResults(Long experimentId) {
        log.info("查询实验结果: {}", experimentId);
        
        // 先检查实验是否存在
        ExperimentDO experiment = experimentMapper.selectById(experimentId);
        if (experiment == null) {
            log.warn("实验不存在: {}", experimentId);
            return null;
        }

        Integer dataCount = datasetVersionMapper.selectById(experiment.getDatasetVersionId()).getDataCount();
        // 检查dataCount是否为null或0，避免除零异常
        if (dataCount == null || dataCount == 0) {
            log.warn("数据集版本数据量为0或不存在: {}", experiment.getDatasetVersionId());
            dataCount = 1; // 避免除零异常，设置默认值
        }


        // 正确解析 evaluatorConfig JSON 数组字符串为 List<EvaluatorConfig>
        List<EvaluatorConfig> evaluatorConfigList = JSON.parseArray(experiment.getEvaluatorConfig(), EvaluatorConfig.class);

        // 提取 evaluatorVersionId 列表
        List<Long> evaluatorList = evaluatorConfigList.stream()
                .map(e -> Long.valueOf(e.getEvaluatorVersionId()))
                .toList();

        // 使用stream map collect方式构建结果列表
        Integer finalDataCount = dataCount;
        return evaluatorList.stream().map(evaluatorVersionId -> {
            List<ExperimentResultDO> resultList = experimentResultMapper.selectByExperimentAndEvaluator(experimentId, evaluatorVersionId);
            //计算score的平均值，避免除零异常
            BigDecimal averageScore = BigDecimal.ZERO;
            if (resultList != null && !resultList.isEmpty()) {
                averageScore = resultList.stream()
                        .map(ExperimentResultDO::getScore)
                        .filter(score -> score != null) // 过滤掉空值
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(new BigDecimal(resultList.size()), 2, BigDecimal.ROUND_HALF_UP);
            }
            int completeItemsCount = (resultList != null) ? resultList.size() : 0;
            Integer progress = completeItemsCount * 100 / finalDataCount;
            return ExperimentEvaluatorResult.builder()
                    .experimentId(experimentId)
                    .averageScore(averageScore)
                    .evaluatorVersionId(evaluatorVersionId)
                    .progress(progress)
                    .completeItemsCount(completeItemsCount)
                    .totalItemsCount(finalDataCount)
                    .build();
        }).collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    @Override
    public PageResult<ExperimentEvaluatorResultDetail> getResult(ExperimentEvaluatorResultDetailListRequest request){

        // 先检查实验是否存在
        ExperimentDO experiment = experimentMapper.selectById(request.getExperimentId());
        if (experiment == null) {
            log.warn("实验不存在: {}", request.getExperimentId());
            return null;
        }
        if (request.getEvaluatorVersionId() == null) {
            throw new IllegalArgumentException("Evaluator version ID cannot be null");
        }


        Integer offset = (request.getPageNumber() - 1) * request.getPageSize();

        Integer limit = request.getPageSize();

        if (offset < 0 || limit <= 0) {
            throw new IllegalArgumentException("Invalid page number or page size");
        }


        Integer totalCount = experimentResultMapper.selectCountByExperimentIdAndEvaluator(request.getExperimentId(), request.getEvaluatorVersionId());

        List<ExperimentResultDO> resultList = experimentResultMapper.selectByExperimentAndEvaluatorWithPageble(request.getExperimentId(),request.getEvaluatorVersionId(),offset,request.getPageSize());


        List<ExperimentEvaluatorResultDetail> resultItems = resultList.stream()
                .map(ExperimentEvaluatorResultDetail::fromDO)
                .toList();

        return new PageResult<>(
                (long) totalCount,
                (long) request.getPageNumber(),
                (long) request.getPageSize(),
                resultItems);

    }

    @Override
    @Transactional
    public Experiment stop(Long id) {
        log.info("停止实验: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Experiment ID cannot be null");
        }
        
        // 获取实验信息
        ExperimentDO experimentDO = experimentMapper.selectById(id);
        if (experimentDO == null) {
            throw new IllegalArgumentException("Experiment not found: " + id);
        }



        // 检查实验状态
        if (ExperimentStatus.COMPLETED.getCode().equals(experimentDO.getStatus()) ||
            ExperimentStatus.FAILED.getCode().equals(experimentDO.getStatus()) ||
            ExperimentStatus.STOPPED.getCode().equals(experimentDO.getStatus())) {
            log.warn("实验 {} 状态为 {}，无法停止", id, experimentDO.getStatus());
            return Experiment.fromDO(experimentDO);
        }


        
        // 更新实验状态为已停止
        experimentDO.setStatus(String.valueOf(ExperimentStatus.STOPPED));
        experimentDO.setUpdateTime(LocalDateTime.now());
        
        int result = experimentMapper.updateById(experimentDO);
        if (result <= 0) {
            throw new RuntimeException("Failed to stop experiment");
        }
        
        log.info("实验停止成功: {}", id);
        return Experiment.fromDO(experimentDO);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        log.info("删除实验: {}", id);
        
        if (id == null) {
            throw new IllegalArgumentException("Experiment ID cannot be null");
        }
        
        // 检查实验是否存在
        ExperimentDO experimentDO = experimentMapper.selectById(id);
        if (experimentDO == null) {
            throw new IllegalArgumentException("Experiment not found: " + id);
        }
        
        // 检查实验状态，运行中的实验不能删除
        if (Objects.equals(experimentDO.getStatus(), ExperimentStatus.RUNNING.getCode())) {
            throw new IllegalStateException("Cannot delete running experiment: " + id);
        }
        
        // 删除实验
        int result = experimentMapper.deleteById(id);
        if (result <= 0) {
            throw new RuntimeException("Failed to delete experiment");
        }
//
//        // 删除相关的实验结果
//        experimentResultMapper.deleteByExperimentId(id);
        
        log.info("实验删除成功: {}", id);
    }

    @Override
    public void restartById(Long id) {
        //清理历史数据
        experimentResultMapper.deleteByExperimentId(id);
        //实验执行

        ExperimentDO experimentDO = experimentMapper.selectById(id);

        startExperimentExecution(experimentDO);


    }



    /**
     * 启动实验执行
     */
    private void startExperimentExecution(ExperimentDO experimentDO)  {
        try {
            experimentExecutor.submit(
                    ()->{
                        try {
                            executeExperiment (experimentDO);
                        } catch (Exception e) {
                            log.error("实验执行过程中发生错误: {}", experimentDO.getId(), e);
                            updateExperimentStatus(experimentDO.getId(), ExperimentStatus.FAILED, null);
                        }
                    }
            );

            log.info("实验执行任务已启动: {}", experimentDO.getId());

        } catch (Exception e) {
            log.error("启动实验执行失败: {}", experimentDO.getId(), e);

            // 更新实验状态为失败
            updateExperimentStatus(experimentDO.getId(), ExperimentStatus.FAILED, null);
        }
    }

    /**
     * 执行实验的核心逻辑
     */
    private void executeExperiment(ExperimentDO experimentDO) throws StudioException {
        log.info("开始执行实验: {}", experimentDO.getId());

        //解析实验 目标 配置

        EvaluationObjectConfig evaluationObjectConfig = JSONObject.parseObject(experimentDO.getEvaluationObjectConfig(),EvaluationObjectConfig.class);
        if(evaluationObjectConfig.getType().equals("prompt")){
            promptEvaluation(experimentDO);

        }
    }



    private void promptEvaluation(ExperimentDO experimentDO) throws StudioException {
        EvaluationObjectConfig evaluationObjectConfig = JSONObject.parseObject(experimentDO.getEvaluationObjectConfig(),EvaluationObjectConfig.class);
        EvaluationPromptConfig evaluationPromptConfig = JSONObject.parseObject(evaluationObjectConfig.getConfig(),EvaluationPromptConfig.class);

        Long experimentId = experimentDO.getId();

        // 获取数据集中的所有数据项

        DatasetVersionDO datasetVersion = datasetVersionMapper.selectById(experimentDO.getDatasetVersionId());

        List<Long> itemIds = CommonUtils.parseItemIds(datasetVersion.getDatasetItems());

        List<DatasetItemDO> datasetItems = datasetItemMapper.selectByDatasetIdAndItemIds(
                datasetVersion.getDatasetId(), itemIds);

        if (datasetItems.isEmpty()) {
            log.warn("数据集为空，实验完成: {}", experimentId);
            updateExperimentStatus(experimentId, ExperimentStatus.COMPLETED, 100);
            return;
        }

        int totalItems = datasetItems.size();
        AtomicInteger processedItems = new AtomicInteger(0);

        log.info("实验 {} 开始处理 {} 个数据项", experimentId, totalItems);


        PromptVersionDetail prompt = promptVersionService.getByPromptKeyAndVersion(evaluationPromptConfig.getPromptKey(),evaluationPromptConfig.getVersion());


        for (DatasetItemDO datasetItem : datasetItems) {
            try {
                // 检查实验是否被停止
                if (isExperimentStopped(experimentId)) {
                    log.info("实验 {} 已被停止", experimentId);
                    return;
                }


                JSONObject dataContent = JSONObject.parseObject(datasetItem.getDataContent());


                String actualOutput = getPromptResult(prompt, dataContent, evaluationPromptConfig);


                List<EvaluatorConfig> evaluatorConfigs = JSON.parseArray(experimentDO.getEvaluatorConfig(), EvaluatorConfig.class);

                evaluatorConfigs.forEach(
                        evaluatorConfig -> {
                            EvaluatorDebugResult debugResult= getEvaluatorResult(evaluatorConfig,dataContent,actualOutput);
                            saveExperimentResult(experimentId, datasetItem.getId(), dataContent.getString("input"), actualOutput, dataContent.getString("reference_output"), debugResult.getScore(), debugResult.getReason(), evaluatorConfig.getEvaluatorVersionId());
                        }

                );


                // 更新进度
                int currentProgress = (processedItems.incrementAndGet() * 100) / totalItems;
                updateExperimentProgress(experimentId, currentProgress);

                log.debug("实验 {} 进度: {}/{} ({}%)", experimentId, processedItems.get(), totalItems, currentProgress);

            } catch (Exception e) {
                log.error("处理数据项失败: experimentId={}, itemId={}", experimentId, datasetItem.getId(), e);
                // 继续处理下一个数据项，不中断整个实验
            }
        }

        // 实验完成
        log.info("实验 {} 执行完成，共处理 {} 个数据项", experimentId, totalItems);
        updateExperimentStatus(experimentId, ExperimentStatus.COMPLETED, 100);

    }



    private  String getPromptResult(PromptVersionDetail prompt,JSONObject dataContent,EvaluationPromptConfig evaluationPromptConfig){
        //value 是通过  EvaluationPromptConfigVariableMap 确定的。
        JSONObject variables = JSONObject.parseObject(prompt.getVariables());
        // 从 EvaluationPromptConfigVariableMap 中拿到  prompt variables 和datasetvolumsname的映射关系，从datacontent中拿到对应的值，放入variables 中 对应prompt viriable name的key中

        List<EvaluationPromptConfigVariableMap> variableMapList = evaluationPromptConfig.getVariableMap();

        variableMapList.forEach(
                variableMap -> {
                    variables.put(variableMap.getPromptVariable(), dataContent.getString(variableMap.getDatasetVolumn()));
                }
        );


        String userPrompt = modelConfigParser.replaceVariables(prompt.getTemplate(),variables.toJSONString());

        log.info("getPromptResult,prompt:{}",userPrompt);



        ChatSession session = chatSessionService.createSession(prompt.getPromptKey(), prompt.getVersion(), prompt.getTemplate(),
                variables.toJSONString(), prompt.getModelConfig());

        session.addUserMessage((String) dataContent.get("input"));
        chatSessionService.updateSession(session);
        
        Map<String, String> observationMetadata = new HashMap<>();
        observationMetadata.put("studioSource", "experiment");
        observationMetadata.put("promptKey", prompt.getPromptKey());
        observationMetadata.put("promptVersion", prompt.getVersion());
        observationMetadata.put("promptTemplate", prompt.getTemplate());
        observationMetadata.put("promptVariables", variables.toJSONString());
        // 获取或创建会话绑定的ModelClient
        ChatClient client = chatSessionService.getOrCreateSessionChatClient(session.getSessionId(), observationMetadata);


        String response = client.prompt(userPrompt).messages(convertChatMessages(session.getMessages())).call().content();

        log.info("getPromptResult,response:{}",response);

        return response;
    }


    private EvaluatorDebugResult getEvaluatorResult(EvaluatorConfig evaluatorConfig, JSONObject dataContent,String actualOutput) {

        EvaluatorTestRequest request = new EvaluatorTestRequest();

        EvaluatorVersionDO evaluatorVersionDO = evaluatorVersionMapper.selectById(evaluatorConfig.getEvaluatorVersionId());

        JSONObject variables = JSONObject.parseObject(evaluatorVersionDO.getVariables());

        evaluatorConfig.getVariableMap().forEach(
                variableMapItem -> {
                    if(variableMapItem.getSource().equals("actual_output")){
                        variables.put(variableMapItem.getEvaluatorVariable(),actualOutput);
                    }else{
                        variables.put(variableMapItem.getEvaluatorVariable(),dataContent.getString(variableMapItem.getSource()));
                    }
                }
        );

        request.setModelConfig(evaluatorVersionDO.getModelConfig());
        request.setPrompt(evaluatorVersionDO.getPrompt());
        request.setVariables(variables.toJSONString());

        EvaluatorDebugResult result = evaluatorServiceImpl.evaluatorTest(request);
        return result;
    }



    /**
     * 保存实验结果
     */
    private void saveExperimentResult(Long experimentId, Long datasetItemId,
                                      String input, String actualOutput, String referenceOutput,
                                      String score, String reason, Long evaluatorVersionId) {
        try {
            ExperimentResultDO resultDO = ExperimentResultDO.builder()
                    .experimentId(experimentId)
                    .input(input)
                    .actualOutput(actualOutput)
                    .referenceOutput(referenceOutput)
                    .score(new BigDecimal(score))
                    .reason(reason)
                    .evaluatorVersionId(evaluatorVersionId)
                    .evaluationTime(LocalDateTime.now())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            
            // 使用批量插入方法，将单个结果包装成列表
            List<ExperimentResultDO> results = new ArrayList<>();
            results.add(resultDO);
            experimentResultMapper.batchInsert(results);
            log.debug("保存实验结果成功: experimentId={}, itemId={}", experimentId, datasetItemId);
            
        } catch (Exception e) {
            log.error("保存实验结果失败: experimentId={}, itemId={}", experimentId, datasetItemId, e);
        }
    }

    /**
     * 检查实验是否被停止
     */
    private boolean isExperimentStopped(Long experimentId) {
        try {
            ExperimentDO experimentDO = experimentMapper.selectById(experimentId);
            return experimentDO != null && 
                   ExperimentStatus.STOPPED.getCode().equals(experimentDO.getStatus());
        } catch (Exception e) {
            log.error("检查实验状态失败: {}", experimentId, e);
            return false;
        }
    }

    /**
     * 更新实验进度
     */
    private void updateExperimentProgress(Long experimentId, Integer progress) {
        try {
            ExperimentDO experimentDO = ExperimentDO.builder()
                    .id(experimentId)
                    .progress(progress)
                    .updateTime(LocalDateTime.now())
                    .build();
            experimentMapper.updateById(experimentDO);
        } catch (Exception e) {
            log.error("更新实验进度失败: {}", experimentId, e);
        }
    }

    /**
     * 更新实验状态
     */
    private void updateExperimentStatus(Long experimentId, ExperimentStatus status, Integer progress) {
        try {
            ExperimentDO experimentDO = ExperimentDO.builder()
                    .id(experimentId)
                    .status(status.getCode())
                    .progress(progress)
                    .updateTime(LocalDateTime.now())
                    .build();
            
            if (status == ExperimentStatus.COMPLETED) {
                experimentDO.setCompleteTime(LocalDateTime.now());
            }
            
            experimentMapper.updateById(experimentDO);
            log.info("实验状态更新成功: experimentId={}, status={}", experimentId, status);
            
        } catch (Exception e) {
            log.error("更新实验状态失败: {}", experimentId, e);
        }
    }

    @Override
    public PageResult<Experiment> getExperimentsByEvaluator(EvaluatorExperimentsListRequest request) {
        log.info("查询评估器关联的实验: {}", request);
        
        try {
            // 计算偏移量
            long offset = (request.getPageNumber() - 1L) * request.getPageSize();
            
            List<ExperimentDO> experimentDOList;
            int totalCount;

            experimentDOList = experimentMapper.selectByEvaluatorId(
                    request.getEvaluatorId(), offset, request.getPageSize());

            totalCount = experimentMapper.selectCountByEvaluatorId(request.getEvaluatorId());

            
            // 转换为DTO并获取数据集版本信息
            List<Experiment> experiments = experimentDOList.stream()
                    .map(experimentDO -> {
                        try {
                            return Experiment.fromDO(experimentDO);
                        } catch (Exception e) {
                            log.warn("获取数据集版本信息失败: experimentId={}, datasetVersionId={}", 
                                experimentDO.getId(), experimentDO.getDatasetVersionId(), e);
                            return Experiment.fromDO(experimentDO);
                        }
                    })
                    .toList();
            
            return new PageResult<>(
                    (long) totalCount,
                    (long) request.getPageNumber(),
                    (long) request.getPageSize(),
                    experiments
            );
            
        } catch (Exception e) {
            log.error("查询评估器关联的实验失败: {}", request, e);
            throw new RuntimeException("查询评估器关联的实验失败: " + e.getMessage());
        }
    }

} 