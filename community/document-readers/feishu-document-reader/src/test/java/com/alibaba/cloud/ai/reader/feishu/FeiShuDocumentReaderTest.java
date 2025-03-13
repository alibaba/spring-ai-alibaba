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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * Test class for FeiShuDocumentReader. Tests will be skipped if FEISHU_APP_ID and
 * FEISHU_APP_SECRET environment variables are not set.
 */
@EnabledIfEnvironmentVariable(named = "FEISHU_APP_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "FEISHU_APP_SECRET", matches = ".+")
public class FeiShuDocumentReaderTest {

	private static final Logger log = LoggerFactory.getLogger(FeiShuDocumentReaderTest.class);

	// Get configuration from environment variables
	private static final String FEISHU_APP_ID = System.getenv("FEISHU_APP_ID");

	private static final String FEISHU_APP_SECRET = System.getenv("FEISHU_APP_SECRET");

	// Optional user token and document ID from environment variables
	private static final String FEISHU_USER_TOKEN = System.getenv("FEISHU_USER_TOKEN");

	private static final String FEISHU_DOCUMENT_ID = System.getenv("FEISHU_DOCUMENT_ID");

	private FeiShuDocumentReader feiShuDocumentReader;

	private FeiShuResource feiShuResource;

	static {
		if (FEISHU_APP_ID == null || FEISHU_APP_SECRET == null) {
			System.out
				.println("FEISHU_APP_ID or FEISHU_APP_SECRET environment variable is not set. Tests will be skipped.");
		}
	}

	@BeforeEach
	void setup() {
		// Skip test if environment variables are not set
		Assumptions.assumeTrue(FEISHU_APP_ID != null && !FEISHU_APP_ID.isEmpty(),
				"Skipping test because FEISHU_APP_ID is not set");
		Assumptions.assumeTrue(FEISHU_APP_SECRET != null && !FEISHU_APP_SECRET.isEmpty(),
				"Skipping test because FEISHU_APP_SECRET is not set");

		// Create FeiShuResource with environment variables
		feiShuResource = FeiShuResource.builder().appId(FEISHU_APP_ID).appSecret(FEISHU_APP_SECRET).build();
	}

	@Test
	void feiShuDocumentTest() {
		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("result:{}", documentList);
	}

	@Test
	void feiShuDocumentTestByUserToken() {
		// Skip test if user token is not set
		Assumptions.assumeTrue(FEISHU_USER_TOKEN != null && !FEISHU_USER_TOKEN.isEmpty(),
				"Skipping test because FEISHU_USER_TOKEN is not set");

		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, FEISHU_USER_TOKEN);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("result:{}", documentList);
	}

	@Test
	void feiShuDocumentTestByUserTokenAndDocumentId() {
		// Skip test if user token or document ID is not set
		Assumptions.assumeTrue(FEISHU_USER_TOKEN != null && !FEISHU_USER_TOKEN.isEmpty(),
				"Skipping test because FEISHU_USER_TOKEN is not set");
		Assumptions.assumeTrue(FEISHU_DOCUMENT_ID != null && !FEISHU_DOCUMENT_ID.isEmpty(),
				"Skipping test because FEISHU_DOCUMENT_ID is not set");

		feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, FEISHU_USER_TOKEN, FEISHU_DOCUMENT_ID);
		List<Document> documentList = feiShuDocumentReader.get();
		log.info("result:{}", documentList);
	}

}
