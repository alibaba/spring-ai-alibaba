/*
 * Copyright 2023 - 2024 the original author or authors.
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
package com.alibaba.cloud.ai.dashscope.api;

import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.ai.retry.RetryUtils;
import org.springframework.ai.util.api.ApiUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Turn audio into text or text into audio. Based on <a href=
 * "https://help.aliyun.com/zh/dashscope/developer-reference/quick-start-13">DashScope
 * Audio</a>
 */
public class DashScopeAudioApi {

	private final RestClient restClient;

	private final WebClient webClient;

	/**
	 * Create a new audio api.
	 * @param dashScopeToken DashScope apiKey.
	 */
	public DashScopeAudioApi(String dashScopeToken) {
		this(DashScopeApiConstants.DEFAULT_BASE_URL, dashScopeToken, RestClient.builder(), WebClient.builder(),
				RetryUtils.DEFAULT_RESPONSE_ERROR_HANDLER);
	}

	/**
	 * Create a new chat completion api.
	 * @param baseUrl api base URL.
	 * @param dashScopeToken DashScope apiKey.
	 * @param restClientBuilder RestClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	public DashScopeAudioApi(String baseUrl, String dashScopeToken, RestClient.Builder restClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		this.restClient = restClientBuilder.baseUrl(baseUrl).defaultHeaders(headers -> {
			headers.setBearerAuth(dashScopeToken);
		}).defaultStatusHandler(responseErrorHandler).build();

		this.webClient = WebClient.builder().baseUrl(baseUrl).defaultHeaders(headers -> {
			headers.setBearerAuth(dashScopeToken);
		}).defaultHeaders(ApiUtils.getJsonContentHeaders(dashScopeToken)).build();
	}

	/**
	 * Create n new chat completion api.
	 * @param baseUrl api base URL.
	 * @param apiKey DashScope apiKey.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	public DashScopeAudioApi(String baseUrl, String apiKey, RestClient.Builder restClientBuilder,
			WebClient.Builder webClientBuilder, ResponseErrorHandler responseErrorHandler) {

		this(baseUrl, apiKey, CollectionUtils.toMultiValueMap(Map.of()), restClientBuilder, webClientBuilder,
				responseErrorHandler);
	}

	/**
	 * Create n new chat completion api.
	 * @param baseUrl api base URL.
	 * @param apiKey DashScope apiKey.
	 * @param headers the http headers to use.
	 * @param restClientBuilder RestClient builder.
	 * @param webClientBuilder WebClient builder.
	 * @param responseErrorHandler Response error handler.
	 */
	public DashScopeAudioApi(String baseUrl, String apiKey, MultiValueMap<String, String> headers,
			RestClient.Builder restClientBuilder, WebClient.Builder webClientBuilder,
			ResponseErrorHandler responseErrorHandler) {

		// @formatter:off
		this.restClient = restClientBuilder
			.baseUrl(baseUrl)
			.defaultHeaders(h -> {
				h.setBearerAuth(apiKey);
				h.addAll(headers);
			})
			.defaultStatusHandler(responseErrorHandler).build();

		this.webClient = webClientBuilder
			.baseUrl(baseUrl)
			.defaultHeaders(h -> {
				h.setBearerAuth(apiKey);
				h.addAll(headers);
			})
			.defaultHeaders(ApiUtils.getJsonContentHeaders(apiKey)).build();
		// @formatter:on
	}

	/**
	 * TTS is an AI model that converts text to natural sounding spoken text. Check the
	 * reference below for the available TTS models.:
	 * <a href="https://help.aliyun.com/zh/dashscope/model-list">TTS</a>
	 */
	public enum TtsModel {

		// @formatter:off
		/**
		 * Standard male voice by default.
		 */
		@JsonProperty("tts-1") TTS_1("sambert-zhide-v1");

		public final String value;

		TtsModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

	}

