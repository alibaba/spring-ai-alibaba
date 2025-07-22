/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.prompt;

import org.springframework.ai.chat.prompt.PromptTemplate;

/**
 * 提示词常量类，动态加载提示词文件
 *
 * @author zhangshenghang
 */
public class PromptConstant {

	// 提示词模板获取方法
	public static PromptTemplate getInitRewritePromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("init-rewrite"));
	}

	public static PromptTemplate getQuestionToKeywordsPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("question-to-keywords"));
	}

	public static PromptTemplate getMixSelectorPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("mix-selector"));
	}

	public static PromptTemplate getMixSqlGeneratorSystemPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("mix-sql-generator-system"));
	}

	public static PromptTemplate getMixSqlGeneratorPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("mix-sql-generator"));
	}

	public static PromptTemplate getExtractDatetimePromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("extract-datetime"));
	}

	public static PromptTemplate getSemanticConsistencyPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("semantic-consistency"));
	}

	public static PromptTemplate getMixSqlGeneratorSystemCheckPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("mix-sql-generator-system-check"));
	}

	public static PromptTemplate getPlannerPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("planner"));
	}

	public static PromptTemplate getReportGeneratorPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("report-generator"));
	}

	public static PromptTemplate getSqlErrorFixerPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("sql-error-fixer"));
	}

	public static PromptTemplate getPythonExecutorPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("python-executor"));
	}

	public static PromptTemplate getQuestionExpansionPromptTemplate() {
		return new PromptTemplate(PromptLoader.loadPrompt("question-expansion"));
	}

	// 兼容性方法，保持向后兼容
	@Deprecated
	public static final PromptTemplate INIT_REWRITE_PROMPT_TEMPLATE = getInitRewritePromptTemplate();

	@Deprecated
	public static final PromptTemplate QUESTION_TO_KEYWORDS_PROMPT_TEMPLATE = getQuestionToKeywordsPromptTemplate();

	@Deprecated
	public static final PromptTemplate MIX_SELECTOR_PROMPT_TEMPLATE = getMixSelectorPromptTemplate();

	@Deprecated
	public static final PromptTemplate MIX_SQL_GENERATOR_SYSTEM_PROMPT_TEMPLATE = getMixSqlGeneratorSystemPromptTemplate();

	@Deprecated
	public static final PromptTemplate MIX_SQL_GENERATOR_PROMPT_TEMPLATE = getMixSqlGeneratorPromptTemplate();

	@Deprecated
	public static final PromptTemplate EXTRACT_DATETIME_PROMPT_TEMPLATE = getExtractDatetimePromptTemplate();

	@Deprecated
	public static final PromptTemplate SEMANTIC_CONSISTENCY_PROMPT_TEMPLATE = getSemanticConsistencyPromptTemplate();

	@Deprecated
	public static final PromptTemplate MIX_SQL_GENERATOR_SYSTEM_PROMPT_CHECK_TEMPLATE = getMixSqlGeneratorSystemCheckPromptTemplate();

}
