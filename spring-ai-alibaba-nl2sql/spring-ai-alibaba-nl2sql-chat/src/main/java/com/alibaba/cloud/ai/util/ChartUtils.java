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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图表工具类，用于处理报告中的图表生成、保存和引用
 *
 * @author zhangshenghang
 */
public class ChartUtils {

	private static final Logger logger = LoggerFactory.getLogger(ChartUtils.class);

	private static final String DEFAULT_CHARTS_DIR = "charts";

	private static final Pattern CHART_PATH_PATTERN = Pattern.compile("!\\[.*?\\]\\((.*?)\\)");

	private static final Pattern MERMAID_CHART_PATTERN = Pattern.compile("```mermaid\\s+([\\s\\S]*?)\\s+```");

	/**
	 * 从文本中提取所有图表路径
	 * @param text 包含图表引用的文本
	 * @return 图表路径列表
	 */
	public static List<String> extractChartPaths(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		List<String> paths = new ArrayList<>();
		Matcher matcher = CHART_PATH_PATTERN.matcher(text);
		while (matcher.find()) {
			String path = matcher.group(1);
			paths.add(path);
		}
		return paths;
	}

	/**
	 * 从文本中提取所有Mermaid图表定义
	 * @param text 包含Mermaid图表定义的文本
	 * @return Mermaid图表内容列表
	 */
	public static List<String> extractMermaidCharts(String text) {
		if (text == null || text.isEmpty()) {
			return new ArrayList<>();
		}

		List<String> charts = new ArrayList<>();
		Matcher matcher = MERMAID_CHART_PATTERN.matcher(text);
		while (matcher.find()) {
			String chart = matcher.group(1);
			charts.add(chart);
		}
		return charts;
	}

	/**
	 * 生成图表的唯一路径
	 * @param baseDir 基础目录，可为null则使用默认目录
	 * @param fileExtension 文件扩展名（如png, svg等）
	 * @return 图表文件路径
	 */
	public static String generateChartPath(String baseDir, String fileExtension) {
		String dir = baseDir != null ? baseDir : DEFAULT_CHARTS_DIR;
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
		String uuid = UUID.randomUUID().toString().substring(0, 8);
		String fileName = String.format("chart_%s_%s.%s", timestamp, uuid, fileExtension);

		try {
			Path dirPath = Paths.get(dir);
			if (!Files.exists(dirPath)) {
				Files.createDirectories(dirPath);
			}
			return Paths.get(dir, fileName).toString();
		}
		catch (IOException e) {
			logger.error("创建图表目录失败: {}", e.getMessage());
			return fileName;
		}
	}

	/**
	 * 检查图表文件是否存在
	 * @param path 图表文件路径
	 * @return 是否存在
	 */
	public static boolean chartExists(String path) {
		return Files.exists(Paths.get(path));
	}

	/**
	 * 将图表路径转换为相对路径（适合在Markdown中引用）
	 * @param absolutePath 绝对路径
	 * @param basePath 基准路径，如果为null则返回原路径
	 * @return 相对路径
	 */
	public static String toRelativePath(String absolutePath, String basePath) {
		if (absolutePath == null || basePath == null) {
			return absolutePath;
		}

		try {
			Path pathAbsolute = Paths.get(absolutePath);
			Path pathBase = Paths.get(basePath);
			return pathBase.relativize(pathAbsolute).toString().replace('\\', '/');
		}
		catch (Exception e) {
			logger.error("转换为相对路径失败: {}", e.getMessage());
			return absolutePath;
		}
	}

	/**
	 * 清理过期图表（超过指定天数的图表）
	 * @param baseDir 图表目录
	 * @param daysOld 过期天数
	 * @return 删除的文件数量
	 */
	public static int cleanupOldCharts(String baseDir, int daysOld) {
		String dir = baseDir != null ? baseDir : DEFAULT_CHARTS_DIR;
		Path dirPath = Paths.get(dir);
		if (!Files.exists(dirPath)) {
			return 0;
		}

		int count = 0;
		try {
			LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysOld);
			Files.list(dirPath).forEach(path -> {
				try {
					LocalDateTime fileTime = LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(),
							java.time.ZoneId.systemDefault());

					if (fileTime.isBefore(cutoffDate)) {
						Files.delete(path);
					}
				}
				catch (IOException e) {
					logger.warn("删除过期图表失败: {}", e.getMessage());
				}
			});
		}
		catch (IOException e) {
			logger.error("清理过期图表失败: {}", e.getMessage());
		}

		return count;
	}

	/**
	 * 创建图表的Markdown引用
	 * @param chartPath 图表路径
	 * @param altText 替代文本
	 * @return Markdown格式的图表引用
	 */
	public static String createChartReference(String chartPath, String altText) {
		String alt = altText != null ? altText : "图表";
		return String.format("![%s](%s)", alt, chartPath);
	}

}
