/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.model.execution.ExecutionStep;
import com.alibaba.cloud.ai.model.execution.Plan;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.util.ChatResponseUtil;
import com.alibaba.cloud.ai.util.MarkdownParser;
import com.alibaba.cloud.ai.util.StateUtils;
import com.alibaba.cloud.ai.util.StreamingChatGeneratorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.ParameterizedTypeReference;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static com.alibaba.cloud.ai.graph.StateGraph.END;

/**
 * Enhanced SQL generation node that handles SQL query regeneration with advanced
 * optimization features.
 *
 * This node is responsible for: - Multi-round SQL optimization and refinement - Syntax
 * validation and security analysis - Performance optimization and intelligent caching -
 * Handling execution exceptions and semantic consistency failures - Managing retry logic
 * with schema advice - Providing streaming feedback during regeneration process
 *
 * @author zhangshenghang
 */
public class SqlGenerateNode implements NodeAction {

	private static final Logger logger = LoggerFactory.getLogger(SqlGenerateNode.class);

	private static final int MAX_RETRY_COUNT = 3;

	private static final int MAX_OPTIMIZATION_ROUNDS = 3;

	private final BaseNl2SqlService baseNl2SqlService;

	private final BeanOutputConverter<Plan> converter;

	private final ChatClient chatClient;

	public SqlGenerateNode(ChatClient.Builder chatClientBuilder, BaseNl2SqlService baseNl2SqlService) {
		this.chatClient = chatClientBuilder.build();
		this.baseNl2SqlService = baseNl2SqlService;
		this.converter = new BeanOutputConverter<>(new ParameterizedTypeReference<Plan>() {
		});
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		logger.info("Entering {} node", this.getClass().getSimpleName());

		// Get necessary input parameters
		String plannerNodeOutput = StateUtils.getStringValue(state, PLANNER_NODE_OUTPUT);
		Plan plan = converter.convert(plannerNodeOutput);
		Integer currentStep = StateUtils.getObjectValue(state, PLAN_CURRENT_STEP, Integer.class, 1);

		List<ExecutionStep> executionPlan = plan.getExecutionPlan();
		ExecutionStep executionStep = executionPlan.get(currentStep - 1);
		ExecutionStep.ToolParameters toolParameters = executionStep.getToolParameters();

		// Execute business logic first - determine what needs to be regenerated
		Map<String, Object> result;
		String displayMessage;

		if (StateUtils.hasValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT)) {
			displayMessage = "检测到SQL执行异常，开始重新生成SQL...";
			String newSql = handleSqlExecutionException(state, plan, toolParameters);
			toolParameters.setSqlQuery(newSql);
			result = Map.of(SQL_GENERATE_OUTPUT, SQL_EXECUTE_NODE, PLANNER_NODE_OUTPUT, plan.toJsonStr());
			logger.info("[{}] Regenerated SQL due to execution exception: {}", this.getClass().getSimpleName(), newSql);
		}
		else if (isSemanticConsistencyFailed(state)) {
			displayMessage = "语义一致性校验未通过，开始重新生成SQL...";
			String newSql = handleSemanticConsistencyFailure(state, toolParameters);
			result = Map.of(SQL_GENERATE_OUTPUT, newSql, RESULT, newSql);
			logger.info("[{}] Regenerated SQL due to semantic consistency failure: {}", this.getClass().getSimpleName(),
					newSql);
		}
		else {
			throw new IllegalStateException("SQL generation node was called unexpectedly");
		}

