package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.dbconnector.DbAccessor;
import com.alibaba.cloud.ai.dbconnector.DbConfig;
import com.alibaba.cloud.ai.service.LlmService;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.base.BaseVectorStoreService;
import com.alibaba.cloud.ai.service.simple.SimpleNl2SqlService;
import com.alibaba.cloud.ai.service.simple.SimpleSchemaService;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author zhangshenghang
 */
@Configuration
public class BaseDefaultConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(Nl2sqlConfiguration.class);

    @Autowired
    private DbAccessor dbAccessor;

    @Autowired
    private DbConfig dbConfig;


    @Bean("nl2SqlServiceImpl")
    @ConditionalOnMissingBean(name = "nl2SqlServiceImpl")
    public BaseNl2SqlService defaultNl2SqlService(
            @Qualifier("simpleVectorStoreService") BaseVectorStoreService vectorStoreService,
            @Qualifier("simpleSchemaService") BaseSchemaService schemaService,
            LlmService aiService) {
        logger.info("Creating default BaseNl2SqlService implementation");
        return new SimpleNl2SqlService(vectorStoreService, schemaService, aiService, dbAccessor, dbConfig);
    }

    @Bean("schemaServiceImpl")
    @ConditionalOnMissingBean(name = "schemaServiceImpl")
    public BaseSchemaService defaultSchemaService(
            @Qualifier("simpleVectorStoreService") BaseVectorStoreService vectorStoreService, DbConfig dbConfig, Gson gson) {
        logger.info("Creating default BaseSchemaService implementation");
        return new SimpleSchemaService(dbConfig,gson,vectorStoreService);
    }
}


