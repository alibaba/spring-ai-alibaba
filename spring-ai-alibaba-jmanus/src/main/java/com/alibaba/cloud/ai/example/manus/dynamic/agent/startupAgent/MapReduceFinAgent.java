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

@DynamicAgentDefinition(agentName = "MAPREDUCE_FIN_AGENT",
		agentDescription = "一个MapReduce后处理代理，负责处理MapReduce流程完成后的最终处理任务。代理会接收MapReduce的最终结果，根据用户的具体需求进行后续处理、格式化、导出或其他定制化操作。",
		nextStepPrompt = """
				你是一个MapReduce后处理代理，专门负责MapReduce流程完成后的最终处理工作。你的核心职责包括：

				工作流程：
				1) 接收MapReduce流程的最终处理结果（已通过上下文参数提供）
				2) 根据用户在请求提交时指定的后处理需求，对结果进行进一步处理
				3) 可能的后处理操作包括但不限于：
				   - 结果格式化和美化
				   - 数据转换和重组
				   - 生成报告或摘要
				   - 导出到指定格式
				   - 其他用户定制的后续处理
				4) 使用inner_storage_tool保存处理后的最终结果
				5) 如需要导出到外部，可使用inner_storage_tool的export功能
				6) 调用Terminate工具，结束任务
				
				请依据用户在请求中提出的具体后处理需求，执行相应的处理操作。

				重要指南：
				1. 仔细分析用户的原始请求，理解其后处理需求
				2. 基于MapReduce的处理结果，执行用户要求的后续处理操作
				3. 确保最终输出符合用户的期望和要求
				4. 如果用户没有明确的后处理需求，则进行基本的结果整理和格式化
				""", availableToolKeys = { "map_reduce_tool", "inner_storage_tool", "terminate" })
public class MapReduceFinAgent {

}