	/**
	 * Request to generates audio from the input text. Reference:
	 * <a href="https://help.aliyun.com/zh/dashscope/api-details-14">Create
	 * Speech</a>
	 *
	 * @param model The model to use for generating the audio. One of the available Sambert
	 * models like 'sambert-zhide-v1', find all available models here https://help.aliyun.com/zh/dashscope/model-list
	 * @param input The input text to synthesize. Must be at most 4096 tokens long.
	 * @param voice The voice to use for synthesis. One of the available voices for the
	 * chosen model: 'alloy', 'echo', 'fable', 'onyx', 'nova', and 'shimmer'.
	 * @param responseFormat The format to audio in. Supported formats are mp3, opus, aac,
	 * and flac. Defaults to mp3.
	 * @param speed The speed of the voice synthesis. The acceptable range is from 0.25
	 * (slowest) to 4.0 (fastest).
	 */
	@JsonInclude(Include.NON_NULL)
	public record SpeechRequest(
	// @formatter:off
		@JsonProperty("model") String model,
		@JsonProperty("input") String input,
		@JsonProperty("voice") Voice voice,
		@JsonProperty("response_format") AudioResponseFormat responseFormat,
		@JsonProperty("speed") Float speed) {
		// @formatter:on

		/**
		 * The voice to use for synthesis.
		 */
		public enum Voice {

		// @formatter:off
			@JsonProperty("alloy") ALLOY("alloy"),
			@JsonProperty("echo") ECHO("echo"),
			@JsonProperty("fable") FABLE("fable"),
			@JsonProperty("onyx") ONYX("onyx"),
			@JsonProperty("nova") NOVA("nova"),
			@JsonProperty("shimmer") SHIMMER("shimmer");
			// @formatter:on

			public final String value;

			private Voice(String value) {
				this.value = value;
			}

			public String getValue() {
				return this.value;
			}

		}

		/**
		 * The format to audio in. Supported formats are mp3, opus, aac, and flac.
		 * Defaults to mp3.
		 */
		public enum AudioResponseFormat {

			// @formatter:off
			@JsonProperty("mp3") MP3("mp3"),
			@JsonProperty("opus") OPUS("opus"),
			@JsonProperty("aac") AAC("aac"),
			@JsonProperty("flac") FLAC("flac");
			// @formatter:on

			public final String value;

			AudioResponseFormat(String value) {
				this.value = value;
			}

			public String getValue() {
				return this.value;
			}

		}

		public static Builder builder() {
			return new Builder();
		}

		/**
		 * Builder for the SpeechRequest.
		 */
		public static class Builder {

			private String model = TtsModel.TTS_1.getValue();

			private String input;

			private Voice voice;

			private AudioResponseFormat responseFormat = AudioResponseFormat.MP3;

			private Float speed;

			public Builder withModel(String model) {
				this.model = model;
				return this;
			}

			public Builder withInput(String input) {
				this.input = input;
				return this;
			}

			public Builder withVoice(Voice voice) {
				this.voice = voice;
				return this;
			}

			public Builder withResponseFormat(AudioResponseFormat responseFormat) {
				this.responseFormat = responseFormat;
				return this;
			}

			public Builder withSpeed(Float speed) {
				this.speed = speed;
				return this;
			}

			public SpeechRequest build() {
				Assert.hasText(model, "model must not be empty");
				Assert.hasText(input, "input must not be empty");

				return new SpeechRequest(this.model, this.input, this.voice, this.responseFormat, this.speed);
			}

		}
	}

	/**
	 * <a href="https://platform.openai.com/docs/models/whisper">Whisper</a> is a
	 * general-purpose speech recognition model. It is trained on a large dataset of
	 * diverse audio and is also a multi-task model that can perform multilingual speech
	 * recognition as well as speech translation and language identification. The Whisper
	 * v2-large model is currently available through our API with the whisper-1 model
	 * name.
	 */
	public enum WhisperModel {

		// @formatter:off
		@JsonProperty("whisper-1") WHISPER_1("whisper-1");
		// @formatter:on

		public final String value;

		WhisperModel(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

	}

