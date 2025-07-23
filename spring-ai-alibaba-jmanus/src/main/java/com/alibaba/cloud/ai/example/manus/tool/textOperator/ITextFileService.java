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
package com.alibaba.cloud.ai.example.manus.tool.textOperator;

import java.io.IOException;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.innerStorage.SmartContentSavingService;

/**
 * 文本文件服务接口，提供文件操作管理功能
 */
public interface ITextFileService {

	/**
	 * 获取内部存储服务
	 * @return 内部存储服务
	 */
	SmartContentSavingService getInnerStorageService();

	/**
	 * 获取指定计划的文件状态
	 * @param planId 计划ID
	 * @return 文件状态
	 */
	Object getFileState(String planId);

	/**
	 * 关闭指定计划的文件
	 * @param planId 计划ID
	 */
	void closeFileForPlan(String planId);

	/**
	 * 检查是否支持的文件类型
	 * @param filePath 文件路径
	 * @return 是否支持
	 */
	boolean isSupportedFileType(String filePath);

	/**
	 * 获取文件扩展名
	 * @param filePath 文件路径
	 * @return 文件扩展名
	 */
	String getFileExtension(String filePath);

	/**
	 * 验证并获取绝对路径
	 * @param workingDirectoryPath 工作目录路径
	 * @param filePath 文件路径
	 * @throws IOException IO异常
	 */
	void validateAndGetAbsolutePath(String workingDirectoryPath, String filePath) throws IOException;

	/**
	 * 更新文件状态
	 * @param planId 计划ID
	 * @param filePath 文件路径
	 * @param operationResult 操作结果
	 */
	void updateFileState(String planId, String filePath, String operationResult);

	/**
	 * 获取当前文件路径
	 * @param planId 计划ID
	 * @return 当前文件路径
	 */
	String getCurrentFilePath(String planId);

	/**
	 * 获取Manus属性
	 * @return Manus属性
	 */
	ManusProperties getManusProperties();

	/**
	 * 获取最后操作结果
	 * @param planId 计划ID
	 * @return 最后操作结果
	 */
	String getLastOperationResult(String planId);

	/**
	 * 清理资源
	 */
	void cleanup();

}
