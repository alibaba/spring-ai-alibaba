/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.node;

import com.alibaba.cloud.ai.dto.BusinessKnowledgeDTO;
import com.alibaba.cloud.ai.dto.SemanticModelDTO;
import com.alibaba.cloud.ai.dto.schema.SchemaDTO;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import com.alibaba.cloud.ai.service.base.BaseNl2SqlService;
import com.alibaba.cloud.ai.service.base.BaseSchemaService;
import com.alibaba.cloud.ai.service.business.BusinessKnowledgeRecallService;
import com.alibaba.cloud.ai.service.semantic.SemanticModelRecallService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.dao.DataAccessException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.constant.Constant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TableRelationNodeTest {

	@Mock
	private BaseSchemaService baseSchemaService;

	@Mock
	private BaseNl2SqlService baseNl2SqlService;

	@Mock
	private BusinessKnowledgeRecallService businessKnowledgeRecallService;

	@Mock
	private SemanticModelRecallService semanticModelRecallService;

	private TableRelationNode tableRelationNode;

	private OverAllState state;

	@BeforeEach
	void setUp() {
		tableRelationNode = new TableRelationNode(baseSchemaService, baseNl2SqlService, businessKnowledgeRecallService,
				semanticModelRecallService);

		// Initialize state
		state = new OverAllState();
		state.registerKeyAndStrategy(INPUT_KEY, new ReplaceStrategy());
		state.registerKeyAndStrategy(EVIDENCES, new ReplaceStrategy());
		state.registerKeyAndStrategy(TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT, new ReplaceStrategy());
		state.registerKeyAndStrategy(COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT, new ReplaceStrategy());
		state.registerKeyAndStrategy(AGENT_ID, new ReplaceStrategy());
		state.registerKeyAndStrategy(TABLE_RELATION_RETRY_COUNT, new ReplaceStrategy());
		state.registerKeyAndStrategy(TABLE_RELATION_EXCEPTION_OUTPUT, new ReplaceStrategy());
		state.registerKeyAndStrategy(TABLE_RELATION_OUTPUT, new ReplaceStrategy());
		state.registerKeyAndStrategy(BUSINESS_KNOWLEDGE, new ReplaceStrategy());
		state.registerKeyAndStrategy(SEMANTIC_MODEL, new ReplaceStrategy());
	}

	@Test
	void testSuccessfulExecution() throws Exception {
		// Prepare test data
		setupSuccessfulMocks();
		setupStateWithValidData();

		// Execute test
		Map<String, Object> result = tableRelationNode.apply(state);

		// Verify results
		assertNotNull(result);
		assertTrue(result.containsKey(TABLE_RELATION_OUTPUT));
		assertTrue(result.containsKey(BUSINESS_KNOWLEDGE));
		assertTrue(result.containsKey(SEMANTIC_MODEL));
		assertEquals(0, result.get(TABLE_RELATION_RETRY_COUNT));
		assertEquals("", result.get(TABLE_RELATION_EXCEPTION_OUTPUT));

		// Verify service calls, check TableRelationNode code uses agentId value for
		// DataSetId
		verify(businessKnowledgeRecallService).getFieldByDataSetId("123");
		verify(semanticModelRecallService).getFieldByDataSetId("123");
		verify(baseSchemaService).extractDatabaseName(any(SchemaDTO.class));
		verify(baseNl2SqlService).fineSelect(any(SchemaDTO.class), anyString(), any(List.class));
	}

	@Test
	void testDatabaseQueryException_Retryable() throws Exception {
		// Simulate retryable database exception
		DataAccessException retryableException = new DataAccessException("Connection timeout") {
		};
		when(businessKnowledgeRecallService.getFieldByDataSetId(anyString())).thenThrow(retryableException);

		setupStateWithValidData();
		setupSchemaServiceMocks();

		// execute test
		Map<String, Object> result = tableRelationNode.apply(state);

		// Verify error handling
		assertNotNull(result);
		assertTrue(result.containsKey(TABLE_RELATION_EXCEPTION_OUTPUT));
		assertTrue(result.containsKey(TABLE_RELATION_RETRY_COUNT));

		String errorMessage = (String) result.get(TABLE_RELATION_EXCEPTION_OUTPUT);
		assertTrue(errorMessage.startsWith("RETRYABLE:"));
		assertEquals(1, result.get(TABLE_RELATION_RETRY_COUNT));
	}

	@Test
	void testDatabaseQueryException_NonRetryable() throws Exception {
		// Simulate non-retryable database exception (e.g., H2 Boolean conversion issue)
		DataAccessException nonRetryableException = new DataAccessException(
				"Feature not supported: converting to class boolean") {
		};
		when(businessKnowledgeRecallService.getFieldByDataSetId(anyString())).thenThrow(nonRetryableException);

		setupStateWithValidData();
		setupSchemaServiceMocks();

		// execute test
		Map<String, Object> result = tableRelationNode.apply(state);

		// Verify error handling
		assertNotNull(result);
		assertTrue(result.containsKey(TABLE_RELATION_EXCEPTION_OUTPUT));

		String errorMessage = (String) result.get(TABLE_RELATION_EXCEPTION_OUTPUT);
		assertTrue(errorMessage.startsWith("NON_RETRYABLE:"));
		assertEquals(1, result.get(TABLE_RELATION_RETRY_COUNT));
	}

	@Test
	void testRetryCountIncrement() throws Exception {
		setupStateWithValidData();
		setupSchemaServiceMocks();
		// Set existing retry count
		state.updateState(Map.of(TABLE_RELATION_RETRY_COUNT, 2));

		DataAccessException exception = new DataAccessException("Network error") {
		};
		when(businessKnowledgeRecallService.getFieldByDataSetId(anyString())).thenThrow(exception);

		// execute test
		Map<String, Object> result = tableRelationNode.apply(state);

		// Verify retry count increment
		assertEquals(3, result.get(TABLE_RELATION_RETRY_COUNT));
	}

	@Test
    void testSemanticModelServiceException() throws Exception {
        // Simulate semantic model service exception
        when(businessKnowledgeRecallService.getFieldByDataSetId(anyString()))
                .thenReturn(new ArrayList<>());
        when(semanticModelRecallService.getFieldByDataSetId(anyString()))
                .thenThrow(new DataAccessException("Semantic model service error") {});

        setupStateWithValidData();
        setupSchemaServiceMocks();

        // execute test
        Map<String, Object> result = tableRelationNode.apply(state);

        // Verify error handling
        assertTrue(result.containsKey(TABLE_RELATION_EXCEPTION_OUTPUT));
        assertEquals(1, result.get(TABLE_RELATION_RETRY_COUNT));
    }

	private void setupSuccessfulMocks() throws Exception {
		// Simulate successful business knowledge recall
		List<BusinessKnowledgeDTO> businessKnowledges = List.of(createBusinessKnowledge("term1", "description1"));
		when(businessKnowledgeRecallService.getFieldByDataSetId(anyString())).thenReturn(businessKnowledges);

		// Simulate successful semantic model recall
		List<SemanticModelDTO> semanticModels = List.of(createSemanticModel("user_age", "用户年龄"));
		when(semanticModelRecallService.getFieldByDataSetId(anyString())).thenReturn(semanticModels);

		setupSchemaServiceMocks();
	}

	private void setupSchemaServiceMocks() {
		// Simulate Schema service
		doNothing().when(baseSchemaService).extractDatabaseName(any(SchemaDTO.class));
		doNothing().when(baseSchemaService).buildSchemaFromDocuments(any(), any(), any());

		SchemaDTO mockSchema = new SchemaDTO();
		when(baseNl2SqlService.fineSelect(any(SchemaDTO.class), anyString(), any(List.class))).thenReturn(mockSchema);
	}

	private void setupStateWithValidData() {
		List<Document> tableDocuments = List.of(new Document("table content"));
		List<List<Document>> columnDocuments = List.of(List.of(new Document("column content")));

		state.updateState(Map.of(INPUT_KEY, "test query", EVIDENCES, List.of("evidence1"),
				TABLE_DOCUMENTS_FOR_SCHEMA_OUTPUT, tableDocuments, COLUMN_DOCUMENTS_BY_KEYWORDS_OUTPUT, columnDocuments,
				AGENT_ID, "123", TABLE_RELATION_RETRY_COUNT, 0));
	}

	private BusinessKnowledgeDTO createBusinessKnowledge(String term, String description) {
		BusinessKnowledgeDTO dto = new BusinessKnowledgeDTO();
		dto.setBusinessTerm(term);
		dto.setDescription(description);
		return dto;
	}

	private SemanticModelDTO createSemanticModel(String originalFieldName, String agentFieldName) {
		// 使用 SemanticModelDTO 的实际构造函数和字段
		SemanticModelDTO dto = new SemanticModelDTO();
		dto.setOriginalFieldName(originalFieldName);
		dto.setAgentFieldName(agentFieldName);
		dto.setFieldSynonyms("年龄,岁数");
		dto.setFieldDescription("用户的年龄信息");
		dto.setFieldType("INTEGER");
		dto.setOriginalDescription("数据库中的用户年龄字段");
		dto.setDefaultRecall(true);
		dto.setEnabled(true);
		return dto;
	}

}
