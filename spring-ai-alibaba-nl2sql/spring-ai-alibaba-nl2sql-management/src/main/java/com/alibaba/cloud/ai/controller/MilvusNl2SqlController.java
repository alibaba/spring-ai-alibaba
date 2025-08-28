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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.annotation.ConditionalOnMilvusEnabled;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import com.alibaba.cloud.ai.service.MilvusVectorStoreManagementService;
import com.alibaba.cloud.ai.service.milvus.MilvusNl2SqlService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@ConditionalOnMilvusEnabled
public class MilvusNl2SqlController {

	private final MilvusNl2SqlService nl2SqlService;

	private final MilvusVectorStoreManagementService milvusVectorStoreManagementService;



	private final DbConfig dbConfig;

	public MilvusNl2SqlController(MilvusNl2SqlService nl2SqlService,
			MilvusVectorStoreManagementService milvusVectorStoreManagementService, DbConfig dbConfig) {
		this.nl2SqlService = nl2SqlService;
		this.milvusVectorStoreManagementService = milvusVectorStoreManagementService;
		this.dbConfig = dbConfig;
	}

	@PostMapping("/milvus-chat")
	public String nl2Sql(@RequestBody String input) throws Exception {
		return nl2SqlService.nl2sql(input);
	}

	@PostMapping("/milvus-init-schema")
	public String initSchema() throws Exception {
		SchemaInitRequest schemaInitRequest = new SchemaInitRequest();
		schemaInitRequest.setDbConfig(dbConfig);
		schemaInitRequest.setTables(Arrays.asList("class", "student"));
		Boolean schema = milvusVectorStoreManagementService.schema(schemaInitRequest);
		if (schema) {
			return "success";
		}
		return "fail";
	}

}
