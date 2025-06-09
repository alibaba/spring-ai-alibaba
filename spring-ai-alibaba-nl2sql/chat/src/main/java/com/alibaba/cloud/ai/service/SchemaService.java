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

import com.alibaba.cloud.ai.dbconnector.BizDataSourceTypeEnum;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.schema.ColumnDTO;
import com.alibaba.cloud.ai.schema.SchemaDTO;
import com.alibaba.cloud.ai.schema.TableDTO;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Schema 构建服务，支持基于 RAG 的混合查询。
 */
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@Service
public class SchemaService extends BaseSchemaService {

	@Autowired
	public SchemaService(DbConfig dbConfig, Gson gson,
			@Qualifier("vectorStoreService") BaseVectorStoreService vectorStoreService) {
		super(dbConfig, gson);
		setVectorStoreService(vectorStoreService);
	}

}
