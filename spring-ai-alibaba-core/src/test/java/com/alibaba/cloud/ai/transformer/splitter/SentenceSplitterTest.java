/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.transformer.splitter;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Title SentenceSplitter test cases.<br/>
 * Description SentenceSplitter test cases.<br/>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

@SpringBootTest(classes = SentenceSplitterTest.class)
class SentenceSplitterTest {

	private static final Logger logger = LoggerFactory.getLogger(SentenceSplitterTest.class);

	@Value("classpath:data/acme/intro.txt")
	private Resource resource;

	@Test
	void splitText() throws IOException {
		SentenceSplitter sentenceSplitter = new SentenceSplitter(128);
		List<String> chunks = sentenceSplitter.splitText(resource.getContentAsString(StandardCharsets.UTF_8));

		for (int i = 0; i < chunks.size(); i++) {
			logger.info("================ index: {} ====================", i);
			logger.info(chunks.get(i));
		}
	}

}