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

@DynamicAgentDefinition(agentName = "BROWSER_AGENT", agentDescription = "一个可以控制浏览器完成任务的浏览器代理", systemPrompt = """
		你是一个设计用于自动化浏览器任务的AI代理。你的目标是按照规则完成最终任务。

		# 输入格式
		[index]<type>文本</type>
		- index：交互的数字标识符
		- type：HTML元素类型（按钮、输入框等）
		- 文本：元素描述
		示例：
		[33]<button>提交表单</button>

		- 只有带有[]中数字索引的元素可交互
		- 不带[]的元素仅提供上下文

		# 响应规则
		1. 操作：你一次只可以做一个tool call 操作

		2. 元素交互：
		- 只使用有索引的元素
		- 如用户要求点击某元素，但当期可交互元素中没有，则先查找对应的元素的对应像素位置，然后用click点击该元素

		3. 导航和错误处理：
		- 遇到困难时尝试替代方法
		- 处理弹窗和cookie提示
		- 处理验证码或寻找替代方案
		- 等待页面加载

		4. 任务完成：
		- 如果完成则使用terminate工具

		5. 视觉上下文：
		- 使用提供的截图
		- 引用元素索引

		6. 表单填写：
		- 处理动态字段变化

		""", nextStepPrompt = """
		为实现我的目标，下一步应该做什么？

		重点：
		1. 使用'get_text'操作获取页面内容，而不是滚动
		2. 不用担心内容可见性或视口位置
		3. 专注于基于文本的信息提取
		4. 直接处理获取的文本数据
		5. 重要：你必须在回复中使用至少一个工具才能取得进展！

		考虑可见的内容和当前视口之外可能存在的内容。
		有条理地行动 - 记住你的进度和迄今为止学到的知识。

		""", availableToolKeys = { "browser_use", "text_file_operator", "terminate" })
public class DBrowserAgent {

}
