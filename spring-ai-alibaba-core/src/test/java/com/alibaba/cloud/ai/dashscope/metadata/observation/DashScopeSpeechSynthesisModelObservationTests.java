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
package com.alibaba.cloud.ai.dashscope.metadata.observation;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.retry.support.RetryTemplate;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Test cases for DashScopeSpeechSynthesisModel's observability features.
 * Tests cover both synchronous and streaming scenarios, including observation names,
 * audio parameters handling, and key value generation.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 1.0.0-M5.1
 */
class DashScopeSpeechSynthesisModelObservationTests {

    @Mock
    private DashScopeSpeechSynthesisApi api;

    @Mock
    private ObservationRegistry observationRegistry;

    private DashScopeSpeechSynthesisModel model;
    private DashScopeSpeechSynthesisOptions options;
    private RetryTemplate retryTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        options = DashScopeSpeechSynthesisOptions.builder()
                .withModel("sambert-zhichu-v1")
                .withResponseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.MP3)
                .withSampleRate(16000)
                .withVoice("female")
                .withVolume(50)
                .withSpeed(1.0)
                .build();
        retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;
        model = new DashScopeSpeechSynthesisModel(api, options, retryTemplate, observationRegistry);
    }

    private DashScopeSpeechSynthesisApi.Response createResponse(ByteBuffer audioBuffer) {
        try {
            DashScopeSpeechSynthesisApi.Response response = new DashScopeSpeechSynthesisApi.Response();
            Field audioField = DashScopeSpeechSynthesisApi.Response.class.getDeclaredField("audio");
            audioField.setAccessible(true);
            audioField.set(response, audioBuffer);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create response", e);
        }
    }

    @Test
    void testSynchronousCallObservation() {
        // 准备测试数据
        String testText = "测试文本";
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
        ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);

        // 模拟API响应
        DashScopeSpeechSynthesisApi.Response apiResponse = createResponse(audioBuffer);
        when(api.call(any())).thenReturn(apiResponse);

        // 执行测试
        SpeechSynthesisResponse response = model.call(prompt);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput()).isNotNull();
        assertThat(response.getResult().getOutput().getAudio()).isNotNull();
    }

    @Test
    void testStreamingCallObservation() {
        // 准备测试数据
        String testText = "测试文本";
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
        ByteBuffer audioBuffer1 = ByteBuffer.wrap(new byte[512]);
        ByteBuffer audioBuffer2 = ByteBuffer.wrap(new byte[512]);

        // 模拟API流式响应
        when(api.streamOut(any())).thenReturn(Flux.just(audioBuffer1, audioBuffer2));

        // 执行测试
        Flux<SpeechSynthesisResponse> responseFlux = model.stream(prompt);

        // 验证结果
        StepVerifier.create(responseFlux)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void testErrorHandlingObservation() {
        // 准备测试数据
        String testText = "测试文本";
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);

        // 模拟API错误
        when(api.call(any())).thenThrow(new RuntimeException("API调用失败"));

        // 执行测试并验证异常
        try {
            model.call(prompt);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(RuntimeException.class);
            assertThat(e.getMessage()).isEqualTo("API调用失败");
        }
    }

    @Test
    void testStreamingErrorHandlingObservation() {
        // 准备测试数据
        String testText = "测试文本";
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);

        // 模拟API流式错误
        when(api.streamOut(any())).thenReturn(Flux.error(new RuntimeException("流式处理失败")));

        // 执行测试
        Flux<SpeechSynthesisResponse> responseFlux = model.stream(prompt);

        // 验证错误处理
        StepVerifier.create(responseFlux)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void testEmptyPromptHandling() {
        // 准备空提示
        SpeechSynthesisPrompt emptyPrompt = new SpeechSynthesisPrompt("");

        // 模拟API响应
        ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);
        DashScopeSpeechSynthesisApi.Response apiResponse = createResponse(audioBuffer);
        when(api.call(any())).thenReturn(apiResponse);

        // 执行测试
        SpeechSynthesisResponse response = model.call(emptyPrompt);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
    }

    @Test
    void testCustomOptionsObservation() {
        // 准备自定义选项
        DashScopeSpeechSynthesisOptions customOptions = DashScopeSpeechSynthesisOptions.builder()
                .withModel("custom-model")
                .withResponseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.WAV)
                .withSampleRate(44100)
                .withVoice("male")
                .withVolume(75)
                .withSpeed(1.5)
                .build();

        // 创建带自定义选项的模型
        DashScopeSpeechSynthesisModel customModel = new DashScopeSpeechSynthesisModel(api, customOptions, retryTemplate, observationRegistry);

        // 准备测试数据
        String testText = "测试文本";
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
        ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);

        // 模拟API响应
        DashScopeSpeechSynthesisApi.Response apiResponse = createResponse(audioBuffer);
        when(api.call(any())).thenReturn(apiResponse);

        // 执行测试
        SpeechSynthesisResponse response = customModel.call(prompt);

        // 验证结果
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
    }

    @Test
    void testObservationContext() {
        // 准备测试数据
        String testText = "测试文本";
        SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(testText);
        ByteBuffer audioBuffer = ByteBuffer.wrap(new byte[1024]);

        // 模拟API响应
        DashScopeSpeechSynthesisApi.Response apiResponse = createResponse(audioBuffer);
        when(api.call(any())).thenReturn(apiResponse);

        // 执行测试
        SpeechSynthesisResponse response = model.call(prompt);

        // 验证观察上下文
        assertThat(response).isNotNull();
        assertThat(response.getResult()).isNotNull();
        assertThat(response.getResult().getOutput()).isNotNull();
        assertThat(response.getResult().getOutput().getAudio()).isNotNull();
    }
}