	/**
	 * Request to transcribe an audio file to text. Reference: <a href=
	 * "https://platform.openai.com/docs/api-reference/audio/createTranscription">Create
	 * Transcription</a>
	 *
	 * @param file The audio file to transcribe. Must be a valid audio file type.
	 * @param model ID of the model to use. Only whisper-1 is currently available.
	 * @param language The language of the input audio. Supplying the input language in
	 * ISO-639-1 format will improve accuracy and latency.
	 * @param prompt An optional text to guide the model's style or continue a previous
	 * audio segment. The prompt should match the audio language.
	 * @param responseFormat The format of the transcript output, in one of these options:
	 * json, text, srt, verbose_json, or vtt. Defaults to json.
	 * @param temperature The sampling temperature, between 0 and 1. Higher values like
	 * 0.8 will make the output more random, while lower values like 0.2 will make it more
	 * focused and deterministic. If set to 0, the model will use log probability to
	 * automatically increase the temperature until certain thresholds are hit.
	 * @param granularityType The timestamp granularities to populate for this
	 * transcription. response_format must be set verbose_json to use timestamp
	 * granularities. Either or both of these options are supported: word, or segment.
	 * Note: There is no additional latency for segment timestamps, but generating word
	 * timestamps incurs additional latency.
	 */
	@JsonInclude(Include.NON_NULL)
	public record TranscriptionRequest(
	// @formatter:off
		@JsonProperty("file") byte[] file,
		@JsonProperty("model") String model,
		@JsonProperty("language") String language,
		@JsonProperty("prompt") String prompt,
		@JsonProperty("response_format") TranscriptResponseFormat responseFormat,
		@JsonProperty("temperature") Float temperature,
		@JsonProperty("timestamp_granularities") GranularityType granularityType) {
		// @formatter:on

		public enum GranularityType {

			// @formatter:off
			@JsonProperty("word") WORD("word"),
			@JsonProperty("segment") SEGMENT("segment");
			// @formatter:on

			public final String value;

			GranularityType(String value) {
				this.value = value;
			}

			public String getValue() {
				return this.value;
			}

		}

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private byte[] file;

			private String model = WhisperModel.WHISPER_1.getValue();

			private String language;

			private String prompt;

			private TranscriptResponseFormat responseFormat = TranscriptResponseFormat.JSON;

			private Float temperature;

			private GranularityType granularityType;

			public Builder withFile(byte[] file) {
				this.file = file;
				return this;
			}

			public Builder withModel(String model) {
				this.model = model;
				return this;
			}

			public Builder withLanguage(String language) {
				this.language = language;
				return this;
			}

			public Builder withPrompt(String prompt) {
				this.prompt = prompt;
				return this;
			}

			public Builder withResponseFormat(TranscriptResponseFormat response_format) {
				this.responseFormat = response_format;
				return this;
			}

			public Builder withTemperature(Float temperature) {
				this.temperature = temperature;
				return this;
			}

			public Builder withGranularityType(GranularityType granularityType) {
				this.granularityType = granularityType;
				return this;
			}

			public TranscriptionRequest build() {
				Assert.notNull(this.file, "file must not be null");
				Assert.hasText(this.model, "model must not be empty");
				Assert.notNull(this.responseFormat, "response_format must not be null");

				return new TranscriptionRequest(this.file, this.model, this.language, this.prompt, this.responseFormat,
						this.temperature, this.granularityType);
			}

		}
	}

	/**
	 * The format of the transcript and translation outputs, in one of these options:
	 * json, text, srt, verbose_json, or vtt. Defaults to json.
	 */
	public enum TranscriptResponseFormat {

		// @formatter:off
		@JsonProperty("json") JSON("json", StructuredResponse.class),
		@JsonProperty("text") TEXT("text", String.class),
		@JsonProperty("srt") SRT("srt", String.class),
		@JsonProperty("verbose_json") VERBOSE_JSON("verbose_json", StructuredResponse.class),
		@JsonProperty("vtt") VTT("vtt", String.class);
		// @formatter:on

		public final String value;

		public final Class<?> responseType;

		public boolean isJsonType() {
			return this == JSON || this == VERBOSE_JSON;
		}

		TranscriptResponseFormat(String value, Class<?> responseType) {
			this.value = value;
			this.responseType = responseType;
		}

		public String getValue() {
			return this.value;
		}

		public Class<?> getResponseType() {
			return this.responseType;
		}

	}

