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
package com.alibaba.cloud.ai.vectorstore.tablestore;

import com.aliyun.openservices.tablestore.agent.model.Document;
import com.aliyun.openservices.tablestore.agent.model.DocumentHit;
import com.aliyun.openservices.tablestore.agent.model.Metadata;

import java.util.Map;

class Utils {

	static Document toTablestoreDocument(boolean enableMultiTenant, float[] embedding,
			org.springframework.ai.document.Document springAiDocument) {
		if (springAiDocument.getMedia() != null) {
			throw new UnsupportedOperationException("Media is not supported yet.");
		}
		Map<String, Object> springMetadata = springAiDocument.getMetadata();
		String documentId = springAiDocument.getId();
		String tenantId = Document.DOCUMENT_DEFAULT_TENANT_ID;
		if (enableMultiTenant) {
			Object springTenantId = springMetadata.remove(Document.DOCUMENT_TENANT_ID);
			if (!(springTenantId instanceof String)) {
				throw new IllegalArgumentException(
						"Multi-tenant is enabled but `tenantId` is not set in the document metadata.");
			}
			tenantId = springTenantId.toString();
		}
		String text = springAiDocument.getText();
		Metadata metadata = new Metadata(springMetadata);
		return new Document(documentId, tenantId, text, embedding, metadata);
	}

	static org.springframework.ai.document.Document toSpringAIDocument(DocumentHit documentHit) {
		Document tsDocument = documentHit.getDocument();
		Double score = documentHit.getScore();
		Map<String, Object> metaData = tsDocument.getMetadata().toMap();
		String tenantId = tsDocument.getTenantId();
		if (tenantId != null && !tenantId.equals(Document.DOCUMENT_DEFAULT_TENANT_ID)) {
			metaData.put(Document.DOCUMENT_TENANT_ID, tenantId);
		}
		return org.springframework.ai.document.Document.builder()
			.id(tsDocument.getDocumentId())
			.text(tsDocument.getText())
			.metadata(metaData)
			.score(score)
			.build();

	}

}
