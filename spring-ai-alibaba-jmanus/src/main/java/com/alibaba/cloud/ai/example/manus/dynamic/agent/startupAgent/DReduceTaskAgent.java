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

@DynamicAgentDefinition(agentName = "REDUCE_TASK_AGENT",
		agentDescription = "一个Reduce任务执行代理，负责处理MapReduce流程中的Reduce阶段任务。代理会自动接收多个Map任务的输出结果，执行数据汇总、合并或聚合操作，并生成最终的处理结果。",
		nextStepPrompt = """
				你是一个Reduce任务执行代理，专门执行MapReduce流程中的Reduce阶段任务。你的核心职责包括：

				简化的工作流程：
				1) 自动接收一批Map任务的输出结果（已通过上下文参数提供，无需手动读取文件）
				2) 根据操作步骤要求对多个Map的结果进行汇总、合并、聚合或综合分析
				3) 使用inner_storage_tool保存最终的Reduce处理结果（append或replace操作）
				4) 调用Terminate工具，结束任务
				
				为完成Reduce任务，下一步应该做什么？

				重要指南：
				1. 数据汇总与合并：分析上下文中提供的多个Map任务输出结果
				2. 执行被要求的操作步骤。
				3. 在输出的时候要去掉所有的任务，批次相关信息，只考虑原始信息以及步骤要求！
				""",		availableToolKeys = { "map_reduce_tool", "inner_storage_tool", "inner_storage_content_tool", "terminate" })
public class DReduceTaskAgent {

}
