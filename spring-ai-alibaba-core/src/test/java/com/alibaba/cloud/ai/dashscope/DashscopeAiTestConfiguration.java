package com.alibaba.cloud.ai.dashscope;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeAudioApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechModelOpenAPI;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModelOpenAPI;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import com.alibaba.cloud.ai.model.RerankModel;
import com.alibaba.dashscope.audio.asr.transcription.Transcription;
import com.alibaba.dashscope.audio.tts.SpeechSynthesizer;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

@SpringBootConfiguration
public class DashscopeAiTestConfiguration {

	@Bean
	public DashScopeImageApi dashscopeImageApi() {
		return newDashScopeImageApi(getApiKey());
	}

	@Bean
	public DashScopeApi dashscopeApi() {
		return newDashScopeApi(getApiKey());
	}

	@Bean
	public DashScopeAudioApi dashScopeAudioApi() {
		return newDashScopeAudioApi(getApiKey());
	}

	@Bean
	public DashScopeSpeechSynthesisApi dashScopeSpeechSynthesisApi() {
		return newDashScopeSpeechSynthesisApi(getApiKey());
	}

	@Bean
	public DashScopeApi dashscopeChatApi() {
		return newDashScopeChatApi(getApiKey());
	}

	private DashScopeApi newDashScopeChatApi(String apiKey) {
		return new DashScopeApi(DEFAULT_BASE_URL, apiKey, "");
	}

	private DashScopeApi newDashScopeApi(String apiKey) {
		return new DashScopeApi(apiKey);
	}

	private DashScopeAudioApi newDashScopeAudioApi(String apiKey) {
		return new DashScopeAudioApi(apiKey);
	}

	private DashScopeSpeechSynthesisApi newDashScopeSpeechSynthesisApi(String apiKey) {
		return new DashScopeSpeechSynthesisApi(apiKey);
	}

	private DashScopeImageApi newDashScopeImageApi(String apiKey) {
		return new DashScopeImageApi(apiKey);
	}

	private String getApiKey() {
		String apiKey = System.getenv("DASHSCOPE_API_KEY");
		if (!StringUtils.hasText(apiKey)) {
			throw new IllegalArgumentException(
					"You must provide an API key.  Put it in an environment variable under the name DASHSCOPE_API_KEY");
		}
		return apiKey;
	}

	@Bean
	public ChatModel dashscopeChatModel(DashScopeApi dashscopeChatApi) {
		return new DashScopeChatModel(dashscopeChatApi,
				DashScopeChatOptions.builder().withModel(DashScopeApi.DEFAULT_CHAT_MODEL).build());
	}

	@Bean
	public EmbeddingModel dashscopeEmbeddingModel(DashScopeApi dashscopeApi) {
		return new DashScopeEmbeddingModel(dashscopeApi);
	}

	@Bean
	public DashScopeImageModel dashscopeImageModel(DashScopeImageApi dashscopeImageApi) {
		return new DashScopeImageModel(dashscopeImageApi);
	}

	@Bean
	public DashScopeAudioSpeechModelOpenAPI dashscopeAudioSpeechModel(DashScopeAudioApi dashScopeAudioApi,
			SpeechSynthesizer speechSynthesizer) {
		return new DashScopeAudioSpeechModelOpenAPI(dashScopeAudioApi,
				DashScopeAudioSpeechOptions.builder().withModel("sambert-zhichu-v1").build());
	}

	@Bean
	public DashScopeSpeechSynthesisModel dashscopeAudioSpeechModel(
			DashScopeSpeechSynthesisApi dashScopeSpeechSynthesisApi) {
		return new DashScopeSpeechSynthesisModel(dashScopeSpeechSynthesisApi,
				DashScopeSpeechSynthesisOptions.builder().withModel("cosyvoice-v1").withVoice("longhua").build());
	}

	@Bean
	public DashScopeAudioTranscriptionModelOpenAPI dashscopeAudioTranscriptionModel(DashScopeAudioApi dashScopeAudioApi,
			Transcription transcription) {
		return new DashScopeAudioTranscriptionModelOpenAPI(dashScopeAudioApi,
				DashScopeAudioTranscriptionOptions.builder().withModel("paraformer-v2").build());
	}

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public SpeechSynthesizer speechSynthesizer() {
		return new SpeechSynthesizer();
	}

	@Bean
	@ConditionalOnMissingBean
	public Transcription transcription() {
		return new Transcription();
	}

	@Bean
	@ConditionalOnMissingBean
	public RerankModel dashscopeRerankModel(DashScopeApi dashscopeApi) {
		return new DashScopeRerankModel(dashscopeApi);
	}

}
