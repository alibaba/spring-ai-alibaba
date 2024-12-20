package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import org.springframework.ai.model.ModelResponse;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResult;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponseMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisResponse implements ModelResponse<SpeechSynthesisResult> {

	private final SpeechSynthesisResult result;

	private final SpeechSynthesisResponseMetadata metadata;

	public SpeechSynthesisResponse(SpeechSynthesisResult result) {
		this(result, SpeechSynthesisResponseMetadata.NULL);
	}

	public SpeechSynthesisResponse(SpeechSynthesisResult result, SpeechSynthesisResponseMetadata metadata) {
		this.result = result;
		this.metadata = metadata;
	}

	@Override
	public SpeechSynthesisResult getResult() {
		return this.result;
	}

	@Override
	public List<SpeechSynthesisResult> getResults() {
		return Collections.singletonList(this.result);
	}

	@Override
	public SpeechSynthesisResponseMetadata getMetadata() {
		return this.metadata;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (!(o instanceof SpeechSynthesisResponse that))
			return false;
		return Objects.equals(this.result, that.result) && Objects.equals(this.metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.result, this.metadata);
	}

}
