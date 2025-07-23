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
package com.alibaba.cloud.ai.example.manus.tool.innerStorage;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;

/**
 * 智能内容保存服务接口，用于MapReduce流程中存储中间数据
 */
public interface ISmartContentSavingService {

	/**
	 * 获取Manus属性
	 * @return Manus属性
	 */
	ManusProperties getManusProperties();

	/**
	 * 处理内容，如果内容过长则自动存储
	 * @param planId 计划ID
	 * @param content 内容
	 * @param callingMethod 调用的方法名
	 * @return 处理结果，包含文件名和摘要
	 */
	SmartContentSavingService.SmartProcessResult processContent(String planId, String content, String callingMethod);

}
