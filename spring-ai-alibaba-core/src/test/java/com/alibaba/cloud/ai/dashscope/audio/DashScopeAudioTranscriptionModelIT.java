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
package com.alibaba.cloud.ai.dashscope.audio;

import com.alibaba.cloud.ai.autoconfig.dashscope.DashScopeAutoConfiguration;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;

import java.net.MalformedURLException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;

@TestPropertySource("classpath:application.yml")
@SpringBootTest(classes = DashScopeAutoConfiguration.class)
public class DashScopeAudioTranscriptionModelIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeAudioTranscriptionModelIT.class);

	private static final String TEST_URL = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";

	private static final String TEST_PATH = "src/test/resources/speech/count.pcm";

	@Autowired
	private DashScopeAudioTranscriptionModel model;

	@Test
	void call() {
		try {
			AudioTranscriptionResponse response = model.call(new AudioTranscriptionPrompt(new UrlResource(TEST_URL),
					DashScopeAudioTranscriptionOptions.builder().withModel("sensevoice-v1").build()));
			DashScopeAudioTranscriptionApi.Response.Output output = Objects
				.requireNonNull(response.getMetadata().get("output"));

			logger.info("transcription result: {}", response.getResult().getOutput());
			logger.info("call response : {}", output);
		}
		catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void asyncCall() {
		try {
			AudioTranscriptionResponse submitResponse = model
				.asyncCall(new AudioTranscriptionPrompt(new UrlResource(TEST_URL),
						DashScopeAudioTranscriptionOptions.builder().withModel("paraformer-v2").build()));

			DashScopeAudioTranscriptionApi.Response.Output submitOutput = Objects
				.requireNonNull(submitResponse.getMetadata().get("output"));
			String taskId = submitOutput.taskId();

			logger.info("async submit response : {}", submitOutput);

			while (true) {
				AudioTranscriptionResponse fetchResponse = model.fetch(taskId);

				DashScopeAudioTranscriptionApi.Response.Output fetchOutput = Objects
					.requireNonNull(fetchResponse.getMetadata().get("output"));
				DashScopeAudioTranscriptionApi.TaskStatus taskStatus = fetchOutput.taskStatus();

				if (taskStatus.equals(DashScopeAudioTranscriptionApi.TaskStatus.SUCCEEDED)) {
					logger.info("async transcription result: {}", fetchResponse.getResult().getOutput());
					break;
				}

				Thread.sleep(1000);
			}

		}
		catch (MalformedURLException | InterruptedException e) {
			throw new DashScopeException(String.valueOf(e));
		}
	}

	@Test
	void stream() {
		CountDownLatch latch = new CountDownLatch(1);

		Flux<AudioTranscriptionResponse> response = model
			.stream(new AudioTranscriptionPrompt(new FileSystemResource(TEST_PATH),
					DashScopeAudioTranscriptionOptions.builder()
						.withModel("paraformer-realtime-v2")
						.withSampleRate(16000)
						.withFormat(DashScopeAudioTranscriptionOptions.AudioFormat.PCM)
						.withDisfluencyRemovalEnabled(false)
						.build()));

		response.doFinally(signal -> {
			latch.countDown();
		}).subscribe(resp -> {
			logger.info("stream transcription result: {}", resp.getResult().getOutput());
		});

		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
