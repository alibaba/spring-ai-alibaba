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
package com.alibaba.cloud.ai.reader.feishu;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

public class FeiShuDocumentReaderTest {

	private static final Logger log = LoggerFactory.getLogger(FeiShuDocumentReaderTest.class);

	private FeiShuDocumentReader feiShuDocumentReader;

	private FeiShuResource feiShuResource;

	void create() {
		feiShuResource = FeiShuResource.builder()
			.appId("cli_a7c6b258ae3c9013")
			.appSecret("KeifrxEeKvGHXJKxkOFRrfteovAOHwFy")
			.build();
	}

	@Test
	void feiShuDocumentTest() {
		create();
		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("result:{}", documentList);
	}

	@Test
	void feiShuDocumentTestByUserToken() {
		create();
		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource,
				"u-esTKL7nYJ0Sa60TNcQflx9h41.6wk4lFgG00llS2w4oy");
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("result:{}", documentList);
	}

	@Test
	void feiShuDocumentTestByUserTokenAndDocumentId() {
		create();
		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource,
				"u-esTKL7nYJ0Sa60TNcQflx9h41.6wk4lFgG00llS2w4oy", "QdVwdxUKaoVuk5xGe34cm8PonBf");
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("result:{}", documentList);
	}

}
