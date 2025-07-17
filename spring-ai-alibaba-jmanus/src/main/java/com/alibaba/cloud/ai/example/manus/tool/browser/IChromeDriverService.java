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
package com.alibaba.cloud.ai.example.manus.tool.browser;

import com.alibaba.cloud.ai.example.manus.config.IManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;
import com.alibaba.cloud.ai.example.manus.tool.filesystem.UnifiedDirectoryManager;

/**
 * Chrome驱动服务接口，提供浏览器驱动管理功能
 */
public interface IChromeDriverService {

	/**
	 * 获取共享目录
	 * @return 共享目录路径
	 */
	String getSharedDir();

	/**
	 * 将所有驱动的cookies保存到全局共享目录
	 */
	void saveCookiesToSharedDir();

	/**
	 * 从全局共享目录加载cookies到所有驱动
	 */
	void loadCookiesFromSharedDir();

	/**
	 * 获取指定计划ID的驱动包装器
	 * @param planId 计划ID
	 * @return 驱动包装器
	 */
	DriverWrapper getDriver(String planId);

	/**
	 * 关闭指定计划的驱动
	 * @param planId 计划ID
	 */
	void closeDriverForPlan(String planId);

	/**
	 * 清理所有资源
	 */
	void cleanup();

	/**
	 * 设置Manus属性
	 * @param manusProperties Manus属性
	 */
	void setManusProperties(IManusProperties manusProperties);

	/**
	 * 获取Manus属性
	 * @return Manus属性
	 */
	IManusProperties getManusProperties();

	/**
	 * 获取内部存储服务
	 * @return 内部存储服务
	 */
	SmartContentSavingService getInnerStorageService();

	/**
	 * 获取统一目录管理器
	 * @return 统一目录管理器
	 */
	UnifiedDirectoryManager getUnifiedDirectoryManager();

}