	/**
	 * Request to translate an audio file to English.
	 *
	 * @param file The audio file object (not file name) to translate, in one of these
	 * formats: flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, or webm.
	 * @param model ID of the model to use. Only whisper-1 is currently available.
	 * @param prompt An optional text to guide the model's style or continue a previous
	 * audio segment. The prompt should be in English.
	 * @param responseFormat The format of the transcript output, in one of these options:
	 * json, text, srt, verbose_json, or vtt.
	 * @param temperature The sampling temperature, between 0 and 1. Higher values like
	 * 0.8 will make the output more random, while lower values like 0.2 will make it more
	 * focused and deterministic. If set to 0, the model will use log probability to
	 * automatically increase the temperature until certain thresholds are hit.
	 */
	@JsonInclude(Include.NON_NULL)
	public record TranslationRequest(
	// @formatter:off
		@JsonProperty("file") byte[] file,
		@JsonProperty("model") String model,
		@JsonProperty("prompt") String prompt,
		@JsonProperty("response_format") TranscriptResponseFormat responseFormat,
		@JsonProperty("temperature") Float temperature) {
		// @formatter:on

		public static Builder builder() {
			return new Builder();
		}

		public static class Builder {

			private byte[] file;

			private String model = WhisperModel.WHISPER_1.getValue();

			private String prompt;

			private TranscriptResponseFormat responseFormat = TranscriptResponseFormat.JSON;

			private Float temperature;

			public Builder withFile(byte[] file) {
				this.file = file;
				return this;
			}

			public Builder withModel(String model) {
				this.model = model;
				return this;
			}

			public Builder withPrompt(String prompt) {
				this.prompt = prompt;
				return this;
			}

			public Builder withResponseFormat(TranscriptResponseFormat responseFormat) {
				this.responseFormat = responseFormat;
				return this;
			}

			public Builder withTemperature(Float temperature) {
				this.temperature = temperature;
				return this;
			}

			public TranslationRequest build() {
				Assert.notNull(file, "file must not be null");
				Assert.hasText(model, "model must not be empty");
				Assert.notNull(responseFormat, "response_format must not be null");

				return new TranslationRequest(this.file, this.model, this.prompt, this.responseFormat,
						this.temperature);
			}

		}
	}

	/**
	 * The <a href=
	 * "https://platform.openai.com/docs/api-reference/audio/verbose-json-object">Transcription
	 * Object </a> represents a verbose json transcription response returned by model,
	 * based on the provided input.
	 *
	 * @param language The language of the transcribed text.
	 * @param duration The duration of the audio in seconds.
	 * @param text The transcribed text.
	 * @param words The extracted words and their timestamps.
	 * @param segments The segments of the transcribed text and their corresponding
	 * details.
	 */
	@JsonInclude(Include.NON_NULL)
	public record StructuredResponse(
	// @formatter:off
		@JsonProperty("language") String language,
		@JsonProperty("duration") Float duration,
		@JsonProperty("text") String text,
		@JsonProperty("words") List<Word> words,
		@JsonProperty("segments") List<Segment> segments) {
		// @formatter:on

		/**
		 * Extracted word and it corresponding timestamps.
		 *
		 * @param word The text content of the word.
		 * @param start The start time of the word in seconds.
		 * @param end The end time of the word in seconds.
		 */
		@JsonInclude(Include.NON_NULL)
		public record Word(
		// @formatter:off
			@JsonProperty("word") String word,
			@JsonProperty("start") Float start,
			@JsonProperty("end") Float end) {
			// @formatter:on
		}

		/**
		 * Segment of the transcribed text and its corresponding details.
		 *
		 * @param id Unique identifier of the segment.
		 * @param seek Seek offset of the segment.
		 * @param start Start time of the segment in seconds.
		 * @param end End time of the segment in seconds.
		 * @param text The text content of the segment.
		 * @param tokens Array of token IDs for the text content.
		 * @param temperature Temperature parameter used for generating the segment.
		 * @param avgLogprob Average logprob of the segment. If the value is lower than
		 * -1, consider the logprobs failed.
		 * @param compressionRatio Compression ratio of the segment. If the value is
		 * greater than 2.4, consider the compression failed.
		 * @param noSpeechProb Probability of no speech in the segment. If the value is
		 * higher than 1.0 and the avg_logprob is below -1, consider this segment silent.
		 */
		@JsonInclude(Include.NON_NULL)
		public record Segment(
		// @formatter:off
			@JsonProperty("id") Integer id,
			@JsonProperty("seek") Integer seek,
			@JsonProperty("start") Float start,
			@JsonProperty("end") Float end,
			@JsonProperty("text") String text,
			@JsonProperty("tokens") List<Integer> tokens,
			@JsonProperty("temperature") Float temperature,
			@JsonProperty("avg_logprob") Float avgLogprob,
			@JsonProperty("compression_ratio") Float compressionRatio,
			@JsonProperty("no_speech_prob") Float noSpeechProb) {
			// @formatter:on
		}
	}

