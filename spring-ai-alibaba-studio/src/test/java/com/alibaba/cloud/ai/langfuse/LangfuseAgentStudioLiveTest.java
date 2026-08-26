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

package com.alibaba.cloud.ai.langfuse;

import com.alibaba.cloud.ai.StudioApplication;
import com.alibaba.cloud.ai.agent.studio.dto.Thread;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = StudioApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles({ "graph", "langfuse" })
@TestPropertySource(properties = {
		"logging.level.io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor=OFF",
		"logging.level.reactor.core.scheduler.Schedulers=OFF"
})
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "LANGFUSE_ENABLED", matches = "(?i:true|1|yes)")
@EnabledIfEnvironmentVariable(named = "LANGFUSE_OTEL_AUTH", matches = ".+")
class LangfuseAgentStudioLiveTest {

	private static final String APP_NAME = "single_agent";

	private static final String USER_ID = "langfuse-user";

	@Autowired
	private MockMvc mockMvc;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void runAgentAndExportTraceToLangfuse() throws Exception {
		MvcResult createResult = mockMvc.perform(post("/apps/{appName}/users/{userId}/threads", APP_NAME, USER_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"))
			.andExpect(status().isOk())
			.andReturn();

		Thread created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Thread.class);

		Map<String, Object> requestBody = Map.of(
				"appName", APP_NAME,
				"userId", USER_ID,
				"threadId", created.threadId(),
				"newMessage", Map.of(
						"messageType", "user",
						"content", "简单介绍下Spring Ai Alibaba",
						"metadata", Map.of(),
						"media", List.of()
				),
				"streaming", true
		);

		MvcResult asyncResult = mockMvc.perform(post("/run_sse")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody))
				.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
			.andExpect(request().asyncStarted())
			.andReturn();

		MvcResult runResult = mockMvc.perform(asyncDispatch(asyncResult))
			.andExpect(status().isOk())
			.andReturn();

		assertThat(runResult.getResponse().getContentAsString()).isNotBlank();
	}

}
