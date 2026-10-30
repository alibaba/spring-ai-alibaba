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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@code resume()} and {@code addHumanFeedback(...)} both write
 * {@link RunnableConfig#HUMAN_FEEDBACK_METADATA_KEY}, so one must not clobber the other.
 * <p>
 * The key carries two different meanings: a real {@link InterruptionMetadata} holds the
 * approved/rejected/edited decisions, while the {@code "placeholder"} string only marks
 * "continue from the latest checkpoint". Because both go through a plain {@code Map.put},
 * the placeholder used to overwrite the feedback, and readers that expect the
 * {@code InterruptionMetadata} either failed on the cast or silently saw no feedback.
 */
class RunnableConfigResumeFeedbackTest {

	private static InterruptionMetadata feedback() {
		return InterruptionMetadata.builder()
			.nodeId("tool-node")
			.addToolFeedback(ToolFeedback.builder()
				.id("call-1")
				.name("query_order")
				.arguments("{}")
				.result(ToolFeedback.FeedbackResult.APPROVED)
				.description("approved by the user")
				.build())
			.build();
	}

	@Test
	void resumeKeepsFeedbackAddedBeforeIt() {
		InterruptionMetadata feedback = feedback();

		RunnableConfig config = RunnableConfig.builder()
			.threadId("t-1")
			.addHumanFeedback(feedback)
			.resume()
			.build();

		assertSame(feedback, config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElseThrow(),
				"resume() must not replace the feedback it was chained after");
	}

	@Test
	void feedbackWinsWhicheverOrderTheTwoMethodsAreCalledIn() {
		InterruptionMetadata feedback = feedback();

		RunnableConfig config = RunnableConfig.builder()
			.threadId("t-1")
			.resume()
			.addHumanFeedback(feedback)
			.build();

		assertSame(feedback, config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElseThrow());
	}

	@Test
	void resumeStillMarksTheRunWhenNoFeedbackIsGiven() {
		RunnableConfig config = RunnableConfig.builder().threadId("t-1").resume().build();

		assertEquals("placeholder", config.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElseThrow(),
				"a plain resume() must still produce the resume marker");
	}

	@Test
	void withResumeKeepsExistingFeedback() {
		InterruptionMetadata feedback = feedback();
		RunnableConfig config = RunnableConfig.builder().threadId("t-1").addHumanFeedback(feedback).build();

		RunnableConfig resumed = config.withResume();

		assertSame(feedback, resumed.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElseThrow(),
				"withResume() must not replace the feedback the config already carries");
	}

	@Test
	void withResumeAddsPlaceholderWhenNoFeedbackIsPresent() {
		RunnableConfig config = RunnableConfig.builder().threadId("t-1").build();

		RunnableConfig resumed = config.withResume();

		assertEquals("placeholder", resumed.metadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY).orElseThrow());
	}

}
