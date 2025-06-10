package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.request.DeleteRequest;
import org.springframework.ai.vectorstore.SearchRequest;
import com.alibaba.cloud.ai.request.EvidenceRequest;
import com.alibaba.cloud.ai.request.SchemaInitRequest;
import org.springframework.ai.document.Document;

import java.util.List;

public interface VectorStoreManagementService {

	Boolean addEvidence(List<EvidenceRequest> evidenceRequests);

	Boolean deleteDocuments(DeleteRequest deleteRequest) throws Exception;

	Boolean schema(SchemaInitRequest schemaInitRequest) throws Exception;

}
