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

package com.alibaba.cloud.ai.example.graph.bigtool.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;

import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VectorStoreService {

	private final EmbeddingModel embeddingModel;

	private final VectorStore vectorStore;

	public VectorStoreService(EmbeddingModel embeddingModel) {
		this.embeddingModel = embeddingModel;
		this.vectorStore = SimpleVectorStore.builder(embeddingModel).build();
	}

	public void addDocuments(List<Document> documents) {
		vectorStore.add(documents);
	}

	public List<Document> search(String query, int topK) {
		return vectorStore.similaritySearch(SearchRequest.builder().query(query).topK(topK).build());
	}

}
