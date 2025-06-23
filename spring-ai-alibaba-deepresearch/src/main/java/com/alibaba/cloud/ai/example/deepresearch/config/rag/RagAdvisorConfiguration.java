/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.config.rag;

import com.alibaba.cloud.ai.example.deepresearch.rag.post.DocumentSelectFirstProcess;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the RAG pipeline by creating the RetrievalAugmentationAdvisor bean.
 *
 * @author hupei
 */
@Configuration
@ConditionalOnProperty(prefix = RagProperties.RAG_PREFIX, name = "enabled", havingValue = "true")
@EnableConfigurationProperties(RagProperties.class)
public class RagAdvisorConfiguration {

	/**
	 * Creates the core RetrievalAugmentationAdvisor by assembling modules based on
	 * configuration.
	 */
	@Bean
	public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore,
			RagProperties ragProperties, ChatClient.Builder chatClientBuilder) {

		var advisorBuilder = RetrievalAugmentationAdvisor.builder()
			.documentRetriever(VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).build());

		// Conditionally add Query Expander
		if (ragProperties.getPipeline().isQueryExpansionEnabled()) {
			advisorBuilder.queryExpander(MultiQueryExpander.builder().chatClientBuilder(chatClientBuilder).build());
		}

		// Conditionally add Query Translator
		if (ragProperties.getPipeline().isQueryTranslationEnabled()) {
			advisorBuilder.queryTransformers(TranslationQueryTransformer.builder()
				.chatClientBuilder(chatClientBuilder)
				.targetLanguage(ragProperties.getPipeline().getQueryTranslationLanguage())
				.build());
		}

		// todo 文档后处理，增加如相关性排序、删除不相关文档，压缩文档等 Conditionally add Post-Processor
		if (ragProperties.getPipeline().isPostProcessingSelectFirstEnabled()) {
			advisorBuilder.documentPostProcessors(new DocumentSelectFirstProcess());
		}

		return advisorBuilder.build();
	}

}