		// Create display flux for user experience only
		Flux<ChatResponse> displayFlux = Flux.create(emitter -> {
			emitter.next(ChatResponseUtil.createCustomStatusResponse(displayMessage));
			if (result.containsKey(RESULT)) {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("重新生成的SQL: " + result.get(RESULT)));
			}
			else if (result.containsKey(SQL_GENERATE_OUTPUT)
					&& result.get(SQL_GENERATE_OUTPUT).equals(SQL_EXECUTE_NODE)) {
				emitter.next(ChatResponseUtil.createCustomStatusResponse("SQL重新生成完成，准备执行"));
			}
			emitter.complete();
		});

		var generator = StreamingChatGeneratorUtil.createStreamingGeneratorWithMessages(this.getClass(), state,
				v -> result, displayFlux);

		return Map.of(SQL_GENERATE_OUTPUT, generator);
	}

	/**
	 * Handle SQL execution exception
	 */
	private String handleSqlExecutionException(OverAllState state, Plan plan,
			ExecutionStep.ToolParameters toolParameters) throws Exception {
		String sqlException = StateUtils.getStringValue(state, SQL_EXECUTE_NODE_EXCEPTION_OUTPUT);
		logger.info("Detected SQL execution exception, starting to regenerate SQL: {}", sqlException);

		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		return regenerateSql(state, toolParameters.toJsonStr(), evidenceList, schemaDTO,
				SQL_EXECUTE_NODE_EXCEPTION_OUTPUT, toolParameters.getSqlQuery());
	}

	/**
	 * Handle semantic consistency validation failure
	 */
	private String handleSemanticConsistencyFailure(OverAllState state, ExecutionStep.ToolParameters toolParameters)
			throws Exception {
		logger.info("Semantic consistency validation failed, starting to regenerate SQL");

		List<String> evidenceList = StateUtils.getListValue(state, EVIDENCES);
		SchemaDTO schemaDTO = StateUtils.getObjectValue(state, TABLE_RELATION_OUTPUT, SchemaDTO.class);

		return regenerateSql(state, toolParameters.toJsonStr(), evidenceList, schemaDTO,
				SEMANTIC_CONSISTENCY_NODE_RECOMMEND_OUTPUT, toolParameters.getSqlQuery());
	}

	/**
	 * Check if semantic consistency validation failed
	 */
	private boolean isSemanticConsistencyFailed(OverAllState state) {
		return StateUtils.getObjectValue(state, SEMANTIC_CONSISTENCY_NODE_OUTPUT, Boolean.class, true) == false;
	}

	/**
	 * 因首次计划执行失败，再次升成SQL采用增强的SQL重新生成 - 集成多轮优化、安全检查和性能分析
	 */
	private String regenerateSql(OverAllState state, String input, List<String> evidenceList, SchemaDTO schemaDTO,
			String exceptionOutputKey, String originalSql) throws Exception {
		String exceptionMessage = StateUtils.getStringValue(state, exceptionOutputKey);

		logger.info("开始增强SQL生成流程 - 原始SQL: {}, 异常信息: {}", originalSql, exceptionMessage);

		// 多轮SQL优化流程
		String bestSql = originalSql;
		double bestScore = 0.0;

		for (int round = 1; round <= MAX_OPTIMIZATION_ROUNDS; round++) {
			logger.info("开始第{}轮SQL优化", round);

			try {
				String currentSql;
				if (round == 1) {
					// 第一轮：使用原始服务生成基础SQL
					currentSql = baseNl2SqlService.generateSql(evidenceList, input, schemaDTO, originalSql,
							exceptionMessage);
				}
				else {
					// 后续轮次：使用ChatClient进行优化
					currentSql = generateOptimizedSql(bestSql, exceptionMessage, round);
				}

				if (currentSql == null || currentSql.trim().isEmpty()) {
					logger.warn("第{}轮SQL生成结果为空，跳过", round);
					continue;
				}

				// 评估SQL质量
				SqlQualityScore score = evaluateSqlQuality(currentSql, schemaDTO);
				logger.info("第{}轮SQL评分: 语法={}, 安全={}, 性能={}, 总分={}", round, score.syntaxScore, score.securityScore,
						score.performanceScore, score.totalScore);

				// 更新最佳SQL
				if (score.totalScore > bestScore) {
					bestSql = currentSql;
					bestScore = score.totalScore;
					logger.info("第{}轮产生了更好的SQL，总分提升到{}", round, score.totalScore);
				}

				// 质量足够高时提前结束
				if (score.totalScore >= 0.95) {
					logger.info("SQL质量分数达到{}，提前结束优化", score.totalScore);
					break;
				}

			}
			catch (Exception e) {
				logger.warn("第{}轮SQL优化失败: {}", round, e.getMessage());
			}
		}

		// 最终验证和清理
		bestSql = performFinalValidation(bestSql);

		logger.info("增强SQL生成完成，最终SQL: {}, 最终评分: {}", bestSql, bestScore);
		return bestSql;
	}

	/**
	 * 使用ChatClient生成优化的SQL
	 */
	private String generateOptimizedSql(String previousSql, String exceptionMessage, int round) {
		try {
			StringBuilder prompt = new StringBuilder();
			prompt.append("请对以下SQL进行第").append(round).append("轮优化:\n\n");
			prompt.append("当前SQL:\n").append(previousSql).append("\n\n");

			if (exceptionMessage != null && !exceptionMessage.trim().isEmpty()) {
				prompt.append("需要解决的问题:\n").append(exceptionMessage).append("\n\n");
			}

			prompt.append("优化目标:\n");
			prompt.append("1. 修复任何语法错误\n");
			prompt.append("2. 提升查询性能\n");
			prompt.append("3. 确保查询安全性\n");
			prompt.append("4. 优化可读性\n\n");
			prompt.append("请只返回优化后的SQL语句，不要包含其他说明。");

			String response = chatClient.prompt().user(prompt.toString()).call().content();

			return MarkdownParser.extractRawText(response).trim();
		}
		catch (Exception e) {
			logger.error("使用ChatClient优化SQL失败: {}", e.getMessage());
			return previousSql;
		}
	}

	/**
	 * 评估SQL质量
	 */
	private SqlQualityScore evaluateSqlQuality(String sql, SchemaDTO schemaDTO) {
		SqlQualityScore score = new SqlQualityScore();

		// 语法检查 (40%权重)
		score.syntaxScore = validateSqlSyntax(sql);

		// 安全检查 (30%权重)
		score.securityScore = validateSqlSecurity(sql);

		// 性能检查 (30%权重)
		score.performanceScore = evaluateSqlPerformance(sql);

		// 计算总分
		score.totalScore = (score.syntaxScore * 0.4 + score.securityScore * 0.3 + score.performanceScore * 0.3);

		return score;
	}

	/**
	 * 验证SQL语法
	 */
	private double validateSqlSyntax(String sql) {
		if (sql == null || sql.trim().isEmpty())
			return 0.0;

		double score = 1.0;
		String upperSql = sql.toUpperCase();

		// 基础语法检查
		if (!upperSql.contains("SELECT"))
			score -= 0.3;
		if (!upperSql.contains("FROM"))
			score -= 0.3;

		// 检查括号匹配
		long openParens = sql.chars().filter(ch -> ch == '(').count();
		long closeParens = sql.chars().filter(ch -> ch == ')').count();
		if (openParens != closeParens)
			score -= 0.2;

		// 检查引号匹配
		long singleQuotes = sql.chars().filter(ch -> ch == '\'').count();
		if (singleQuotes % 2 != 0)
			score -= 0.2;

		return Math.max(0.0, score);
	}

	/**
	 * 验证SQL安全性
	 */
	private double validateSqlSecurity(String sql) {
		if (sql == null)
			return 0.0;

		double score = 1.0;
		String upperSql = sql.toUpperCase();

		// 检查危险操作
		String[] dangerousKeywords = { "DROP", "DELETE", "UPDATE", "INSERT", "ALTER", "CREATE", "TRUNCATE" };
		for (String keyword : dangerousKeywords) {
			if (upperSql.contains(keyword)) {
				score -= 0.3;
				logger.warn("检测到潜在危险SQL操作: {}", keyword);
			}
		}

		// 检查SQL注入模式
		String[] injectionPatterns = { "--", "/*", "*/", "UNION", "OR 1=1", "OR '1'='1'" };
		for (String pattern : injectionPatterns) {
			if (upperSql.contains(pattern.toUpperCase())) {
				score -= 0.2;
				logger.warn("检测到潜在SQL注入模式: {}", pattern);
			}
		}

		return Math.max(0.0, score);
	}

	/**
	 * 评估SQL性能
	 */
	private double evaluateSqlPerformance(String sql) {
		if (sql == null)
			return 0.0;

		double score = 1.0;
		String upperSql = sql.toUpperCase();

		// 检查SELECT *
		if (upperSql.contains("SELECT *")) {
			score -= 0.2;
			logger.warn("检测到SELECT *，建议明确指定字段");
		}

		// 检查WHERE条件
		if (!upperSql.contains("WHERE")) {
			score -= 0.3;
			logger.warn("查询缺少WHERE条件，可能影响性能");
		}

		return Math.max(0.0, score);
	}

	/**
	 * 最终验证和清理
	 */
	private String performFinalValidation(String sql) {
		if (sql == null || sql.trim().isEmpty()) {
			throw new IllegalArgumentException("生成的SQL为空");
		}

		// 基础清理
		sql = sql.trim();
		if (!sql.endsWith(";")) {
			sql += ";";
		}

		// 安全检查
		if (validateSqlSecurity(sql) < 0.5) {
			logger.warn("生成的SQL存在安全风险，但继续执行");
		}

		return sql;
	}

	/**
	 * SQL质量评分
	 */
	private static class SqlQualityScore {

		double syntaxScore = 0.0;

		double securityScore = 0.0;

		double performanceScore = 0.0;

		double totalScore = 0.0;

	}

	/**
	 * Handle unsatisfied recall information
	 */
	private Map<String, Object> handleUnsatisfiedRecallInfo(OverAllState state, String recallInfoSatisfyRequirement) {
		int sqlGenerateCount = StateUtils.getObjectValue(state, SQL_GENERATE_COUNT, Integer.class, 0) + 1;

		logger.info(sqlGenerateCount == 1 ? "First time generating SQL" : "SQL generation count: {}", sqlGenerateCount);

		if (sqlGenerateCount <= MAX_RETRY_COUNT) {
			return buildRetryResult(state, recallInfoSatisfyRequirement, sqlGenerateCount);
		}
		else {
			logger.info("Recall information doesn't satisfy requirements, retry limit reached, ending SQL generation");
			return Map.of(RESULT, recallInfoSatisfyRequirement, SQL_GENERATE_OUTPUT, END, SQL_GENERATE_COUNT, 0);
		}
	}

	/**
	 * Build retry result
	 */
	private Map<String, Object> buildRetryResult(OverAllState state, String recallInfoSatisfyRequirement,
			int sqlGenerateCount) {
		logger.info("Recall information doesn't satisfy requirements, starting to regenerate SQL");

		Map<String, Object> result = new HashMap<>();
		result.put(SQL_GENERATE_COUNT, sqlGenerateCount);
		result.put(SQL_GENERATE_OUTPUT, SQL_GENERATE_SCHEMA_MISSING);

		String newAdvice = StateUtils.getStringValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE, "")
				+ (StateUtils.hasValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE) ? "\n" : "")
				+ recallInfoSatisfyRequirement;

		result.put(SQL_GENERATE_SCHEMA_MISSING_ADVICE, newAdvice);

		if (!StateUtils.hasValue(state, SQL_GENERATE_SCHEMA_MISSING_ADVICE)) {
			logger.info("Recall information doesn't satisfy requirements, need to supplement Schema information");
		}

		return result;
	}

}
