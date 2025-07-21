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

package com.alibaba.cloud.ai.example.manus.tool.database.action;

import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.database.DataSourceService;
import com.alibaba.cloud.ai.example.manus.tool.database.DatabaseRequest;

public abstract class AbstractDatabaseAction {

	/**
	 * 执行数据库操作
	 * @param request 请求参数
	 * @param dataSourceService 数据源服务
	 * @return 执行结果
	 */
	public abstract ToolExecuteResult execute(DatabaseRequest request, DataSourceService dataSourceService);

}
