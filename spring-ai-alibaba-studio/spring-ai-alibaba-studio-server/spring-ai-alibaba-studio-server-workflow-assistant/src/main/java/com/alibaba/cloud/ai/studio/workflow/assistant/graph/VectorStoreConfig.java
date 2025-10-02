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

package com.alibaba.cloud.ai.studio.workflow.assistant.graph;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

@Configuration
class VectorStoreConfig {

	/**
	 * Define a default VectorStore bean so that the KnowledgeRetrievalNode can get it.
	 * Following Single Responsibility Principle - this configuration class is solely responsible for VectorStore setup.
	 */

	@Value("classpath:data/manual.txt")
	private Resource ragSource;

	@Bean
	@Primary
	public VectorStore customVectorStore(EmbeddingModel embeddingModel) {

		var chunks = new TokenTextSplitter().transform(new TextReader(ragSource).read());

		SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

		vectorStore.write(chunks);
		return vectorStore;
	}

}
