package com.alibaba.cloud.ai.example.deepresearch.controller.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.postretrieval.document.DocumentPostProcessor;
import java.util.Collections;
import java.util.List;

/**
 * A simple DocumentPostProcessor that returns only the first document from the list.
 *
 * @author yingzi
 */
public class DocumentSelectFirstProcess implements DocumentPostProcessor {

	@Override
	public List<Document> process(Query query, List<Document> documents) {
		if (documents == null || documents.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.singletonList(documents.get(0));
	}

}
