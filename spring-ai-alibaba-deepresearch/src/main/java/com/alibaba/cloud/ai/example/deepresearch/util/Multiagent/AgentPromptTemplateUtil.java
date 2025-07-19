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

package com.alibaba.cloud.ai.example.deepresearch.util.Multiagent;

import com.alibaba.cloud.ai.example.deepresearch.model.mutiagent.AgentType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Agent提示词模板工具类 从classpath加载markdown文件作为提示词模板
 *
 * @author Makoto
 * @since 2025/07/17
 */
public class AgentPromptTemplateUtil {

	private static final Map<AgentType, String> PROMPT_FILE_PATHS = Map.of(AgentType.ACADEMIC_RESEARCH,
			"prompts/mutiagent/academic-researcher.md", AgentType.LIFESTYLE_TRAVEL,
			"prompts/mutiagent/lifestyle-travel.md", AgentType.ENCYCLOPEDIA, "prompts/mutiagent/encyclopedia.md",
			AgentType.DATA_ANALYSIS, "prompts/mutiagent/data-analysis.md");

	private static final String CLASSIFIER_PROMPT_PATH = "prompts/mutiagent/classifier.md";

	/**
	 * 引用指导模板
	 */
	private static final Map<AgentType, String> CITATION_GUIDANCE = Map.of(AgentType.ACADEMIC_RESEARCH,
			"学术研究要求：请确保引用格式规范，优先引用高质量的学术资源。文末参考资料格式：\n- [论文标题 - 作者, 期刊/会议, 年份](URL)\n\n- [另一篇论文](URL)",

			AgentType.LIFESTYLE_TRAVEL,
			"生活旅游指南：请提供实用的建议和最新信息，引用可靠的旅游平台和生活服务信息。文末参考资料格式：\n- [资源标题 - 平台名称](URL)\n\n- [另一个资源](URL)",

			AgentType.ENCYCLOPEDIA, "百科知识解答：请确保信息准确性，引用权威的百科和官方资源。文末参考资料格式：\n- [百科条目 - 来源](URL)\n\n- [权威资料](URL)",

			AgentType.DATA_ANALYSIS,
			"数据分析报告：请引用官方统计数据和权威数据源，提供数据的时间范围和准确性说明。文末参考资料格式：\n- [数据来源 - 发布机构, 时间](URL)\n\n- [统计资料](URL)");

	private static String loadFileContent(String filePath) {
		try {
			ClassPathResource resource = new ClassPathResource(filePath);
			try (InputStream inputStream = resource.getInputStream()) {
				return StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
			}
		}
		catch (IOException e) {
			throw new RuntimeException("加载提示词文件失败: " + filePath, e);
		}
	}

	public static String getSystemPrompt(AgentType agentType) {
		String filePath = PROMPT_FILE_PATHS.get(agentType);
		if (filePath == null) {
			throw new RuntimeException("未找到Agent类型的提示词文件映射: " + agentType);
		}
		return loadFileContent(filePath);
	}

	/**
	 * 获取问题分类的系统提示词
	 * @return 分类系统提示词
	 * @throws RuntimeException 当文件加载失败时抛出异常
	 */
	public static String getClassificationPrompt() {
		return loadFileContent(CLASSIFIER_PROMPT_PATH);
	}

	public static String getCitationGuidance(AgentType agentType) {
		return CITATION_GUIDANCE.getOrDefault(agentType,
				"重要提示：请避免在文本中使用内联引用，而是在文末添加参考资料部分，使用链接引用格式。每个引用之间留空行以提高可读性。格式：\n- [资料标题](URL)\n\n- [另一个资料](URL)");
	}

	/**
	 * 构建完整的Agent提示词（系统提示词 + 引用指导）
	 * @param agentType Agent类型
	 * @return 完整的提示词
	 * @throws RuntimeException 当Agent类型不支持或文件加载失败时抛出异常
	 */
	public static String buildCompletePrompt(AgentType agentType) {
		return getSystemPrompt(agentType) + "\n\n" + getCitationGuidance(agentType);
	}

}
