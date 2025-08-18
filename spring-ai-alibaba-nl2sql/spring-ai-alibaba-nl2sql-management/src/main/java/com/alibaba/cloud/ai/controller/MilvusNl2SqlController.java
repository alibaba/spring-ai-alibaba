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

	;

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
