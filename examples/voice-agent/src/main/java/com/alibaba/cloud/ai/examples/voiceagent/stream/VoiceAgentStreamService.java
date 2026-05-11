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
package com.alibaba.cloud.ai.examples.voiceagent.stream;

import java.util.Map;

import com.alibaba.cloud.ai.dashscope.audio.transcription.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.tts.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.transcription.AudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.tts.StreamingInputTextToSpeechModel;
import com.alibaba.cloud.ai.dashscope.spec.DashScopeModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.transcription.AudioTranscriptionResponse;
import org.springframework.ai.audio.tts.Speech;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Streaming voice agent pipeline: Agent stream → TTS stream.
 *
 * <p>Voice Agent: agent streams text chunks, TTS consumes them via
 * {@link StreamingInputTextToSpeechModel} when available for lower latency.
 *
 * <p>Emits {@link VoiceAgentEvent}: agent_chunk, tool_call, tool_result, agent_end, tts_chunk.
 */
@Service
public class VoiceAgentStreamService {

	private final ReactAgent sandwichAgent;

	private final TextToSpeechModel ttsModel;

	private final AudioTranscriptionModel sttModel;

	public VoiceAgentStreamService(@Qualifier("sandwichAgent") ReactAgent sandwichAgent,
			@Qualifier("dashScopeSpeechSynthesisModel") TextToSpeechModel ttsModel,
			@Autowired(required = false) AudioTranscriptionModel sttModel) {
		this.sandwichAgent = sandwichAgent;
		this.ttsModel = ttsModel;
		this.sttModel = sttModel;
	}

	/**
	 * Stream voice agent events for text input: agent_chunk, tool_call, tool_result, agent_end,
	 * tts_chunk. Uses streaming text → TTS when TTS model implements
	 * {@link StreamingInputTextToSpeechModel} (e.g. DashScope CosyVoice).
	 */
	public Flux<VoiceAgentEvent> streamWithText(String text) throws GraphRunnerException {
		if (ttsModel instanceof StreamingInputTextToSpeechModel streamingTts) {
			return streamWithTextAndStreamingTts(text, streamingTts);
		}
		return streamWithTextAndFullTextTts(text);
	}

	/**
	 * Agent stream feeds text into TTS stream (streaming input); agent events and TTS events are
	 * merged and emitted to the client.
	 */
	private Flux<VoiceAgentEvent> streamWithTextAndStreamingTts(String text,
			StreamingInputTextToSpeechModel streamingTts) throws GraphRunnerException {
		Sinks.Many<String> textSink = Sinks.many().unicast().onBackpressureBuffer();
		Flux<String> agentTextFlux = textSink.asFlux();

		DashScopeAudioSpeechOptions ttsOptions = DashScopeAudioSpeechOptions.builder()
				.model(DashScopeModel.AudioModel.COSYVOICE_V3_FLASH.getValue())
				.textType("PlainText")
				.voice("longanyang")
				.format("mp3")
				.sampleRate(22050)
				.build();

		Flux<VoiceAgentEvent> agentEvents = sandwichAgent.streamMessages(text).flatMap(msg -> {
			if (msg instanceof AssistantMessage am) {
				if (am.hasToolCalls()) {
					return Flux.fromIterable(am.getToolCalls())
							.map(tc -> VoiceAgentEvent.ToolCallEvent.create(tc.id(), tc.name(),
									parseArgs(tc.arguments())));
				}
				String chunk = am.getText();
				if (chunk != null && !chunk.isBlank()) {
					textSink.tryEmitNext(chunk);
					return Flux.just(VoiceAgentEvent.AgentChunkEvent.create(chunk));
				}
			}
			else if (msg instanceof ToolResponseMessage trm) {
				return Flux.fromIterable(trm.getResponses())
						.map(r -> VoiceAgentEvent.ToolResultEvent.create(r.id(), r.name(),
								r.responseData() != null ? r.responseData() : ""));
			}
			return Flux.<VoiceAgentEvent>empty();
		}).doOnComplete(() -> textSink.tryEmitComplete());

		Flux<VoiceAgentEvent> agentEnd = Flux.just(VoiceAgentEvent.AgentEndEvent.create());

		Flux<VoiceAgentEvent> ttsEvents = streamingTts.stream(agentTextFlux, ttsOptions)
				.map(VoiceAgentStreamService::extractAudio)
				.filter(bytes -> bytes != null && bytes.length > 0)
				.map(VoiceAgentEvent.TtsChunkEvent::create);

		return Flux.merge(agentEvents.concatWith(agentEnd), ttsEvents).cast(VoiceAgentEvent.class);
	}

