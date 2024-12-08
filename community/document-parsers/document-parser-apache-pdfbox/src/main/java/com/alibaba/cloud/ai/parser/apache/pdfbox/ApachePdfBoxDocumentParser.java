package com.alibaba.cloud.ai.parser.apache.pdfbox;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author HeYQ
 * @since 2024-12-08 22:34
 */

public class ApachePdfBoxDocumentParser implements DocumentParser {

	private final boolean includeMetadata;

	public ApachePdfBoxDocumentParser() {
		this(false);
	}

	public ApachePdfBoxDocumentParser(boolean includeMetadata) {
		this.includeMetadata = includeMetadata;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try (PDDocument pdfDocument = PDDocument.load(inputStream)) {
			PDFTextStripper stripper = new PDFTextStripper();
			String text = stripper.getText(pdfDocument);
			Assert.notNull(text, "Text cannot be null");
			return includeMetadata ? Collections.singletonList(new Document(text, toMetadata(pdfDocument)))
					: Collections.singletonList(new Document(text));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Map<String, Object> toMetadata(PDDocument pdDocument) {
		PDDocumentInformation documentInformation = pdDocument.getDocumentInformation();
		Map<String, Object> metadata = new HashMap<>();
		for (String metadataKey : documentInformation.getMetadataKeys()) {
			String value = documentInformation.getCustomMetadataValue(metadataKey);
			if (value != null) {
				metadata.put(metadataKey, value);
			}
		}
		return metadata;
	}

}
