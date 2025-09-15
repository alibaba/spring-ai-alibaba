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

package com.alibaba.cloud.ai.example.deepresearch.service;

/**
 * 报告服务接口
 *
 * @author huangzhen
 * @since 2025/6/20
 */
public interface ReportService {

	/**
	 * 存储报告
	 * @param threadId 线程ID
	 * @param report 报告内容
	 */
	void saveReport(String threadId, String report);

	/**
	 * 获取报告
	 * @param threadId 线程ID
	 * @return 报告内容，如果不存在返回 null
	 */
	String getReport(String threadId);

	/**
	 * 检查报告是否存在
	 * @param threadId 线程ID
	 * @return 是否存在
	 */
	boolean existsReport(String threadId);

	/**
	 * 删除报告
	 * @param threadId 线程ID
	 */
	void deleteReport(String threadId);

}
