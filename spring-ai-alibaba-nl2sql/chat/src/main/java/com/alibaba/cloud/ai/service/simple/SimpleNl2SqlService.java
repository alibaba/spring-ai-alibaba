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
package com.alibaba.cloud.ai.service.simple;

import com.alibaba.cloud.ai.dbconnector.*;
import com.alibaba.cloud.ai.dbconnector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.dbconnector.bo.ResultSetBO;
import com.alibaba.cloud.ai.prompt.PromptHelper;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.alibaba.cloud.ai.util.DateTimeUtil;
import com.alibaba.cloud.ai.util.MarkdownParser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.alibaba.cloud.ai.prompt.PromptHelper.buildMixSelectorPrompt;

@Service
public class SimpleNl2SqlService extends BaseNl2SqlService {

	@Autowired
	public SimpleNl2SqlService(@Qualifier("simpleVectorStoreService") BaseVectorStoreService vectorStoreService,
			@Qualifier("simpleSchemaService") BaseSchemaService schemaService, LlmService aiService,
			DbAccessor dbAccessor, DbConfig dbConfig) {
		super(vectorStoreService, schemaService, aiService, dbAccessor, dbConfig);
	}

	@PostConstruct
	public void init() throws Exception {
		MysqlJdbcDdl mysqlJdbcDdl = new MysqlJdbcDdl();
		DdlFactory.registry(mysqlJdbcDdl);
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest.setTables(Arrays.asList("xhs_note"));
		vectorStoreService.schema(schemaInitRequest);
		String s = nl2sql("查找来涉及到大连旅游的文章，取最新的三十篇文章");
		System.out.println(s);
	}

}
