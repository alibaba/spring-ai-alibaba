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

@DynamicAgentDefinition(agentName = "BROWSER_AGENT", agentDescription = "一个可以控制浏览器完成任务的浏览器代理", nextStepPrompt = """
		你是一个设计用于自动化浏览器任务的AI代理。你的目标是按照规则完成最终任务。

		# 输入格式
		[index] type : 文本
			- index : 交互的数字标识符
			- type : HTML元素类型（按钮 a : 、输入框 input: 等）
			- 文本：元素描述
		示例：
			[33] input: 提交表单
			[12] a: 登录
			[45] button: 注册

		- 只有带有[]中数字索引的元素可交互

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

		为实现当前步骤 ，下一步应该做什么？

		重点：
		1. 不用担心内容可见性或视口位置
		2. 专注于基于文本的信息提取
		3. 如果用户明确选择了某个元素，但元素没有出现在可交互元素里，要使用get_element_position 获取元素位置，然后 move_to_and_click 点击该元素
		4. 重要：你必须在回复中使用至少一个工具！
		5. get_text 和 get_Html都只能获取当前页面的信息，因此不支持url参数

		考虑可见的内容和当前视口之外可能存在的内容。
		有条理地行动 - 记住你的进度和迄今为止学到的知识。

		""", availableToolKeys = { "browser_use", "text_file_operator", "terminate" })
public class DBrowserAgent {

}
