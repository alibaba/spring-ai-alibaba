/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.dsl.adapters;

import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.service.dsl.adapters.CustomDSLAdapter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Slf4j
public class CustomDSLAdapterTest {

	private final CustomDSLAdapter customDSLAdapter;

	@Autowired
	public CustomDSLAdapterTest(CustomDSLAdapter customDSLAdapter) {
		this.customDSLAdapter = customDSLAdapter;
	}

	/**
	 * metadata test case name: test-workflow description: "this is a workflow only for
	 * test" mode: workflow
	 */
	@Test
	public void testMapToMetadata() {
		Map<String, Object> metadataMap = Map.of("metadata",
				Map.of("name", "test-workflow", "description", "this is a workflow only for test", "mode", "workflow"));
		AppMetadata metadata = customDSLAdapter.mapToMetadata(metadataMap);
		assertNotNull(metadata);
		assertNotNull(metadata.getId());
		assertEquals("workflow", metadata.getMode());
		log.info("map to metadata: " + metadata);
	}

	@Test
	public void testMetadataToMap() {
		AppMetadata metadata = new AppMetadata().setName("test-workflow")
			.setDescription("this is a workflow only for test")
			.setMode("workflow");
		Map<String, Object> metadataMap = customDSLAdapter.metadataToMap(metadata);
		assertNotNull(metadataMap);
		Map metadataInMap = (Map) metadataMap.get("metadata");
		assertEquals("test-workflow", metadataInMap.get("name"));
		assertEquals("this is a workflow only for test", metadataInMap.get("description"));
		assertEquals("workflow", metadataInMap.get("mode"));
	}

	// TODO
	@Test
	public void testMapToWorkflow() {

	}

	// TODO
	@Test
	public void testWorkflowToMap() {

	}

}
