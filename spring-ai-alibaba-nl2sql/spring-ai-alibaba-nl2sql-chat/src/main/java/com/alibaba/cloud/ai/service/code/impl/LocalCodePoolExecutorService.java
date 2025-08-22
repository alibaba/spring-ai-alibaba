/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.service.code.impl;

import com.alibaba.cloud.ai.config.CodeExecutorProperties;
import com.alibaba.cloud.ai.service.code.CodePoolExecutorService;

/**
 * 在本地运行Python3代码的实现类
 *
 * @author vlsmb
 * @since 2025/8/16
 */
public class LocalCodePoolExecutorService extends AbstractCodePoolExecutorService implements CodePoolExecutorService {

	// 对于本地运行这个实现类，“容器”为临时文件夹
	public LocalCodePoolExecutorService(CodeExecutorProperties properties) {
		super(properties);
	}

	@Override
	protected String createNewContainer() throws Exception {
		return "";
	}

	@Override
	protected TaskResponse execTaskInContainer(TaskRequest request, String containerId) {
		return null;
	}

	@Override
	protected void stopContainer(String containerId) throws Exception {

	}

	@Override
	protected void removeContainer(String containerId) throws Exception {

	}

}
