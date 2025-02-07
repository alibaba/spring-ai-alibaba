package com.alibaba.cloud.ai.parser.multi;

import org.junit.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @author HeYQ
 * @since 2025-02-07 14:33
 */
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class ImageDashScopeParserTest {

	// "D:/code/paper_impl_and_git_code/ai_framework/spring-ai-alibaba/community/document-parsers/document-parser-multi-modality/src/test/resources/biaozhun.jpg"
	@Test
	public void testImageDashScopeParser() {
		try {
			ImageDashScopeParser imageDashScopeParser = new ImageDashScopeParser(System.getenv("DASHSCOPE_API_KEY"));
			List<Document> documents = imageDashScopeParser.parse(
					"https://help-static-aliyun-doc.aliyuncs.com/file-manage-files/zh-CN/20241108/ctdzex/biaozhun.jpg");
			System.out.println(documents.get(0).getText());
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
		}
		System.exit(0);
	}

}
