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
package com.alibaba.cloud.ai.examples.multimodal.audio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.tool.multimodal.OutputFormat;
import com.alibaba.cloud.ai.graph.agent.tool.multimodal.ToolMultimodalResult;

import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;

/**
 * Service that uses {@link DashScopeAudioSpeechModel} directly for text-to-speech.
 *
 * <p>Supports {@link OutputFormat#url} (save to file, return URI) and
 * {@link OutputFormat#base64} (inline data). Uses {@code call()} when the model supports it
 * (e.g. Qwen TTS REST); falls back to {@code stream()} for CosyVoice / Qwen TTS Realtime.
 */
@Service
public class AudioService {

	private final DashScopeAudioSpeechModel speechModel;

	private final Path outputDir;

	public AudioService(@Qualifier("dashScopeSpeechSynthesisModel") DashScopeAudioSpeechModel speechModel) {
		this.speechModel = speechModel;
		try {
			this.outputDir = Files.createTempDirectory("multimodal-tts-");
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to create TTS output directory", e);
		}
	}

	/**
	 * Synthesize text to speech.
	 * @param text the text to convert
	 * @param voice voice name (e.g. Cherry, Stella), default Cherry
	 * @param outputFormat url (file URI) or base64 (inline data)
	 * @return ToolMultimodalResult with media
	 */
	public ToolMultimodalResult synthesize(String text, String voice, OutputFormat outputFormat) {
		if (text == null || text.isBlank()) {
			return ToolMultimodalResult.of("Error: text cannot be empty.");
		}
		try {
			byte[] audioData = getAudioBytes(text, voice);
			if (audioData == null || audioData.length == 0) {
				return ToolMultimodalResult.of("TTS failed: no audio data returned.");
			}
			var mimeType = MimeTypeUtils.parseMimeType("audio/mpeg");
			if (outputFormat == OutputFormat.base64) {
				return ToolMultimodalResult.builder()
						.text("Audio generated successfully.")
						.media(ToolMultimodalResult.mediaFromBytes(audioData, mimeType))
						.build();
			}
			Path file = outputDir.resolve("tts-" + UUID.randomUUID() + ".mp3");
			Files.write(file, audioData);
			return ToolMultimodalResult.builder()
					.text("Audio generated successfully.")
					.media(ToolMultimodalResult.mediaFromUri(file.toUri(), mimeType))
					.build();
		}
		catch (Exception e) {
			return ToolMultimodalResult.of("TTS failed: " + e.getMessage());
		}
	}

	private byte[] getAudioBytes(String text, String voice) throws Exception {
		var options = DashScopeAudioSpeechOptions.builder()
				.model(DashScopeModel.AudioModel.COSYVOICE_V3_FLASH.getValue())
				// check for available voices, https://help.aliyun.com/zh/model-studio/cosyvoice-voice-list?spm=a2c4g.11186623.help-menu-2400256.d_2_6_0_9.216d5c77ST1d1N
				.voice(voice != null && !voice.isBlank() ? voice : "longanyang")
				.build();
		var prompt = new TextToSpeechPrompt(text, options);
		try {
			TextToSpeechResponse response = speechModel.call(prompt);
			return response.getResult().getOutput();
		}
		catch (IllegalArgumentException e) {
			return collectStreamBytes(speechModel.stream(prompt));
		}
	}

	private byte[] collectStreamBytes(Flux<TextToSpeechResponse> stream) {
		java.util.List<byte[]> chunks = stream
				.filter(r -> r != null && r.getResult() != null && r.getResult().getOutput() != null)
				.map(r -> r.getResult().getOutput())
				.collectList()
				.block();
		if (chunks == null || chunks.isEmpty()) {
			return new byte[0];
		}
		int total = chunks.stream().mapToInt(b -> b.length).sum();
		byte[] result = new byte[total];
		int offset = 0;
		for (byte[] chunk : chunks) {
			System.arraycopy(chunk, 0, result, offset, chunk.length);
			offset += chunk.length;
		}
		return result;
	}
}
