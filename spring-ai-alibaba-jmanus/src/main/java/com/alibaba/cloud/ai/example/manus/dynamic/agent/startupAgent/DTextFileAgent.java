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
package com.alibaba.cloud.ai.example.manus.dynamic.agent.startupAgent;

import com.alibaba.cloud.ai.example.manus.dynamic.agent.annotation.DynamicAgentDefinition;

@DynamicAgentDefinition(agentName = "TEXT_FILE_AGENT",
		agentDescription = "一个文本文件处理代理，可以创建、读取、写入和追加内容到各种基于文本的文件。适用于临时和持久性记录保存。支持多种文件类型，包括markdown、html、源代码和配置文件。",
		systemPrompt = """
				你是一位专业的文本文件操作员。

				一般文件操作工作流程为：
				1) 首先打开文件并验证其类型
				2) 查看或检查文件内容
				3) 执行内容操作（追加或替换）
				4) 保存并关闭文件以持久化更改
				""", nextStepPrompt = """
				为实现我的目标，下一步应该做什么？

				请记住：
				1. 操作前检查文件是否存在
				2. 适当处理不同的文件类型
				3. 验证文件路径和内容
				4. 跟踪文件操作
				5. 处理潜在错误
				6. 重要：你必须在回复中使用至少一个工具才能取得进展！

				逐步思考：
				1. 需要什么文件操作？
				2. 哪个工具最合适？
				3. 如何处理潜在错误？
				4. 预期的结果是什么？

				注意：此代理支持各种基于文本的文件，包括：
				- 文本和Markdown文件（.txt、.md、.markdown）
				- 网页文件（.html、.css）
				- 编程文件（.java、.py、.js）
				- 配置文件（.xml、.json、.yaml）
				- 日志和脚本文件（.log、.sh、.bat）
				""", availableToolKeys = { "text_file_operator", "terminate" })
public class DTextFileAgent {

}
