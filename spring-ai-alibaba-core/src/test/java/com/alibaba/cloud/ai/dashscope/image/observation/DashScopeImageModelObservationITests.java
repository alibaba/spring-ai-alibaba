/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.dashscope.image.observation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi.DashScopeImageAsyncReponse;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseOutput;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi.DashScopeImageAsyncReponse.DashScopeImageAsyncReponseResult;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi.DashScopeImageRequest;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.dashscope.observation.conventions.AiProvider;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.mockito.Mockito;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author Polaris
 * @since 1.0.0-m8.1
 *
 */
class DashScopeImageModelObservationTests {

	private final DashScopeImageModel imageModel;

	private final TestObservationRegistry observationRegistry;

	public DashScopeImageModelObservationTests() {
		this.observationRegistry = TestObservationRegistry.create();
		this.imageModel = new DashScopeImageModel(new DashScopeImageApi("sk" + "-7a74bd9492b24f6f835a03e01affe294"),
				observationRegistry);
		DefaultImageModelObservationConvention defaultImageModelObservationConvention = new DefaultImageModelObservationConvention();
		this.imageModel.setObservationConvention(defaultImageModelObservationConvention);
	}

	@Test
	@Tag("observation")
	@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".*")
	void imageModelObservationTest() {

		DashScopeImageOptions options = DashScopeImageOptions.builder()
			.withModel("wanx-v1")
			.withN(1)
			.withWidth(1024)
			.withHeight(1024)
			.withSeed(42)
			.build();

		var instructions = """
				A light cream colored mini golden doodle with a sign that contains the message "I'm on my way to BARCADE!".""";

		ImagePrompt imagePrompt = new ImagePrompt(instructions, options);

		ImageResponse imageResponse = imageModel.call(imagePrompt);

		assertThat(imageResponse.getResults()).hasSize(1);

		ImageResponseMetadata imageResponseMetadata = imageResponse.getMetadata();
		assertThat(imageResponseMetadata.getCreated()).isPositive();

		var generation = imageResponse.getResult();
		Image image = generation.getOutput();
		assertThat(image.getUrl()).isNotEmpty();

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.doesNotHaveAnyRemainingCurrentObservation()
			.hasObservationWithNameEqualTo(DefaultImageModelObservationConvention.DEFAULT_NAME)
			.that()
			.hasContextualNameEqualTo("image " + "wanx-v1")
			.hasHighCardinalityKeyValue(
					ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_SIZE.asString(),
					"1024x1024")
			.hasLowCardinalityKeyValue(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER.asString(),
					AiProvider.DASHSCOPE.value())
			.hasLowCardinalityKeyValue(
					ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),
					AiOperationType.IMAGE.value());
	}

	@Test
	void mockedDashScopeApiShouldStillTriggerObservability() {

		TestObservationRegistry observationRegistry = TestObservationRegistry.create();

		DashScopeImageApi mockApi = Mockito.mock(DashScopeImageApi.class);

		// mock
		var fakeResult = new DashScopeImageAsyncReponseResult("https://example-image.url/image.png");

		var output = new DashScopeImageAsyncReponseOutput("00001", "SUCCEEDED", List.of(fakeResult), null, "code",
				"msg");

		var response = new DashScopeImageAsyncReponse("req-test", output, null);

		Mockito.when(mockApi.submitImageGenTask(any(DashScopeImageRequest.class)))
			.thenReturn(ResponseEntity.ok(new DashScopeImageAsyncReponse(output.taskId(), output, null)));

		Mockito.when(mockApi.getImageGenTaskResult(any(String.class))).thenReturn(ResponseEntity.ok(response));

		DashScopeImageModel model = DashScopeImageModel.builder()
			.dashScopeApi(mockApi)
			.observationRegistry(observationRegistry)
			.build();

		DashScopeImageOptions options = DashScopeImageOptions.builder()
			.withModel("wanx-v1")
			.withWidth(512)
			.withHeight(512)
			.withFunction("mock-fn")
			.withN(1)
			.build();

		ImagePrompt prompt = new ImagePrompt("A test image", options);
		ImageResponse responseObj = model.call(prompt);

		assertThat(responseObj).isNotNull();
		assertThat(responseObj.getResults()).hasSize(1);
		assertThat(responseObj.getResult().getOutput().getUrl()).isEqualTo("https://example-image.url/image.png");

		TestObservationRegistryAssert.assertThat(observationRegistry)
			.hasObservationWithNameEqualTo("dashscope.image.model.operation")
			.that()
			.hasHighCardinalityKeyValue("gen_ai.dashscope.function", "mock-fn")
			.hasLowCardinalityKeyValue("gen_ai.operation.name", "image");
	}

}
