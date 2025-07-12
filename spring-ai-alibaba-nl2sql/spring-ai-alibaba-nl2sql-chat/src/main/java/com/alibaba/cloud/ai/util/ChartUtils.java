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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图表工具类，用于处理分析结果中的图表路径
 *
 * @author Makoto
 */
public class ChartUtils {

	private static final Logger logger = LoggerFactory.getLogger(ChartUtils.class);

	/**
	 * 从分析结果中提取图表路径
	 * @param analysisResult Python分析结果文本
	 * @return 提取的图表路径列表
	 */
	public static List<String> extractChartPaths(String analysisResult) {
		List<String> chartPaths = new ArrayList<>();

		if (analysisResult == null || analysisResult.isEmpty()) {
			return chartPaths;
		}

		try {
			Pattern markdownPattern = Pattern.compile("!\\[(.*?)\\]\\((.*?)\\)");
			Matcher markdownMatcher = markdownPattern.matcher(analysisResult);
			while (markdownMatcher.find()) {
				chartPaths.add(markdownMatcher.group(0));
			}

			Pattern htmlPattern = Pattern.compile("<img\\s+[^>]*src\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>");
			Matcher htmlMatcher = htmlPattern.matcher(analysisResult);
			while (htmlMatcher.find()) {
				chartPaths.add(htmlMatcher.group(0));
			}

			Pattern filePathPattern = Pattern.compile(
					"(?i)(?:图表|chart|figure|图像|image)(?:已?保存|saved|generated)(?:到|at|to)?[\\s:]*[\"']?([\\/\\\\\\w\\.\\-_]+\\.(png|jpg|jpeg|svg|pdf))[\"']?");
			Matcher filePathMatcher = filePathPattern.matcher(analysisResult);
			while (filePathMatcher.find()) {
				chartPaths.add(filePathMatcher.group(1));
			}

			Pattern directPathPattern = Pattern.compile("[\\/\\\\\\w\\.\\-_]+\\.(png|jpg|jpeg|svg|pdf)");
			Matcher directPathMatcher = directPathPattern.matcher(analysisResult);
			while (directPathMatcher.find()) {
				String path = directPathMatcher.group(0);
				boolean alreadyIncluded = chartPaths.stream().anyMatch(cp -> cp.contains(path));
				if (!alreadyIncluded) {
					chartPaths.add(path);
				}
			}
		}
		catch (Exception e) {
			logger.error("提取图表路径时发生错误", e);
		}

		return chartPaths;
	}

	/**
	 * 从分析结果中移除图表路径，避免在文本中重复显示
	 * @param analysisResult Python分析结果文本
	 * @param chartPaths 要移除的图表路径列表
	 * @return 清理后的分析结果
	 */
	public static String removeChartPaths(String analysisResult, List<String> chartPaths) {
		if (analysisResult == null || analysisResult.isEmpty() || chartPaths == null || chartPaths.isEmpty()) {
			return analysisResult;
		}

		String cleanedResult = analysisResult;

		try {
			for (String chartPath : chartPaths) {
				if (chartPath.startsWith("![")) {
					cleanedResult = cleanedResult.replace(chartPath, "");
					continue;
				}

				if (chartPath.startsWith("<img")) {
					cleanedResult = cleanedResult.replace(chartPath, "");
					continue;
				}

				Pattern filePathPattern = Pattern
					.compile("(?i)(?:图表|chart|figure|图像|image)(?:已?保存|saved|generated)(?:到|at|to)?[\\s:]*[\"']?"
							+ Pattern.quote(chartPath) + "[\"']?[\\s\\.,]*");
				Matcher filePathMatcher = filePathPattern.matcher(cleanedResult);
				if (filePathMatcher.find()) {
					cleanedResult = filePathMatcher.replaceAll("");
				}
				else {
					cleanedResult = cleanedResult.replace(chartPath, "");
				}
			}

			cleanedResult = cleanedResult.replaceAll("(?m)^\\s*$\\n", "");
		}
		catch (Exception e) {
			logger.error("移除图表路径时发生错误", e);
			return analysisResult;
		}

		return cleanedResult;
	}

	/**
	 * 为图表生成HTML容器和标题
	 * @param chartPath 图表路径
	 * @param chartNumber 图表编号
	 * @param stepNumber 关联的步骤编号
	 * @return 包含图表和标题的HTML代码
	 */
	public static String generateChartContainer(String chartPath, int chartNumber, int stepNumber) {
		StringBuilder chartContainer = new StringBuilder();

		chartContainer.append("<div class='chart-container'>\n");

		if (chartPath.startsWith("![") || chartPath.startsWith("<img")) {
			chartContainer.append(chartPath).append("\n");
		}
		else {
			chartContainer.append("![数据图表](").append(chartPath).append(")\n");
		}

		chartContainer.append("<div class='chart-caption'>图 ")
			.append(chartNumber)
			.append("：与步骤")
			.append(stepNumber)
			.append("相关的分析图表</div>\n</div>\n\n");

		return chartContainer.toString();
	}

}
