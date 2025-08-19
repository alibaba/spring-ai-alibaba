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

package com.alibaba.cloud.ai.studio.runtime.domain.knowledgebase;

import com.alibaba.cloud.ai.studio.runtime.enums.DocumentType;
import com.alibaba.cloud.ai.studio.runtime.domain.file.UploadPolicy;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * Request model for creating a document in the knowledge base.
 *
 * @since 1.0.0.3
 */
@Data
public class CreateDocumentRequest implements Serializable {

	/** ID of the knowledge base */
	@JsonProperty("kb_id")
	private String kbId;

	/** List of files to be uploaded */
	private List<UploadPolicy> files;

	/** Type of the document */
	private DocumentType type;

	/** Configuration for document processing */
	@JsonProperty("process_config")
	private ProcessConfig processConfig;

}