	/**
	 * Fallback when TTS does not support streaming input: buffer full agent text, then stream TTS.
	 */
	private Flux<VoiceAgentEvent> streamWithTextAndFullTextTts(String text) throws GraphRunnerException {
		Flux<Message> messageFlux = sandwichAgent.streamMessages(text);
		StringBuilder agentTextBuffer = new StringBuilder();

		Flux<VoiceAgentEvent> agentEvents = messageFlux.flatMap(msg -> {
			if (msg instanceof AssistantMessage am) {
				if (am.hasToolCalls()) {
					return Flux.fromIterable(am.getToolCalls())
							.map(tc -> VoiceAgentEvent.ToolCallEvent.create(tc.id(), tc.name(),
									parseArgs(tc.arguments())));
				}
				String chunk = am.getText();
				if (chunk != null && !chunk.isBlank()) {
					agentTextBuffer.append(chunk);
					return Flux.just(VoiceAgentEvent.AgentChunkEvent.create(chunk));
				}
			}
			else if (msg instanceof ToolResponseMessage trm) {
				return Flux.fromIterable(trm.getResponses())
						.map(r -> VoiceAgentEvent.ToolResultEvent.create(r.id(), r.name(),
								r.responseData() != null ? r.responseData() : ""));
			}
			return Flux.<VoiceAgentEvent>empty();
		});

		Flux<VoiceAgentEvent> agentEnd = Flux.just(VoiceAgentEvent.AgentEndEvent.create());

		Flux<VoiceAgentEvent> ttsEvents = Flux.defer(() -> {
			String fullText = agentTextBuffer.toString();
			if (fullText.isBlank()) {
				return Flux.<VoiceAgentEvent>empty();
			}
			var prompt = new TextToSpeechPrompt(fullText,
					DashScopeAudioSpeechOptions.builder()
							.model(DashScopeModel.AudioModel.COSYVOICE_V1.getValue())
							.voice("Cherry")
							.build());
			Flux<TextToSpeechResponse> ttsFlux = ttsModel.stream(prompt);
			return ttsFlux.map(VoiceAgentStreamService::extractAudio)
					.filter(bytes -> bytes != null && bytes.length > 0)
					.map(VoiceAgentEvent.TtsChunkEvent::create);
		});

		return agentEvents.concatWith(agentEnd).concatWith(ttsEvents);
	}

	/**
	 * Semi-streaming: audio input → STT (sync) → Agent stream → TTS stream. Emits stt_output,
	 * agent_chunk, tool_call, tool_result, agent_end, tts_chunk.
	 */
	public Flux<VoiceAgentEvent> streamWithAudio(byte[] audioBytes) throws GraphRunnerException {
		if (sttModel == null) {
			return Flux.error(new IllegalStateException("AudioTranscriptionModel not available."));
		}
		byte[] wavBytes = ensureWavFormat(audioBytes);
		Resource resource = new ByteArrayResource(wavBytes);
		String transcript = transcribe(resource);
		Flux<VoiceAgentEvent> sttEvent = Flux.just(VoiceAgentEvent.SttOutputEvent.create(transcript));
		return sttEvent.concatWith(streamWithText(transcript));
	}

	/**
	 * Stream TTS only: text → audio chunks. Used by text WebSocket when ttsOnly=true.
	 */
	public Flux<byte[]> streamTts(String text) {
		if (text == null || text.isBlank()) {
			return Flux.empty();
		}
		var prompt = new TextToSpeechPrompt(text,
				DashScopeAudioSpeechOptions.builder()
						.model(DashScopeModel.AudioModel.COSYVOICE_V1.getValue())
						.voice("Cherry")
						.build());
		return ttsModel.stream(prompt)
				.map(VoiceAgentStreamService::extractAudio)
				.filter(bytes -> bytes != null && bytes.length > 0);
	}

