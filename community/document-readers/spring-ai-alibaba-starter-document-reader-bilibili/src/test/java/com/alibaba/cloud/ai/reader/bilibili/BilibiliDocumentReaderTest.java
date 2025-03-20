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
package com.alibaba.cloud.ai.reader.bilibili;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @Author: XiaoYunTao
 * @Date: 2025/1/18
 */
public class BilibiliDocumentReaderTest {

	private static final Logger logger = LoggerFactory.getLogger(BilibiliDocumentReader.class);

	@Test
	void bilibiliDocumentReaderTest() {
		BilibiliDocumentReader bilibiliDocumentReader = new BilibiliDocumentReader(
				"https://www.bilibili.com/video/BV1KMwgeKECx/?t=7&vd_source=3069f51b168ac07a9e3c4ba94ae26af5");
		List<Document> documents = bilibiliDocumentReader.get();
		logger.info("documents: {}", documents);
	}

}
