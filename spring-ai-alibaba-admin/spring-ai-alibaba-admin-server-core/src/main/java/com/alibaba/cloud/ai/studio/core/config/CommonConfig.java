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

package com.alibaba.cloud.ai.studio.core.config;

import com.google.common.collect.Sets;
import lombok.Data;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

import static com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum.API;
import static com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum.MCP;
import static com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum.PLUGIN;
import static com.alibaba.cloud.ai.studio.runtime.domain.workflow.NodeTypeEnum.SCRIPT;

/**
 * Common configuration class for Studio service. Contains various configuration
 * parameters for conversation, workflow, and system behavior.
 *
 * @since 1.0.0.3
 */

@Configuration
// @NacosPropertySource(dataId = "spring.ai.alibaba.studio.config", groupId =
// "saa-studio", autoRefreshed = true)
@Data
public class CommonConfig {

	// TTL for conversation memory in cache (in seconds)
	private Long conversationMemoryTtlInCache = 3600L;

	// Maximum number of conversation rounds to store in cache
	private Integer maxConversationRoundInCache = 50;

	// Timeout for agent read operations (in milliseconds)
	private Integer agentReadTimeout = 180000;

	// Input timeout duration (in milliseconds)
	private Long inputTimeout = 5 * 60 * 1000L;

	// Workflow awaiting time between operations (in milliseconds)
	private Long workflowAwaitingTime = 100L;

	// Template for file search prompt
	private String fileSearchPrompt = """
			# Knowledge Base
			Please remember the following materials, they may be helpful in answering questions.
			${documents}
			""";

	private String citationPrompt = """
			指令：您需要仅使用提供的搜索文档为给定问题写出高质量的答案，并正确引用它们。 引用多个搜索结果时，请使用<ref>[1]</ref>或<ref>[1][3]</ref>等格式。 请注意，每个句子中必须至少引用一个文档。 换句话说，你禁止在没有引用任何文献的情况下写句子。 此外，您应该在每个句子中添加引用符号，尤其是在句号（punct.）之前。

			$$材料：
			[1] 【文档名】植物中的光合作用.pdf
			【标题】光合作用位置
			【正文】光合作用主要在叶绿体中进行，涉及光能到化学能的转化。
			[2] 【文档名】光合作用.pdf
			【标题】光合作用转化
			【正文】光合作用是利用阳光将CO2和H2O转化为氧气和葡萄糖的过程。

			问题：光合作用的基本过程是什么？

			推理步骤：

			步骤1：我判断文档[1]和文档[2]与问题相关。

			步骤2：根据文档[1]，我写了一个回答陈述并引用了该文档，即"这一过程主要在叶绿体中进行，其中光能被叶绿素吸收，并通过一系列化学反应转化为化学能，存储在产生的葡萄糖中<ref>[1]</ref>。"

			步骤3：根据文档[2]，我写一个答案声明并引用该文档，即"光合作用是植物、藻类和某些细菌利用阳光将二氧化碳和水转化为氧气和葡萄糖的过程<ref>[2]</ref>。""

			步骤4：我将以上两个答案语句进行合并、排序和连接，以获得流畅连贯的答案。

			答案：光合作用是植物、藻类和某些细菌利用阳光将二氧化碳和水转化为氧气和葡萄糖的过程<ref>[2]</ref>。这一过程主要在叶绿体中进行，其中光能被叶绿素吸收，并通过一系列化学反应转化为化学能，存储在产生的葡萄糖中<ref>[1]</ref>。

			$$材料：
			""";

	/**
	 * Node types that support retry on exception
	 */
	private Set<String> retrySupportNodeTypeSet = Sets.newHashSet(SCRIPT.getCode(), API.getCode(), PLUGIN.getCode(),
			MCP.getCode());

	/**
	 * Node types that support try-catch exception handling
	 */
	private Set<String> tryCatchSupportNodeTypeSet = Sets.newHashSet(SCRIPT.getCode(), API.getCode(), PLUGIN.getCode(),
			MCP.getCode());

	private String workflowRefreshInterval = "{\"console\":3000,\"async\":5000}}";

}