	private static byte[] extractAudio(TextToSpeechResponse r) {
		if (r == null || r.getResult() == null) {
			return null;
		}
		Speech speech = r.getResult();
		return speech != null ? speech.getOutput() : null;
	}

	private String transcribe(Resource audioResource) {
		AudioTranscriptionResponse response = sttModel.call(new AudioTranscriptionPrompt(audioResource,
				DashScopeAudioTranscriptionOptions.builder()
						.model(DashScopeModel.AudioModel.PARAFORMER_V2.getValue())
						.build()));
		return response.getResult().getOutput();
	}

	/**
	 * If input is raw PCM (16kHz, 16-bit, mono), prepend WAV header for DashScope Paraformer.
	 */
	private static byte[] ensureWavFormat(byte[] audioBytes) {
		if (audioBytes == null || audioBytes.length == 0) {
			return audioBytes;
		}
		if (audioBytes.length >= 4 && audioBytes[0] == 'R' && audioBytes[1] == 'I' && audioBytes[2] == 'F'
				&& audioBytes[3] == 'F') {
			return audioBytes;
		}
		return prependWavHeader(audioBytes, 16000, 1, 16);
	}

	private static byte[] prependWavHeader(byte[] pcmData, int sampleRate, int numChannels, int bitsPerSample) {
		int dataLen = pcmData.length;
		int byteRate = sampleRate * numChannels * bitsPerSample / 8;
		int blockAlign = numChannels * bitsPerSample / 8;
		int chunkSize = 36 + dataLen;
		byte[] wav = new byte[44 + dataLen];
		wav[0] = 'R';
		wav[1] = 'I';
		wav[2] = 'F';
		wav[3] = 'F';
		wav[4] = (byte) (chunkSize & 0xff);
		wav[5] = (byte) ((chunkSize >> 8) & 0xff);
		wav[6] = (byte) ((chunkSize >> 16) & 0xff);
		wav[7] = (byte) ((chunkSize >> 24) & 0xff);
		wav[8] = 'W';
		wav[9] = 'A';
		wav[10] = 'V';
		wav[11] = 'E';
		wav[12] = 'f';
		wav[13] = 'm';
		wav[14] = 't';
		wav[15] = ' ';
		wav[16] = 16;
		wav[17] = 0;
		wav[18] = 0;
		wav[19] = 0;
		wav[20] = 1;
		wav[21] = 0;
		wav[22] = (byte) numChannels;
		wav[23] = 0;
		wav[24] = (byte) (sampleRate & 0xff);
		wav[25] = (byte) ((sampleRate >> 8) & 0xff);
		wav[26] = (byte) ((sampleRate >> 16) & 0xff);
		wav[27] = (byte) ((sampleRate >> 24) & 0xff);
		wav[28] = (byte) (byteRate & 0xff);
		wav[29] = (byte) ((byteRate >> 8) & 0xff);
		wav[30] = (byte) ((byteRate >> 16) & 0xff);
		wav[31] = (byte) ((byteRate >> 24) & 0xff);
		wav[32] = (byte) blockAlign;
		wav[33] = 0;
		wav[34] = (byte) bitsPerSample;
		wav[35] = 0;
		wav[36] = 'd';
		wav[37] = 'a';
		wav[38] = 't';
		wav[39] = 'a';
		wav[40] = (byte) (dataLen & 0xff);
		wav[41] = (byte) ((dataLen >> 8) & 0xff);
		wav[42] = (byte) ((dataLen >> 16) & 0xff);
		wav[43] = (byte) ((dataLen >> 24) & 0xff);
		System.arraycopy(pcmData, 0, wav, 44, dataLen);
		return wav;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> parseArgs(String arguments) {
		if (arguments == null || arguments.isBlank()) {
			return Map.of();
		}
		try {
			return new com.fasterxml.jackson.databind.ObjectMapper().readValue(arguments, Map.class);
		}
		catch (Exception e) {
			return Map.of("raw", arguments);
		}
	}
}
