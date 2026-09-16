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
package com.alibaba.cloud.ai.graph.agent.flow.node;

import java.util.List;
import java.util.Map;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.alibaba.cloud.ai.graph.GraphResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.agent.BaseAgent;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnhancedParallelResultAggregatorLogSafetyTest {

	@Test
	void escapesExternalStateKeysWithoutChangingState() throws Exception {
		BaseAgent agent = mock(BaseAgent.class);
		when(agent.name()).thenReturn("agent");
		when(agent.getOutputKey()).thenReturn("output");
		String key = "external\r\nforged entry";
		OverAllState state = new OverAllState(Map.of("output",
				GraphResponse.done(Map.of("output", "result", key, "value\ntext"))));
		Logger logger = (Logger) LoggerFactory.getLogger(EnhancedParallelResultAggregator.class);
		Level previous = logger.getLevel();
		ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.DEBUG);
		try {
			Map<String, Object> result = new EnhancedParallelResultAggregator(
					null, List.of(agent), null, null).apply(state);
			assertThat(result).containsEntry(key, "value\ntext");
			assertThat(appender.list).isNotEmpty();
			assertThat(appender.list).allSatisfy(event ->
					assertThat(event.getFormattedMessage()).doesNotContain("\r", "\n"));
			assertThat(appender.list.stream().map(ILoggingEvent::getFormattedMessage))
					.anyMatch(message -> message.contains("external\\r\\nforged entry"));
		}
		finally {
			logger.detachAppender(appender);
			logger.setLevel(previous);
			appender.stop();
		}
	}

}
