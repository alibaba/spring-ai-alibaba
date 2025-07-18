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
package com.alibaba.cloud.ai.service.analytic;

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Schema 构建服务，支持基于 RAG 的混合查询。
 */
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.analytic", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@Service
public class AnalyticSchemaService extends BaseSchemaService {

	@Autowired
	public AnalyticSchemaService(DbConfig dbConfig, Gson gson,
			@Qualifier("analyticVectorStoreService") BaseVectorStoreService vectorStoreService) {
		super(dbConfig, gson, vectorStoreService);
	}

	@Override
	protected void addTableDocument(List<Document> tableDocuments, String tableName, String vectorType) {
		handleDocumentQuery(tableDocuments, tableName, vectorType, name -> {
			SearchRequest req = new SearchRequest();
			req.setQuery(null);
			req.setFilterFormatted("jsonb_extract_path_text(metadata, 'vectorType') = '" + vectorType
					+ "' and refdocid = '" + name + "'");
			return req;
		}, vectorStoreService::searchWithFilter);
	}

	@Override
	protected void addColumnsDocument(Map<String, Document> weightedColumns, String columnName, String vectorType) {
		handleDocumentQuery(weightedColumns, columnName, vectorType, name -> {
			SearchRequest req = new SearchRequest();
			req.setQuery(null);
			req.setFilterFormatted("jsonb_extract_path_text(metadata, 'vectorType') = '" + vectorType
					+ "' and refdocid = '" + name + "'");
			return req;
		}, vectorStoreService::searchWithFilter);
	}

}
