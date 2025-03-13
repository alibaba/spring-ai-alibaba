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
package com.alibaba.cloud.ai.reader.mongodb.converter;

import com.alibaba.cloud.ai.reader.mongodb.MongodbResource;
import org.springframework.ai.document.Document;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Yongtao Tan
 * @version 1.0.0
 */
public class DefaultDocumentConverter implements DocumentConverter {

	@Override
	public Document convert(org.bson.Document mongoDocument, String database, String collection,
			MongodbResource properties) {
		Map<String, Object> metadata = new HashMap<>();
		metadata.put("source", "mongodb");
		metadata.put("collection", collection);
		metadata.put("database", database);
		metadata.put("id", mongoDocument.getObjectId("_id").toString());

		// Add vectorization support
		if (properties.isEnableVectorization() && mongoDocument.containsKey(properties.getVectorField())) {
			metadata.put("vector", mongoDocument.get(properties.getVectorField()));
		}

		// Convert MongoDB document to string, excluding specific fields
		org.bson.Document contentDoc = new org.bson.Document(mongoDocument);
		contentDoc.remove("_id");
		contentDoc.remove(properties.getVectorField());
		String content = contentDoc.toJson();

		return new org.springframework.ai.document.Document(content, metadata);
	}

}
