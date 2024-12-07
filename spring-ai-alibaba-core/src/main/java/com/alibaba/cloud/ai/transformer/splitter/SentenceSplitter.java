/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.transformer.splitter;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Title Sentence splitter with nlp model.<br>
 * Description Sentence splitter with nlp model.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class SentenceSplitter extends TextSplitter {

	private final EncodingRegistry registry = Encodings.newLazyEncodingRegistry();

	private final Encoding encoding = registry.getEncoding(EncodingType.CL100K_BASE);

	private static final int DEFAULT_CHUNK_SIZE = 1024;

	private final SentenceModel sentenceModel;

	private final int chunkSize;

	public SentenceSplitter() {
		this(DEFAULT_CHUNK_SIZE);
	}

	public SentenceSplitter(int chunkSize) {
		this.chunkSize = chunkSize;
		this.sentenceModel = getSentenceModel();
	}

	@Override
	protected List<String> splitText(String text) {
		SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);
		String[] texts = sentenceDetector.sentDetect(text);
		if (texts == null || texts.length == 0) {
			return Collections.emptyList();
		}

		List<String> chunks = new ArrayList<>();
		StringBuilder chunk = new StringBuilder();
		for (int i = 0; i < texts.length; i++) {
			int currentChunkSize = getEncodedTokens(chunk.toString()).size();
			int textTokenSize = getEncodedTokens(texts[i]).size();
			if (currentChunkSize + textTokenSize > chunkSize) {
				chunks.add(chunk.toString());
				chunk = new StringBuilder(texts[i]);
			}
			else {
				chunk.append(texts[i]);
			}

			if (i == texts.length - 1) {
				chunks.add(chunk.toString());
			}
		}

		return chunks;
	}

	private SentenceModel getSentenceModel() {
		try (InputStream is = getClass().getResourceAsStream("/opennlp/opennlp-en-ud-ewt-sentence-1.2-2.5.0.bin")) {
			if (is == null) {
				throw new RuntimeException("sentence model is invalid");
			}

			return new SentenceModel(is);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private List<Integer> getEncodedTokens(String text) {
		Assert.notNull(text, "Text must not be null");
		return this.encoding.encode(text).boxed();
	}

}
