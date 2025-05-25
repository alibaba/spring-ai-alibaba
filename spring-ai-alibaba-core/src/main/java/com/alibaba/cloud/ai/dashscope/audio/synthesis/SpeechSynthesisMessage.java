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
package com.alibaba.cloud.ai.dashscope.audio.synthesis;

import java.util.Objects;

/**
 * @author kevinlin09
 */
public class SpeechSynthesisMessage {

	private String text;

	/**
	 * Constructs a new {@link SpeechSynthesisMessage} object with the given text.
	 * @param text the text to be converted to speech
	 */
	public SpeechSynthesisMessage(String text) {
		this.text = text;
	}

	/**
	 * Returns the text of this speech synthesis message.
	 * @return the text of this speech synthesis message
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the text of this speech synthesis message.
	 * @param text the new text for this speech synthesis message
	 */
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {

			return true;
		}

		if (!(o instanceof SpeechSynthesisMessage that)) {

			return false;
		}

		return Objects.equals(text, that.text);
	}

	@Override
	public int hashCode() {

		return Objects.hash(text);
	}

}
