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
package com.alibaba.cloud.ai.example.manus.planning.service;

import java.util.Map;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.UserInputWaitState;
import com.alibaba.cloud.ai.example.manus.tool.FormInputTool;

/**
 * 用户输入服务接口，管理用户输入相关功能
 */
public interface IUserInputService {

	/**
	 * 存储表单输入工具
	 * @param planId 计划ID
	 * @param tool 表单输入工具
	 */
	void storeFormInputTool(String planId, FormInputTool tool);

	/**
	 * 获取表单输入工具
	 * @param planId 计划ID
	 * @return 表单输入工具
	 */
	FormInputTool getFormInputTool(String planId);

	/**
	 * 移除表单输入工具
	 * @param planId 计划ID
	 */
	void removeFormInputTool(String planId);

	/**
	 * 创建用户输入等待状态
	 * @param planId 计划ID
	 * @param message 消息
	 * @param formInputTool 表单输入工具
	 * @return 用户输入等待状态
	 */
	UserInputWaitState createUserInputWaitState(String planId, String message, FormInputTool formInputTool);

	/**
	 * 获取等待状态
	 * @param planId 计划ID
	 * @return 用户输入等待状态
	 */
	UserInputWaitState getWaitState(String planId);

	/**
	 * 提交用户输入
	 * @param planId 计划ID
	 * @param inputs 输入数据
	 * @return 是否提交成功
	 */
	boolean submitUserInputs(String planId, Map<String, String> inputs);

}
