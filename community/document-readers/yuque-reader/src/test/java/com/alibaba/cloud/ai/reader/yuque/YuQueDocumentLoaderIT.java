package com.alibaba.cloud.ai.reader.yuque;

import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

class YuQueDocumentLoaderIT {

	private static final String YU_QUE_TOKEN = "${token}";

	private static final String RESOURCE_PATH = "${url}";

	YuQueDocumentReader reader;

	YuQueResource source = YuQueResource.builder().yuQueToken(YU_QUE_TOKEN).resourcePath(RESOURCE_PATH).build();

	@BeforeEach
	public void beforeEach() {
		reader = new YuQueDocumentReader(source, new TikaDocumentParser());
	}

	@Test
	public void should_load_file() {
		List<Document> document = reader.get();
		String content = document.get(0).getContent();

		System.out.println(content);
	}

}
