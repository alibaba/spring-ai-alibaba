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

package com.alibaba.cloud.ai.document;

import com.alibaba.cloud.ai.model.RerankResultMetadata;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.ModelResult;
import org.springframework.ai.model.ResultMetadata;

import java.util.Objects;

/**
 * Title Document with score.<br>
 * Description Document with score.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public class DocumentWithScore implements ModelResult<Document> {

	/**
	 * Score of document
	 */
	private Double score;

	/**
	 * document information
	 */
	private Document document;

	private RerankResultMetadata metadata;

	public Double getScore() {
		return score;
	}

	public void setScore(Double score) {
		this.score = score;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public void setMetadata(RerankResultMetadata metadata) {
		this.metadata = metadata;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public Document getOutput() {
		return this.document;
	}

	@Override
	public ResultMetadata getMetadata() {
		return this.metadata;
	}

	public static final class Builder {

		private final DocumentWithScore documentWithScore;

		private Builder() {
			this.documentWithScore = new DocumentWithScore();
		}

		public Builder withScore(Double score) {
			this.documentWithScore.setScore(score);
			return this;
		}

		public Builder withDocument(Document document) {
			this.documentWithScore.setDocument(document);
			return this;
		}

		public Builder withMetadata(RerankResultMetadata metadata) {
			this.documentWithScore.setMetadata(metadata);
			return this;
		}

		public DocumentWithScore build() {
			return documentWithScore;
		}

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		DocumentWithScore that = (DocumentWithScore) o;
		return Objects.equals(score, that.score) && Objects.equals(document, that.document);
	}

	@Override
	public int hashCode() {
		return Objects.hash(score, document);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("DocumentWithScore{");
		sb.append("score=").append(score);
		sb.append(", document=").append(document);
		sb.append('}');
		return sb.toString();
	}

}
