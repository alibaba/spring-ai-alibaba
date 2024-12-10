package com.alibaba.cloud.ai.reader.yuque;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author YunLong
 */
public class YuQueDocumentReader implements DocumentReader {

	private final DocumentParser parser;

	private final YuQueResource yuQueResource;

	public YuQueDocumentReader(YuQueResource yuQueResource, DocumentParser parser) {
		this.yuQueResource = yuQueResource;
		this.parser = parser;
	}

	@Override
	public List<Document> get() {
		try {
			List<Document> documents = parser.parse(yuQueResource.getInputStream());
			String source = yuQueResource.getResourcePath();

			for (Document doc : documents) {
				doc.getMetadata().put(YuQueResource.SOURCE, source);
			}

			return documents;
		}
		catch (IOException ioException) {
			throw new RuntimeException("Failed to load document from yuque: {}", ioException);
		}
	}

}
