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
			SttDashScopeParser sttDashScopeParser = new SttDashScopeParser(System.getenv("DASHSCOPE_API_KEY"));
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
