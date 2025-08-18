
package com.alibaba.cloud.ai.service.milvus;

import com.alibaba.cloud.ai.annotation.ConditionalOnMilvusEnabled;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.request.SearchRequest;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.google.gson.Gson;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnMilvusEnabled
public class MilvusSchemaService extends BaseSchemaService {

	@Autowired
	public MilvusSchemaService(DbConfig dbConfig, Gson gson,
			@Qualifier("milvusVectorStoreService") BaseVectorStoreService vectorStoreService) {
		super(dbConfig, gson, vectorStoreService);
	}

	@Override
	protected void addTableDocument(List<Document> tableDocuments, String tableName, String vectorType) {
		handleDocumentQuery(tableDocuments, tableName, vectorType, name -> {
			SearchRequest req = new SearchRequest();
			req.setFilterFormatted("name == '" + name + "' and vectorType == '" + vectorType + "'");
			return req;
		}, vectorStoreService::searchWithFilter);
	}

	@Override
	protected void addColumnsDocument(Map<String, Document> weightedColumns, String columnName, String vectorType) {
		handleDocumentQuery(weightedColumns, columnName, vectorType, name -> {
			SearchRequest req = new SearchRequest();
			req.setFilterFormatted("name == '" + name + "' and vectorType == '" + vectorType + "'");
			return req;
		}, vectorStoreService::searchWithFilter);
	}

}
