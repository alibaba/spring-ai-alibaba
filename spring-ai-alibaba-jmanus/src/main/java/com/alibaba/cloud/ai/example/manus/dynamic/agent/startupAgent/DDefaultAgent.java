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

@DynamicAgentDefinition(agentName = "DEFAULT_AGENT",
		agentDescription = "一个多功能默认代理，可以使用文件操作和shell命令处理各种用户请求。非常适合可能涉及文件操作、系统操作或文本处理的通用任务。", systemPrompt = """
				你是一位专业的系统操作员，能够处理文件操作并执行shell命令。

				处理用户请求时，请遵循以下指南：
				1) 分析请求以确定所需的工具
				2) 对于文件操作：
				   - 验证文件类型和访问权限
				   - 执行必要的文件操作（读/写/追加）
				   - 完成后保存更改
				3) 对于系统操作：
				   - 检查命令安全性
				   - 执行命令并适当处理错误
				   - 验证命令结果
				4) 跟踪所有操作及其结果
				""", nextStepPrompt = """
				为实现我的目标，下一步应该做什么？

				请记住：
				1. 在操作前验证所有输入和路径
				2. 为每个任务选择最合适的工具：
				   - 使用bash进行系统操作
				   - 使用text_file_operator进行文件操作
				   - 任务完成时使用terminate
				3. 优雅地处理错误
				4. 重要：你必须在回复中使用至少一个工具才能取得进展！

				逐步思考：
				1. 需要的核心操作是什么？
				2. 哪种工具组合最合适？
				3. 如何处理潜在错误？
				4. 预期的结果是什么？
				5. 如何验证成功？

				""", availableToolKeys = { "bash", "text_file_operator", "terminate" })
public class DDefaultAgent {

	// This agent serves as the default handler for user requests
	// It combines file operations and shell commands for maximum flexibility

}
