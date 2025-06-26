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

@DynamicAgentDefinition(agentName = "DATA_PREPARE_AGENT",
		agentDescription = "一个数据准备代理，负责验证文件/文件夹存在性、识别表格头部信息并调用分割工具进行数据分割处理，固定的用于MapReduce 的开始的 Preparation  准备 环节。",
		nextStepPrompt = """
				你是一个数据准备代理，专门执行以下三个核心任务：

				标准工作流程：
				1) 验证文件或文件夹是否存在，使用text_file_operator工具来做。
				2) 使用text_file_operator工具，读取前2000个字符来查找表格头部信息
				3) 使用表格头部信息（如果存在）和文件/文件夹路径调用分割工具

				为完成数据准备任务，下一步应该做什么？

				请记住：
				1. 首先验证目标文件或文件夹是否存在
				2. 如果文件存在，读取前2000个字符查找表格头部
				3. 识别出表格头部后，调用分割工具进行处理
				4. 如果没有找到表格头部，也要调用分割工具但不提供头部信息
				5. 重要：你必须在回复中使用至少一个工具才能取得进展！

				逐步思考：
				1. 文件/文件夹路径是什么？
				2. 目标是否存在？
				3. 是否能识别到表格头部？
				4. 如何正确调用分割工具？
				""", availableToolKeys = { "text_file_operator", "map_reduce_tool", "terminate","inner_storage_tool" })
public class DDataPrepareAgent {

}
