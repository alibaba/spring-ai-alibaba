package com.alibaba.cloud.ai.vectorstore.opensearch;

import com.aliyun.ha3engine.vector.Client;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

/**
 * @author 北极星
 */
@AutoConfiguration
@ConditionalOnClass({EmbeddingModel.class, Client.class, OpenSearchVectorStore.class})
@EnableConfigurationProperties({OpenSearchApi.class})
@ConditionalOnProperty(prefix = "spring.ai.vectorstore.opensearch", havingValue = "true")
public class OpenSearchDbVectorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BatchingStrategy.class)
    BatchingStrategy batchingStrategy () {
        return new TokenCountBatchingStrategy();
    }

    @Bean
    @ConditionalOnMissingBean
    @DependsOn({"embeddingModel", "batchingStrategy", "openSearchApi"})
    public OpenSearchVectorStore vectorStore (String instanceId, String endpoint,
                                              String accessUserName, String accessPassWord,
                                              EmbeddingModel embeddingModel) {
        OpenSearchApi openSearchApi = new OpenSearchApi(instanceId, endpoint, accessUserName,
                accessPassWord);
        return OpenSearchVectorStore.builder(openSearchApi, embeddingModel)
                .build();
    }
}
