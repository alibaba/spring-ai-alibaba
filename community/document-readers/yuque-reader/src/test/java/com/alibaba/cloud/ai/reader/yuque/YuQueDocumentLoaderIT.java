package com.alibaba.cloud.ai.reader.yuque;

import com.alibaba.cloud.ai.reader.DocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.Document;

import java.util.List;

class YuQueDocumentLoaderIT {

	private static final String YU_QUE_TOKEN = "yqdi4ePciJnvQ3SWofp32h5e2bbW05iN5uVXrJvP";

	private static final String RESOURCE_PATH = "https://cfpamf.yuque.com/qualitycenter/target/xooysu8n0a8dikou";

	YuQueDocumentReader reader;

	YuQueResource source = YuQueResource.builder()
			.yuQueToken(YU_QUE_TOKEN)
			.resourcePath(RESOURCE_PATH)
			.build();

	@BeforeEach
	public void beforeEach() {
		reader = new YuQueDocumentReader(source, DocumentParser.HTML_PARSER);
	}

	@Test
	public void should_load_file() {
		List<Document> document = reader.get();
		String content = document.get(0).getContent();

		System.out.println(content);
	}

}
