/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multimodal.workflow;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.examples.multimodal.MultimodalExceptionHandler;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageGeneration;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageWorkflowTest {

	private static final String OBSERVATION = "A white bird on a blue background";

	@TempDir
	Path directory;

	private final AtomicInteger imageCalls = new AtomicInteger();

	private byte[] imageBytes;

	private MockMvc mvc;

	@BeforeEach
	void setUp() throws Exception {
		this.imageBytes = new ClassPathResource("images/sample.png").getContentAsByteArray();
		ChatModel vision = new ChatModel() {
			@Override
			public DashScopeChatOptions getOptions() {
				return DashScopeChatOptions.builder().build();
			}

			@Override
			public ChatResponse call(Prompt prompt) {
				var user = prompt.getUserMessage();
				assertThat(user.getText()).isEqualTo("Describe the picture");
				assertThat(user.getMedia()).hasSize(1);
				assertThat(user.getMedia().get(0).getDataAsByteArray()).isEqualTo(imageBytes);
				boolean hasIllustrationTool = prompt.getOptions() instanceof ToolCallingChatOptions options
						&& options.getToolCallbacks().stream().anyMatch(tool ->
								"generate_illustration".equals(tool.getToolDefinition().name()));
				var message = hasIllustrationTool
						? AssistantMessage.builder().toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function",
								"generate_illustration", """
									{"observation":"A white bird on a blue background", "prompt":"Illustrate a white bird on blue"}
									"""))).build()
						: new AssistantMessage(OBSERVATION);
				return new ChatResponse(List.of(new Generation(message)));
			}
		};
		ImageModel image = prompt -> {
			assertThat(prompt.getInstructions().get(0).getText()).containsIgnoringCase("white bird");
			assertThat(prompt.getOptions()).isInstanceOfSatisfying(DashScopeImageOptions.class,
					options -> assertThat(options.getResponseFormat()).isEqualTo("b64_json"));
			this.imageCalls.incrementAndGet();
			String encoded = Base64.getEncoder().encodeToString(imageBytes);
			return new ImageResponse(List.of(new ImageGeneration(new Image(null, encoded))));
		};
		var store = new ImageArtifactStore(directory.toString());
		var visionAgent = ReactAgent.builder().name("fixture-vision").model(vision).build();
		var workflow = new ImageWorkflow(vision, visionAgent, image, store, "qwen-image-3.0-pro");
		this.mvc = MockMvcBuilders.standaloneSetup(new ImageWorkflowController(workflow, store))
			.setControllerAdvice(new MultimodalExceptionHandler())
			.build();
	}

	@Test
	void agentToolAndGraphGenerationDeliverStoredImages() throws Exception {
		for (String mode : List.of("agent", "graph")) {
			String body = mvc.perform(multipart("/api/workflows/{mode}", mode).file(uploadedImage())
				.param("question", "Describe the picture"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.observation").value(OBSERVATION))
				.andExpect(jsonPath("$.artifacts.length()").value(1))
				.andExpect(jsonPath("$.artifacts[0].mimeType").value("image/png"))
				.andExpect(jsonPath("$.artifacts[0].sizeBytes").value(imageBytes.length))
				.andReturn().getResponse().getContentAsString();
			var artifact = JsonMapper.shared().readTree(body).path("artifacts").get(0);
			var persisted = new ImageArtifactStore(directory.toString()).describe(UUID.fromString(artifact.path("id").asString()));
			assertThat(persisted.url()).isEqualTo(artifact.path("url").asString());
			mvc.perform(get(persisted.url()))
				.andExpect(status().isOk())
				.andExpect(content().contentType("image/png"))
				.andExpect(content().bytes(imageBytes));
			mvc.perform(multipart("/api/workflows/{mode}", mode).file(uploadedImage())
				.param("question", "Describe the picture").param("createIllustration", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.artifacts").isEmpty());
		}
		assertThat(imageCalls.get()).isEqualTo(2);
		try (var stagedUploads = Files.list(directory.resolve("inputs"))) {
			assertThat(stagedUploads).isEmpty();
		}
		mvc.perform(get("/api/artifacts/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
	}

	@Test
	void bothModesCanInspectWithoutGenerating() throws Exception {
		for (String mode : List.of("agent", "graph")) {
			mvc.perform(multipart("/api/workflows/{mode}", mode).file(uploadedImage())
				.param("question", "Describe the picture").param("createIllustration", "false"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.observation").value(OBSERVATION))
				.andExpect(jsonPath("$.artifacts").isEmpty());
		}
		assertThat(imageCalls.get()).isZero();
	}

	@Test
	void acceptsImagesBasedOnTheirContent() throws Exception {
		var invalid = new MockMultipartFile("image", "invalid.png", "image/png", new byte[] { 1, 2, 3 });
		mvc.perform(multipart("/api/workflows/agent").file(invalid).param("question", "Describe the picture"))
			.andExpect(status().isBadRequest());
	}

	private MockMultipartFile uploadedImage() {
		return new MockMultipartFile("image", "sample.png", "application/octet-stream", imageBytes);
	}

}
