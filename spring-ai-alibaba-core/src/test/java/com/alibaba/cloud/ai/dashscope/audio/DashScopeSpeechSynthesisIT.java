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
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;

@TestPropertySource("classpath:application.yml")
@SpringBootTest(classes = DashScopeAutoConfiguration.class)
public class DashScopeSpeechSynthesisIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeSpeechSynthesisIT.class);

	@Autowired
	private DashScopeSpeechSynthesisModel model;

	@Test
	void call() {
		SpeechSynthesisResponse response = model.call(new SpeechSynthesisPrompt("白日依山尽，黄河入海流。"));

		// play pcm: ffplay -f s16le -ar 48000 -ch_layout mono output-stream.pcm
		File file = new File("/Users/zhiyi/Downloads/output-call.mp3");
		try (FileOutputStream fos = new FileOutputStream(file)) {
			try {
				ByteBuffer byteBuffer = response.getResult().getOutput().getAudio();
				logger.debug("write call audio to file: remain={}, limit={}", byteBuffer.remaining(),
						byteBuffer.limit());
				fos.write(byteBuffer.array());
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void stream() {
		Flux<SpeechSynthesisResponse> response = model.stream(new SpeechSynthesisPrompt("白日依山尽，黄河入海流。"));

		CountDownLatch latch = new CountDownLatch(1);
		// play pcm: ffplay -f s16le -ar 48000 -ch_layout mono output-stream.pcm
		File file = new File("/Users/zhiyi/Downloads/output-stream.mp3");
		try (FileOutputStream fos = new FileOutputStream(file)) {
			response.doFinally(signal -> latch.countDown()).subscribe(synthesisResponse -> {
				ByteBuffer byteBuffer = synthesisResponse.getResult().getOutput().getAudio();
				// fos.write(byteBuffer.array());
				byte[] bytes = new byte[byteBuffer.remaining()];
				byteBuffer.get(bytes);
				try {
					logger.debug("write stream audio to file: remain={}, limit={}", byteBuffer.remaining(),
							byteBuffer.limit());
					fos.write(bytes);
				}
				catch (IOException e) {
					throw new RuntimeException(e);
				}
			});

			latch.await();
		}
		catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

}
