package com.alibaba.cloud.ai.tencent.cos;

import com.alibaba.cloud.ai.reader.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static java.lang.String.format;

/**
 * @author HeYQ
 * @since 2024-11-27 21:46
 */
public class TencentCosDocumentReader implements DocumentReader {

	private static final Logger log = LoggerFactory.getLogger(TencentCosDocumentReader.class);

	private DocumentReader parser;

	private TencentCosResource tencentCosResource;

	private List<TencentCosResource> tencentCosResourceList;

	private List<DocumentReader> parserList;

	public TencentCosDocumentReader(TencentCosResource tencentCosResource, DocumentParser parserType) {
		this(tencentCosResource, parserType.getParser(tencentCosResource));

	}

	public TencentCosDocumentReader(TencentCosResource tencentCosResource, DocumentParser parserType,
			ExtractedTextFormatter formatter) {
		this(tencentCosResource, parserType.getParser(tencentCosResource, formatter));
	}

	public TencentCosDocumentReader(TencentCosResource tencentCosResource, DocumentReader parser) {
		this.tencentCosResource = tencentCosResource;
		this.parser = parser;
	}

	public TencentCosDocumentReader(List<TencentCosResource> tencentCosResourceList, DocumentParser parserType,
			ExtractedTextFormatter formatter) {
		this.tencentCosResourceList = tencentCosResourceList;
		List<DocumentReader> parserList = new ArrayList<>();
		for (TencentCosResource tencentCosResource : tencentCosResourceList) {
			parserList.add(parserType.getParser(tencentCosResource, formatter));
		}
		this.parserList = parserList;
	}

	public TencentCosDocumentReader(List<TencentCosResource> tencentCosResourceList, DocumentParser parserType) {
		this.tencentCosResourceList = tencentCosResourceList;
		List<DocumentReader> parserList = new ArrayList<>();
		for (TencentCosResource tencentCosResource : tencentCosResourceList) {
			parserList.add(parserType.getParser(tencentCosResource));
		}
		this.parserList = parserList;
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		if (!Objects.isNull(tencentCosResourceList) && !tencentCosResourceList.isEmpty()) {
			processResourceList(documents);
		}
		else if (tencentCosResource != null) {
			processSingleResource(documents);
		}

		return documents;
	}

	private void processResourceList(List<Document> documents) {
		for (int i = 0; i < tencentCosResourceList.size(); i++) {
			TencentCosResource resource = tencentCosResourceList.get(i);
			String key = resource.getKey();
			String bucket = resource.getBucket();
			String source = format("cos://%s/%s", bucket, key);

			try {
				List<Document> document = parserList.get(i).get();
				for (Document doc : document) {
					doc.getMetadata().put(TencentCosResource.SOURCE, source);
				}
				documents.addAll(document);
			}
			catch (Exception e) {
				log.warn("Failed to load an object with key '{}' from bucket '{}', skipping it. Stack trace: {}", key,
						bucket, e.getMessage(), e);
			}
		}
	}

	private void processSingleResource(List<Document> documents) {
		String key = tencentCosResource.getKey();
		String bucket = tencentCosResource.getBucket();
		String source = format("cos://%s/%s", bucket, key);

		try {
			List<Document> document = parser.get();
			for (Document doc : document) {
				doc.getMetadata().put(TencentCosResource.SOURCE, source);
			}
			documents.addAll(document);
		}
		catch (Exception e) {
			log.warn("Failed to load an object with key '{}' from bucket '{}', skipping it. Stack trace: {}", key,
					bucket, e.getMessage(), e);
		}
	}

}
