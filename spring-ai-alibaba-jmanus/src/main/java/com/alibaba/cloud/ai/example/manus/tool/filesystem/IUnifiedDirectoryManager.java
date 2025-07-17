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
package com.alibaba.cloud.ai.example.manus.tool.filesystem;

import java.io.IOException;
import java.nio.file.Path;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;

/**
 * 统一目录管理器接口，为所有工具的文件系统操作提供集中的目录管理
 */
public interface IUnifiedDirectoryManager {

	/**
	 * 获取工作目录路径
	 * @return 工作目录路径字符串
	 */
	String getWorkingDirectoryPath();

	/**
	 * 获取工作目录
	 * @return 工作目录Path对象
	 */
	Path getWorkingDirectory();

	/**
	 * 获取根计划目录
	 * @param rootPlanId 根计划ID
	 * @return 根计划目录Path
	 */
	Path getRootPlanDirectory(String rootPlanId);

	/**
	 * 获取子任务目录
	 * @param rootPlanId 根计划ID
	 * @param subTaskId 子任务ID
	 * @return 子任务目录Path
	 */
	Path getSubTaskDirectory(String rootPlanId, String subTaskId);

	/**
	 * 获取指定目录
	 * @param targetPath 目标路径
	 * @return 指定目录Path
	 * @throws IOException IO异常
	 * @throws SecurityException 安全异常
	 */
	Path getSpecifiedDirectory(String targetPath) throws IOException, SecurityException;

	/**
	 * 确保目录存在
	 * @param directory 目录
	 * @throws IOException IO异常
	 */
	void ensureDirectoryExists(Path directory) throws IOException;

	/**
	 * 检查路径是否被允许
	 * @param targetPath 目标路径
	 * @return 是否允许
	 */
	boolean isPathAllowed(Path targetPath);

	/**
	 * 获取内部存储根目录
	 * @return 内部存储根目录Path
	 */
	Path getInnerStorageRoot();

	/**
	 * 从工作目录获取相对路径
	 * @param absolutePath 绝对路径
	 * @return 相对路径字符串
	 */
	String getRelativePathFromWorkingDirectory(Path absolutePath);

	/**
	 * 获取Manus属性
	 * @return Manus属性
	 */
	ManusProperties getManusProperties();

	/**
	 * 清理子任务目录
	 * @param rootPlanId 根计划ID
	 * @param subTaskId 子任务ID
	 * @throws IOException IO异常
	 */
	void cleanupSubTaskDirectory(String rootPlanId, String subTaskId) throws IOException;

	/**
	 * 清理根计划目录
	 * @param rootPlanId 根计划ID
	 * @throws IOException IO异常
	 */
	void cleanupRootPlanDirectory(String rootPlanId) throws IOException;

}
