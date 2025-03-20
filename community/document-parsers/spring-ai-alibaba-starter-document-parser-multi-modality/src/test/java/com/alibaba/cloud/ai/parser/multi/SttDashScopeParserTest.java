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

package com.alibaba.cloud.ai.parser.multi;

import org.junit.Test;
import org.springframework.ai.document.Document;
import java.util.List;

/**
 * @author HeYQ
 * @since 2025-02-07 11:54
 */

public class SttDashScopeParserTest {

	// "D:/code/paper_impl_and_git_code/ai_framework/spring-ai-alibaba/community/document-parsers/document-parser-multi-modality/src/test/resources/count.pcm"
	// D:/code/paper_impl_and_git_code/ai_framework/spring-ai-alibaba/community/document-parsers/document-parser-multi-modality/src/test/resources/hello_world_female2.wav
	// https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3
	@Test
	public void testSttDashScope() {
		try {
			SttDashScopeParser sttDashScopeParser = new SttDashScopeParser(System.getenv("AI_DASHSCOPE_API_KEY"));
			List<Document> documents = sttDashScopeParser
				.parse("https://dashscope.oss-cn-beijing.aliyuncs.com/audios/welcome.mp3");
			System.out.println(documents.get(0).getText());
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.exit(0);
	}

}
