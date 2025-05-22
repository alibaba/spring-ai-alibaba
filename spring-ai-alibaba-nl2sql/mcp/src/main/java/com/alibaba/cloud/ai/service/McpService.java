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
package com.alibaba.cloud.ai.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McpService {

	@Autowired
	private Nl2SqlService nl2SqlService;

	/**
	 * 从数据库中获取问题所需要的数据
	 * @return 从数据库中获取问题所需要的数据
	 */
	@Tool(description = "从数据库中获取问题所需要的数据")
	public String nl2Sql(String input) throws Exception {
		String sql = nl2SqlService.nl2sql(input);
		return nl2SqlService.executeSql(sql);
	}

}
