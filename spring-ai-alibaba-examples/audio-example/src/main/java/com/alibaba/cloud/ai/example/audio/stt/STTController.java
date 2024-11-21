/*
 * Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.example.audio.stt;

import java.net.MalformedURLException;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioTranscriptionApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.transcription.AudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.common.DashScopeException;
import jakarta.annotation.Resource;
import reactor.core.publisher.Flux;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/ai/stt")
public class STTController {

	@Resource
	private AudioTranscriptionModel transcriptionModel;

	private static final String DEFAULT_MODEL_1 = "sensevoice-v1";

	private static final String DEFAULT_MODEL_2 = "paraformer-realtime-v2";
	private static final String DEFAULT_MODEL_3 = "paraformer-v2";

	private static final String FILE_PATH = "spring-ai-alibaba-examples/audio-example/src/main/resources/stt/count.pcm";

	private static final String AUDIO_RESOURCES_URL = "https://dashscope.oss-cn-beijing.aliyuncs.com/samples/audio/paraformer/hello_world_female2.wav";

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	@GetMapping
	public DashScopeAudioTranscriptionApi.Response.Output stt() throws MalformedURLException {

		AudioTranscriptionResponse response = transcriptionModel.call(
				new AudioTranscriptionPrompt(
						new UrlResource(AUDIO_RESOURCES_URL),
						DashScopeAudioTranscriptionOptions.builder()
								.withModel(DEFAULT_MODEL_1)
								.build()
				)
		);

		return response.getMetadata().get("output");
	}

	@GetMapping("/stream")
	public String streamSTT() {

		CountDownLatch latch = new CountDownLatch(1);
		StringBuilder stringBuilder = new StringBuilder();

		Flux<AudioTranscriptionResponse> response = transcriptionModel
				.stream(
						new AudioTranscriptionPrompt(
								new FileSystemResource(FILE_PATH),
								DashScopeAudioTranscriptionOptions.builder()
										.withModel(DEFAULT_MODEL_2)
										.withSampleRate(16000)
										.withFormat(DashScopeAudioTranscriptionOptions.AudioFormat.PCM)
										.withDisfluencyRemovalEnabled(false)
										.build()
						)
				);

		response.doFinally(
				signal -> latch.countDown()
		).subscribe(
				resp -> stringBuilder.append(resp.getResult().getOutput())
		);

		try {
			latch.await();
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		return stringBuilder.toString();
	}

	@GetMapping("/async")
	public String asyncSTT() {
		StringBuilder stringBuilder = new StringBuilder();
		CountDownLatch latch = new CountDownLatch(1);

		try {
			AudioTranscriptionResponse submitResponse = transcriptionModel.asyncCall(
					new AudioTranscriptionPrompt(
							new UrlResource(AUDIO_RESOURCES_URL),
							DashScopeAudioTranscriptionOptions.builder()
									.withModel(DEFAULT_MODEL_3)
									.build()
					)
			);

			DashScopeAudioTranscriptionApi.Response.Output submitOutput = Objects.requireNonNull(submitResponse.getMetadata()
					.get("output"));
			String taskId = submitOutput.taskId();

			scheduler.scheduleAtFixedRate(
					() -> checkTaskStatus(taskId, stringBuilder, latch), 0, 1, TimeUnit.SECONDS);
			latch.await();

		}
		catch (MalformedURLException e) {
			throw new DashScopeException("Error in URL format: " + e.getMessage());
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new DashScopeException("Thread was interrupted: " + e.getMessage());
		}
		finally {
			scheduler.shutdown();
		}

		return stringBuilder.toString();
	}

	private void checkTaskStatus(String taskId, StringBuilder stringBuilder, CountDownLatch latch) {

		try {
			AudioTranscriptionResponse fetchResponse = transcriptionModel.fetch(taskId);
			DashScopeAudioTranscriptionApi.Response.Output fetchOutput =
					Objects.requireNonNull(fetchResponse.getMetadata().get("output"));
			DashScopeAudioTranscriptionApi.TaskStatus taskStatus = fetchOutput.taskStatus();

			if (taskStatus.equals(DashScopeAudioTranscriptionApi.TaskStatus.SUCCEEDED)) {
				stringBuilder.append(fetchResponse.getResult().getOutput());
				latch.countDown();
			}
			else if (taskStatus.equals(DashScopeAudioTranscriptionApi.TaskStatus.FAILED)) {
				System.err.println("Transcription failed.");
				latch.countDown();
			}
		}
		catch (Exception e) {
			latch.countDown();
			throw new RuntimeException("Error occurred while checking task status: " + e.getMessage());
		}
	}

}
