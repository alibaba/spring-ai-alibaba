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
		nextStepPrompt = """
				你是一位专业的文本文件操作员。

				一般文件操作工作流程为：
				1) 首先打开文件并验证其类型
				2) 查看或检查文件内容
				3) 执行内容操作（追加或替换）
				4) 保存并关闭文件以持久化更改

				为实现我的目标，下一步应该做什么？

				请记住：
				1. 操作前检查文件是否存在
				2. 适当处理不同的文件类型
				3. 验证文件路径和内容
				4. 跟踪文件操作
				5. 处理潜在错误
				6. 重要
				- 务必从所提供的工具中进行选择调用，可以对单个工具进行重复调用，或者同时调用多个工具，亦或采用混合调用的方式，以此来提升问题解决的效率与精准度。
				- 在你的回复中，必须至少调用一次工具，这是不可或缺的操作步骤。
				- 为了最大化利用工具的优势，当你有能力同时调用工具多次时，应积极这样做，避免仅进行单次调用造成时间及资源的浪费。并且要格外留意多次调用工具之间存在的内在关联性，确保这些调用能够相互配合、协同工作，以达成最优的问题解决方案。

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
				""",
		availableToolKeys = { "text_file_operator", "terminate" })
public class DTextFileAgent {

}