	/**
	 * Request to generates audio from the input text.
	 * @param requestBody The request body.
	 * @return Response entity containing the audio binary.
	 */
	public ResponseEntity<byte[]> createSpeech(SpeechRequest requestBody) {
		return this.restClient.post().uri("/v1/audio/speech").body(requestBody).retrieve().toEntity(byte[].class);
	}

	/**
	 * Streams audio generated from the input text.
	 *
	 * This method sends a POST request to the DashScope API to generate audio from the
	 * provided text. The audio is streamed back as a Flux of ResponseEntity objects, each
	 * containing a byte array of the audio data.
	 * @param requestBody The request body containing the details for the audio
	 * generation, such as the input text, model, voice, and response format.
	 * @return A Flux of ResponseEntity objects, each containing a byte array of the audio
	 * data.
	 */
	public Flux<ResponseEntity<byte[]>> stream(SpeechRequest requestBody) {

		return webClient.post()
			.uri("/v1/audio/speech")
			.body(Mono.just(requestBody), SpeechRequest.class)
			.accept(MediaType.APPLICATION_OCTET_STREAM)
			.exchangeToFlux(clientResponse -> {
				HttpHeaders headers = clientResponse.headers().asHttpHeaders();
				return clientResponse.bodyToFlux(byte[].class)
					.map(bytes -> ResponseEntity.ok().headers(headers).body(bytes));
			});
	}

	/**
	 * Transcribes audio into the input language.
	 * @param requestBody The request body.
	 * @return Response entity containing the transcribed text in either json or text
	 * format.
	 */
	public ResponseEntity<?> createTranscription(TranscriptionRequest requestBody) {
		return createTranscription(requestBody, requestBody.responseFormat().getResponseType());
	}

	/**
	 * Transcribes audio into the input language. The response type is specified by the
	 * responseType parameter.
	 * @param <T> The response type.
	 * @param requestBody The request body.
	 * @param responseType The response type class.
	 * @return Response entity containing the transcribed text in the responseType format.
	 */
	public <T> ResponseEntity<T> createTranscription(TranscriptionRequest requestBody, Class<T> responseType) {

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("file", new ByteArrayResource(requestBody.file()) {
			@Override
			public String getFilename() {
				return "audio.webm";
			}
		});
		multipartBody.add("model", requestBody.model());
		multipartBody.add("language", requestBody.language());
		multipartBody.add("prompt", requestBody.prompt());
		multipartBody.add("response_format", requestBody.responseFormat().getValue());
		multipartBody.add("temperature", requestBody.temperature());
		if (requestBody.granularityType() != null) {
			Assert.isTrue(requestBody.responseFormat() == TranscriptResponseFormat.VERBOSE_JSON,
					"response_format must be set to verbose_json to use timestamp granularities.");
			multipartBody.add("timestamp_granularities[]", requestBody.granularityType().getValue());
		}

		return this.restClient.post()
			.uri("/v1/audio/transcriptions")
			.body(multipartBody)
			.retrieve()
			.toEntity(responseType);
	}

	/**
	 * Translates audio into English.
	 * @param requestBody The request body.
	 * @return Response entity containing the transcribed text in either json or text
	 * format.
	 */
	public ResponseEntity<?> createTranslation(TranslationRequest requestBody) {
		return createTranslation(requestBody, requestBody.responseFormat().getResponseType());
	}

	/**
	 * Translates audio into English. The response type is specified by the responseType
	 * parameter.
	 * @param <T> The response type.
	 * @param requestBody The request body.
	 * @param responseType The response type class.
	 * @return Response entity containing the transcribed text in the responseType format.
	 */
	public <T> ResponseEntity<T> createTranslation(TranslationRequest requestBody, Class<T> responseType) {

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("file", new ByteArrayResource(requestBody.file()) {
			@Override
			public String getFilename() {
				return "audio.webm";
			}
		});
		multipartBody.add("model", requestBody.model());
		multipartBody.add("prompt", requestBody.prompt());
		multipartBody.add("response_format", requestBody.responseFormat().getValue());
		multipartBody.add("temperature", requestBody.temperature());

		return this.restClient.post()
			.uri("/v1/audio/translations")
			.body(multipartBody)
			.retrieve()
			.toEntity(responseType);
	}

}
