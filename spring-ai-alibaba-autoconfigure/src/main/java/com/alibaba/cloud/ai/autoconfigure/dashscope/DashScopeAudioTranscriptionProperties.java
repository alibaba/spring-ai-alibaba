package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * @author xYLiu
 * @author yuluo
 * @author kevinlin09
 */

@ConfigurationProperties(DashScopeAudioTranscriptionProperties.CONFIG_PREFIX)
public class DashScopeAudioTranscriptionProperties extends DashScopeParentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.audio.transcription";

	@NestedConfigurationProperty
	private DashScopeAudioTranscriptionOptions options = DashScopeAudioTranscriptionOptions.builder().build();

	public DashScopeAudioTranscriptionProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
	}

	public DashScopeAudioTranscriptionOptions getOptions() {
		return this.options;
	}

	public void setOptions(DashScopeAudioTranscriptionOptions options) {
		this.options = options;
	}

}
