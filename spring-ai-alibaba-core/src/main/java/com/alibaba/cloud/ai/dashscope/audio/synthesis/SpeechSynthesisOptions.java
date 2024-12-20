package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.ModelOptions;
import org.springframework.lang.Nullable;

/**
 * @author kevinlin09
 */
public interface SpeechSynthesisOptions extends ModelOptions {

	@Nullable
	String getModel();

}
