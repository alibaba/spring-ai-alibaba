
package com.alibaba.cloud.ai.service.milvus;

import com.alibaba.cloud.ai.annotation.ConditionalOnMilvusEnabled;
import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnMilvusEnabled
public class MilvusNl2SqlService extends BaseNl2SqlService {

	// TODO dbAccessor 的属性注入以后要动态选择，暂时用mysqlAccessor代替
	@Autowired
	public MilvusNl2SqlService(@Qualifier("milvusVectorStoreService") BaseVectorStoreService vectorStoreService,
			@Qualifier("milvusSchemaService") BaseSchemaService schemaService, LlmService aiService,
			@Qualifier("mysqlAccessor") Accessor dbAccessor, DbConfig dbConfig) {
		super(vectorStoreService, schemaService, aiService, dbAccessor, dbConfig);
	}

}
