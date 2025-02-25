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
package com.alibaba.cloud.ai.reader.mongodb;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.ai.document.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author Yongtao Tan
 * @version 1.0.0
 */
public class MongodbDocumentReaderIT {

	private static MongodbResource resource;

	private static MongodbDocumentReader reader;

	@BeforeEach
	public void beforeEach() {

		resource = new MongodbResource();
		resource.setUri(System.getProperty("mongodb.uri"));
		resource.setUsername(System.getProperty("mongodb.username"));
		resource.setPassword(System.getProperty("mongodb.password"));
		resource.setDatabase(System.getProperty("mongodb.database"));
		resource.setCollection(System.getProperty("mongodb.collection"));
		resource.setPoolSize(10);
		resource.setConnectTimeout(5000);
		resource.setChunkSize(100);

		reader = MongodbDocumentReader.builder().withResource(resource).build();
	}

	@Test
	public void findAll() {
		List<Document> documents = reader.get();
		printDocuments(documents);
	}

	@Test
	public void findByQuery() {
		Query query = new Query(Criteria.where("DEMO").is("Java for Demo"));
		List<Document> queryDocs = reader.findByQuery(query);
		printDocuments(queryDocs);

	}

	@Test
	public void findWithPagination() {
		List<Document> pagedDocs = reader.findWithPagination(new Query(), 0, 1);
		printDocuments(pagedDocs);
	}

	private static void printDocuments(List<Document> documents) {
		if (documents.isEmpty()) {
			System.out.println("No documents found");
			return;
		}

		System.out.println("Found " + documents.size() + " documents:");
		for (Document doc : documents) {
			System.out.println("- " + doc.getContent());
			System.out.println("  Metadata: " + doc.getMetadata());
		}
	}

}
