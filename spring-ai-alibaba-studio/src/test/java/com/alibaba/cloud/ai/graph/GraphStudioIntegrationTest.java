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

import com.alibaba.cloud.ai.StudioApplication;
import com.alibaba.cloud.ai.agent.studio.dto.Thread;
import com.alibaba.cloud.ai.agent.studio.dto.GraphResponse;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for Graph Studio API endpoints.
 * Uses unified {@link StudioApplication} that supports both graph and agent.
 */
@SpringBootTest(classes = StudioApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("graph")
class GraphStudioIntegrationTest {

	private static final String GRAPH_NAME = "simple_workflow";

	private static final String USER_ID = "user-001";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ApplicationContext applicationContext;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Test
	void contextLoads_graphAndAgentBeansPresent() {
		String[] graphLoaders = applicationContext.getBeanNamesForType(com.alibaba.cloud.ai.agent.studio.loader.GraphLoader.class);
		String[] graphControllers = applicationContext.getBeanNamesForType(com.alibaba.cloud.ai.agent.studio.controller.GraphController.class);
		String[] compiledGraphs = applicationContext.getBeanNamesForType(com.alibaba.cloud.ai.graph.CompiledGraph.class);
		String[] agentLoaders = applicationContext.getBeanNamesForType(com.alibaba.cloud.ai.agent.studio.loader.AgentLoader.class);
		assertThat(compiledGraphs).as("CompiledGraph beans: %s", (Object) compiledGraphs).isNotEmpty();
		assertThat(graphLoaders).as("GraphLoader beans: %s", (Object) graphLoaders).isNotEmpty();
		assertThat(graphControllers).as("GraphController beans: %s", (Object) graphControllers).isNotEmpty();
		assertThat(agentLoaders).as("AgentLoader beans: %s", (Object) agentLoaders).isNotEmpty();
	}

	@Test
	void listApps_returnsAgentNames() throws Exception {
		MvcResult result = mockMvc.perform(get("/list-apps"))
				.andExpect(status().isOk())
				.andReturn();

		List<String> agents = objectMapper.readValue(result.getResponse().getContentAsString(),
				objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

		assertThat(agents).contains("single_agent");
	}

	@Test
	void listGraphs_returnsGraphNames() throws Exception {
		MvcResult result = mockMvc.perform(get("/list-graphs"))
				.andExpect(status().isOk())
				.andReturn();

		List<String> graphs = objectMapper.readValue(result.getResponse().getContentAsString(),
				objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));

		assertThat(graphs).contains(GRAPH_NAME);
	}

	@Test
	void getGraphRepresentation_returnsMermaid() throws Exception {
		MvcResult result = mockMvc.perform(get("/graphs/{graphName}/representation", GRAPH_NAME))
				.andExpect(status().isOk())
				.andReturn();

		GraphResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), GraphResponse.class);

		assertThat(response.getMermaidSrc()).isNotNull().isNotEmpty();
	}

	@Test
	void graphThreads_createAndListAndDelete() throws Exception {
		// Create thread
		MvcResult createResult = mockMvc.perform(post("/graphs/{graphName}/users/{userId}/threads", GRAPH_NAME, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andReturn();

		Thread created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Thread.class);
		assertThat(created.threadId()).isNotNull();
		String threadId = created.threadId();

		// List threads
		MvcResult listResult = mockMvc.perform(get("/graphs/{graphName}/users/{userId}/threads", GRAPH_NAME, USER_ID))
				.andExpect(status().isOk())
				.andReturn();

		List<Thread> threads = objectMapper.readValue(listResult.getResponse().getContentAsString(),
				objectMapper.getTypeFactory().constructCollectionType(List.class, Thread.class));
		assertThat(threads).anyMatch(t -> t.threadId().equals(threadId));

		// Get thread
		mockMvc.perform(get("/graphs/{graphName}/users/{userId}/threads/{threadId}", GRAPH_NAME, USER_ID, threadId))
				.andExpect(status().isOk());

		// Delete thread
		mockMvc.perform(delete("/graphs/{graphName}/users/{userId}/threads/{threadId}", GRAPH_NAME, USER_ID, threadId))
				.andExpect(status().isNoContent());
	}

	@Test
	void graphRunSse_executesGraph() throws Exception {
		// Create thread first
		MvcResult createResult = mockMvc.perform(post("/graphs/{graphName}/users/{userId}/threads", GRAPH_NAME, USER_ID)
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isOk())
				.andReturn();

		Thread created = objectMapper.readValue(createResult.getResponse().getContentAsString(), Thread.class);
		String threadId = created.threadId();

		// Run graph
		Map<String, Object> request = Map.of(
				"graphName", GRAPH_NAME,
				"userId", USER_ID,
				"threadId", threadId,
				"newMessage", Map.of(
						"messageType", "user",
						"content", "hello",
						"metadata", Map.of(),
						"media", List.of()
				),
				"streaming", true
		);

		MvcResult runResult = mockMvc.perform(post("/graph_run_sse")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(request))
						.accept(MediaType.TEXT_EVENT_STREAM_VALUE))
				.andExpect(status().isOk())
				.andReturn();

		String responseBody = runResult.getResponse().getContentAsString();
		assertThat(responseBody).isNotEmpty();
	}

	@Test
	void getGraphRepresentation_notFound_whenGraphMissing() throws Exception {
		mockMvc.perform(get("/graphs/{graphName}/representation", "nonexistent_graph"))
				.andExpect(status().isNotFound());
	}

}
