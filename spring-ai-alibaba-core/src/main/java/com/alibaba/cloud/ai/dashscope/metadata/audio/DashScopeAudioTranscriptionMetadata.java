package com.alibaba.cloud.ai.dashscope.metadata.audio;

import org.springframework.ai.model.ResultMetadata;

/**
 * @author xYLiu
 * @author yuluo
 * @since 2023.0.1.0
 */

public interface DashScopeAudioTranscriptionMetadata extends ResultMetadata {

	/**
	 * A constant instance of {@link DashScopeAudioTranscriptionMetadata} that represents
	 * a null or empty metadata.
	 */
	DashScopeAudioTranscriptionMetadata NULL = DashScopeAudioTranscriptionMetadata.create();

	/**
	 * Factory method for creating a new instance of
	 * {@link DashScopeAudioTranscriptionMetadata}.
	 * @return a new instance of {@link DashScopeAudioTranscriptionMetadata}
	 */
	static DashScopeAudioTranscriptionMetadata create() {
		return new DashScopeAudioTranscriptionMetadata() {
		};
	}

}
