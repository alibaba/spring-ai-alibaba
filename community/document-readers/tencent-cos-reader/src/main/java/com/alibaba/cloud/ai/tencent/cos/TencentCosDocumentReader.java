package com.alibaba.cloud.ai.tencent.cos;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

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

	private final DocumentParser parser;

	private TencentCosResource tencentCosResource;

	private List<TencentCosResource> tencentCosResourceList;

	public TencentCosDocumentReader(TencentCosResource tencentCosResource, DocumentParser parser) {
		this.tencentCosResource = tencentCosResource;
		this.parser = parser;
	}

	public TencentCosDocumentReader(List<TencentCosResource> tencentCosResourceList, DocumentParser parser) {
		this.tencentCosResourceList = tencentCosResourceList;
		this.parser = parser;
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		if (!Objects.isNull(tencentCosResourceList) && !tencentCosResourceList.isEmpty()) {
			processResourceList(documents);
		}
		else if (tencentCosResource != null) {
			loadDocuments(documents, tencentCosResource);
		}

		return documents;
	}

	private void processResourceList(List<Document> documents) {
		for (TencentCosResource resource : tencentCosResourceList) {
			loadDocuments(documents, resource);
		}
	}

	private void loadDocuments(List<Document> documents, TencentCosResource resource) {
		String key = resource.getKey();
		String bucket = resource.getBucket();
		String source = format("cos://%s/%s", bucket, key);
		try {
			List<Document> documentList = parser.parse(resource.getInputStream());
			for (Document document : documentList) {
				document.getMetadata().put(TencentCosResource.SOURCE, source);
				documents.add(document);
			}
		}
		catch (Exception e) {
			log.warn("Failed to load an object with key '{}' from bucket '{}', skipping it. Stack trace: {}", key,
					bucket, e.getMessage(), e);
		}
	}

}
