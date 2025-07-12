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

package com.alibaba.cloud.ai.util;

import com.alibaba.cloud.ai.schema.Plan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 报告工具类，用于处理报告模板构建和占位符替换等功能
 *
 * @author Makoto
 */
public class ReportUtils {

	private static final Logger logger = LoggerFactory.getLogger(ReportUtils.class);

	/**
	 * 创建报告基本结构
	 * @param userQuery 用户查询问题
	 * @param plan 分析计划
	 * @return 报告基本结构
	 */
	public static StringBuilder createReportStructure(String userQuery, Plan plan) {
		StringBuilder reportData = new StringBuilder();

		reportData.append("# {TITLE}\n\n");

		reportData.append("## 数据概览\n");
		reportData.append("分析时间：{DATETIME}\n");
		reportData.append("数据范围：{DATA_RANGE}\n");
		reportData.append("分析目的：基于用户问题「").append(userQuery).append("」进行数据分析\n\n");

		reportData.append("## 分析背景\n").append(userQuery).append("\n\n");
		reportData.append("## 分析方法\n").append(plan.getThoughtProcess()).append("\n\n");

		return reportData;
	}

	/**
	 * 添加关键发现与建议章节
	 * @param reportData 报告构建器
	 */
	public static void addFindingsAndRecommendations(StringBuilder reportData) {
		reportData.append("## 关键发现与洞察\n\n");
		reportData.append("基于上述分析，以下是关键发现：\n\n");
		reportData.append("1. {KEY_FINDING_1}\n");
		reportData.append("2. {KEY_FINDING_2}\n");
		reportData.append("3. {KEY_FINDING_3}\n\n");
		reportData.append("## 业务建议\n\n");
		reportData.append("根据分析结果，提出以下建议：\n\n");
		reportData.append("1. {RECOMMENDATION_1}\n");
		reportData.append("2. {RECOMMENDATION_2}\n");
		reportData.append("3. {RECOMMENDATION_3}\n\n");
	}

	/**
	 * 添加图表汇总章节
	 * @param reportData 报告构建器
	 * @param chartPaths 图表路径列表
	 */
	public static void addChartSummary(StringBuilder reportData, List<String> chartPaths) {
		if (chartPaths == null || chartPaths.isEmpty()) {
			return;
		}

		reportData.append("## 附录：数据图表汇总\n\n");
		for (int i = 0; i < chartPaths.size(); i++) {
			String chartPath = chartPaths.get(i);
			reportData.append("### 图表 ").append(i + 1).append("\n");
			if (chartPath.startsWith("![") || chartPath.startsWith("<img")) {
				reportData.append(chartPath).append("\n\n");
			}
			else {
				reportData.append("![数据图表](").append(chartPath).append(")\n\n");
			}
		}
	}

	/**
	 * 添加跨维度分析章节
	 * @param reportData 报告构建器
	 * @param pythonResults Python分析结果映射
	 */
	public static void addCrossDimensionalAnalysis(StringBuilder reportData, Map<Integer, String> pythonResults) {
		if (pythonResults == null || pythonResults.size() <= 1) {
			return;
		}

		reportData.append("## 跨维度分析\n\n");
		reportData.append("基于多个维度的分析结果，以下是综合性的发现和洞察：\n\n");
	}

	/**
	 * 替换报告中的基本占位符
	 * @param reportData 报告数据
	 * @return 替换占位符后的报告数据
	 */
	public static String replaceBasicPlaceholders(String reportData) {
		if (reportData == null) {
			return "";
		}

		return reportData
			.replace("{DATETIME}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
			.replace("{DATA_RANGE}", "完整数据集");
	}

	/**
	 * 构建AI提示信息
	 * @param userQuery 用户查询
	 * @param reportData 报告数据
	 * @return AI提示信息
	 */
	public static String buildAIPrompt(String userQuery, String reportData) {
		return String
			.format("请根据以下数据和执行过程为用户问题「%s」生成一份专业的分析报告。\n\n" + "报告中包含以下占位符需要你填充：\n" + "1. {TITLE} - 根据分析内容生成一个专业的报告标题\n"
					+ "2. {KEY_FINDING_1}, {KEY_FINDING_2}, {KEY_FINDING_3} - 分析结果中的关键发现\n"
					+ "3. {RECOMMENDATION_1}, {RECOMMENDATION_2}, {RECOMMENDATION_3} - 基于分析的业务建议\n\n"
					+ "请确保报告风格专业、内容结构清晰，并且突出重点数据和关键洞察。\n\n%s", userQuery, reportData);
	}

	/**
	 * 读取提示词文件
	 * @param promptFilePath 提示词文件路径
	 * @return 提示词内容
	 */
	public static String readPromptFile(String promptFilePath) {
		try {
			try (InputStream inputStream = ReportUtils.class.getClassLoader().getResourceAsStream(promptFilePath)) {
				if (inputStream != null) {
					return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
				}
				else {
					logger.error("找不到提示词文件: {}", promptFilePath);
				}
			}
		}
		catch (Exception e) {
			logger.error("读取提示词文件失败", e);
		}
		return "你是一位专业的数据分析师，需要生成一份详尽而专业的分析报告。报告应包含清晰的标题、数据概览、关键发现和业务建议等内容。请确保报告结构清晰，并针对用户问题提供有深度的见解。";
	}

}
