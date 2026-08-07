/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link InterruptionMetadata} and its nested {@link ToolFeedback}
 * builder logic. These tests are self-contained and require no model, network or
 * datastore.
 */
class InterruptionMetadataTest {

	private OverAllState newState() {
		return new OverAllState(Map.of("messages", List.of()));
	}

	@Test
	void builderWithNodeIdAndStateStoresNodeAndState() {
		OverAllState state = newState();
		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", state).build();

		assertEquals("nodeA", metadata.node());
		assertSame(state, metadata.state());
	}

	@Test
	void toolFeedbacksDefaultsToEmptyListNotNull() {
		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState()).build();

		assertNotNull(metadata.toolFeedbacks(), "toolFeedbacks should never be null");
		assertTrue(metadata.toolFeedbacks().isEmpty());
	}

	@Test
	void toolsAutomaticallyApprovedDefaultsToEmptyListNotNull() {
		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState()).build();

		assertNotNull(metadata.getToolsAutomaticallyApproved(),
				"toolsAutomaticallyApproved should default to an empty list");
		assertTrue(metadata.getToolsAutomaticallyApproved().isEmpty());
	}

	@Test
	void addToolFeedbackAccumulatesFeedbacks() {
		ToolFeedback first = ToolFeedback.builder().id("id-1").name("poem").build();
		ToolFeedback second = ToolFeedback.builder().id("id-2").name("weather").build();

		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState())
			.addToolFeedback(first)
			.addToolFeedback(second)
			.build();

		assertEquals(2, metadata.toolFeedbacks().size());
		assertEquals("poem", metadata.toolFeedbacks().get(0).getName());
		assertEquals("weather", metadata.toolFeedbacks().get(1).getName());
	}

	@Test
	void metadataValuesAreAccessibleByKey() {
		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState())
			.addMetadata("k1", "v1")
			.build();

		assertEquals(Optional.of("v1"), metadata.metadata("k1"));
		assertTrue(metadata.metadata().isPresent());
		assertEquals("v1", metadata.metadata().get().get("k1"));
	}

	@Test
	void addToolsAutomaticallyApprovedAccumulatesToolCalls() {
		AssistantMessage.ToolCall call = new AssistantMessage.ToolCall("id-1", "function", "poem", "{}");

		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState())
			.addToolsAutomaticallyApproved(call)
			.build();

		assertEquals(1, metadata.getToolsAutomaticallyApproved().size());
		assertEquals("poem", metadata.getToolsAutomaticallyApproved().get(0).name());
	}

	@Test
	void copyBuilderPreservesMetadataNodeStateAndAutoApproved() {
		OverAllState state = newState();
		AssistantMessage.ToolCall approved = new AssistantMessage.ToolCall("id-1", "function", "poem", "{}");
		InterruptionMetadata original = InterruptionMetadata.builder("nodeA", state)
			.addMetadata("k1", "v1")
			.addToolsAutomaticallyApproved(approved)
			.addToolFeedback(ToolFeedback.builder().id("id-1").name("poem").build())
			.build();

		InterruptionMetadata copy = InterruptionMetadata.builder(original).build();

		assertEquals("nodeA", copy.node());
		assertSame(state, copy.state());
		assertEquals(Optional.of("v1"), copy.metadata("k1"));
		assertEquals(1, copy.getToolsAutomaticallyApproved().size());
		assertEquals("poem", copy.getToolsAutomaticallyApproved().get(0).name());
	}

	@Test
	void copyBuilderDoesNotCarryOverToolFeedbacks() {
		// The copy builder intentionally does not copy toolFeedbacks (see
		// InterruptionMetadata.builder(InterruptionMetadata)); resuming code rebuilds
		// feedbacks explicitly. This test pins that contract.
		InterruptionMetadata original = InterruptionMetadata.builder("nodeA", newState())
			.addToolFeedback(ToolFeedback.builder().id("id-1").name("poem").build())
			.build();

		InterruptionMetadata copy = InterruptionMetadata.builder(original).build();

		assertNotNull(copy.toolFeedbacks());
		assertTrue(copy.toolFeedbacks().isEmpty(), "copy builder should start with no tool feedbacks");
	}

	@Test
	void toolFeedbacksSetterCopiesInputList() {
		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState())
			.toolFeedbacks(List.of(ToolFeedback.builder().id("id-1").name("poem").build()))
			.build();

		assertEquals(1, metadata.toolFeedbacks().size());
		assertEquals("poem", metadata.toolFeedbacks().get(0).getName());
	}

	@Test
	void toStringContainsNodeId() {
		InterruptionMetadata metadata = InterruptionMetadata.builder("nodeA", newState()).build();

		assertTrue(metadata.toString().contains("nodeA"));
	}

	@Test
	void toolFeedbackBuilderStoresAllFields() {
		ToolFeedback feedback = ToolFeedback.builder()
			.id("id-1")
			.name("poem")
			.arguments("{\"topic\":\"spring\"}")
			.result(FeedbackResult.APPROVED)
			.description("please confirm")
			.build();

		assertEquals("id-1", feedback.getId());
		assertEquals("poem", feedback.getName());
		assertEquals("{\"topic\":\"spring\"}", feedback.getArguments());
		assertEquals(FeedbackResult.APPROVED, feedback.getResult());
		assertEquals("please confirm", feedback.getDescription());
	}

	@Test
	void toolFeedbackTypeParsesResultCaseInsensitively() {
		assertEquals(FeedbackResult.APPROVED, ToolFeedback.builder().type("approved").build().getResult());
		assertEquals(FeedbackResult.REJECTED, ToolFeedback.builder().type("Rejected").build().getResult());
		assertEquals(FeedbackResult.EDITED, ToolFeedback.builder().type("EDITED").build().getResult());
	}

	@Test
	void toolFeedbackTypeRejectsUnknownValue() {
		assertThrows(IllegalArgumentException.class, () -> ToolFeedback.builder().type("unknown").build());
	}

	@Test
	void toolFeedbackCopyBuilderClonesAllFields() {
		ToolFeedback original = ToolFeedback.builder()
			.id("id-1")
			.name("poem")
			.arguments("{}")
			.result(FeedbackResult.EDITED)
			.description("desc")
			.build();

		ToolFeedback copy = ToolFeedback.builder(original).build();

		assertNotSame(original, copy);
		assertEquals(original.getId(), copy.getId());
		assertEquals(original.getName(), copy.getName());
		assertEquals(original.getArguments(), copy.getArguments());
		assertEquals(original.getResult(), copy.getResult());
		assertEquals(original.getDescription(), copy.getDescription());
	}

}
