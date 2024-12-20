package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * @author kevinlin09
 */

@ConfigurationProperties(DashScopeSpeechSynthesisProperties.CONFIG_PREFIX)
public class DashScopeSpeechSynthesisProperties extends DashScopeParentProperties {

	/**
	 * Spring AI Alibaba configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.audio.synthesis";

	@NestedConfigurationProperty
	private DashScopeSpeechSynthesisOptions options = DashScopeSpeechSynthesisOptions.builder()
		.withModel("cosyvoice-v1")
		.withVoice("longhua")
		.build();

	public DashScopeSpeechSynthesisProperties() {
		super.setBaseUrl(DEFAULT_BASE_URL);
	}

	public DashScopeSpeechSynthesisOptions getOptions() {
		return this.options;
	}

	public void setOptions(DashScopeSpeechSynthesisOptions options) {
		this.options = options;
	}

}
