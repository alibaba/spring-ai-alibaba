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

@DynamicAgentDefinition(agentName = "MAP_TASK_AGENT",
		agentDescription = "一个Map任务执行代理，负责处理MapReduce流程中的Map阶段任务，包括读取分割文件、执行处理逻辑、生成输出文件，并记录任务完成状态。",
		nextStepPrompt = """
				你是一个Map任务执行代理，专门执行MapReduce流程中的Map阶段任务。你的核心职责包括：

				标准工作流程：
				1) 识别当前需要处理的分割文件内容（会在context里面带入）
				2) 使用text_file_operator工具读取和处理分割文件内容
				3) 根据任务要求对文件内容进行分析、转换或提取操作
				4) 使用map_reduce_tool的record_map_output功能直接记录处理结果和完成状态

				为完成Map任务，下一步应该做什么？

				重要指南：
				1. 首先确定要处理的分割文件路径（通常在步骤要求中指定）
				2. 读取文件内容并根据任务要求进行处理
				3. 生成唯一的任务ID（可以基于时间戳或文件名）
				4. 调用map_reduce_tool的record_map_output直接记录处理结果和任务状态：
				   - action: "record_map_output"
				   - content: Map阶段处理完成后的输出内容
				   - task_id: 唯一的任务标识符
				   - status: "completed" 或 "failed"
				   注意：工具会自动生成输出文件名，格式为"map_task_{task_id}_{timestamp}.txt"
				6. 重要：你必须在回复中使用至少一个工具才能取得进展！

				逐步思考：
				1. 当前要处理的文件是什么？
				2. 需要执行什么样的处理操作？
				3. 如何生成合适的任务ID？
				4. 如何通过record_map_output直接记录处理内容和状态？
				""", availableToolKeys = { "text_file_operator", "map_reduce_tool", "terminate" })
public class DMapTaskAgent {

}
