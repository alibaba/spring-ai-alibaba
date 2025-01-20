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
package com.alibaba.cloud.ai.parser.markdown.config;

import org.springframework.ai.document.Document;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * @author HeYQ
 * @since 2024-12-08 21:38
 */

public class MarkdownDocumentParserConfig {

	public final boolean horizontalRuleCreateDocument;

	public final boolean includeCodeBlock;

	public final boolean includeBlockquote;

	public final Map<String, Object> additionalMetadata;

	public MarkdownDocumentParserConfig(Builder builder) {
		this.horizontalRuleCreateDocument = builder.horizontalRuleCreateDocument;
		this.includeCodeBlock = builder.includeCodeBlock;
		this.includeBlockquote = builder.includeBlockquote;
		this.additionalMetadata = builder.additionalMetadata;
	}

	/**
	 * @return the default configuration
	 */
	public static MarkdownDocumentParserConfig defaultConfig() {
		return builder().build();
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private boolean horizontalRuleCreateDocument = false;

		private boolean includeCodeBlock = false;

		private boolean includeBlockquote = false;

		private Map<String, Object> additionalMetadata = new HashMap<>();

		private Builder() {
		}

		/**
		 * Text divided by horizontal lines will create new {@link Document}s. The default
		 * is {@code false}, meaning text separated by horizontal lines won't create a new
		 * document.
		 * @param horizontalRuleCreateDocument flag to determine whether new documents are
		 * created from text divided by horizontal line
		 * @return this builder
		 */
		public Builder withHorizontalRuleCreateDocument(boolean horizontalRuleCreateDocument) {
			this.horizontalRuleCreateDocument = horizontalRuleCreateDocument;
			return this;
		}

		/**
		 * Whatever to include code blocks in {@link Document}s. The default is
		 * {@code false}, which means all code blocks are in separate documents.
		 * @param includeCodeBlock flag to include code block into paragraph document or
		 * create new with code only
		 * @return this builder
		 */
		public Builder withIncludeCodeBlock(boolean includeCodeBlock) {
			this.includeCodeBlock = includeCodeBlock;
			return this;
		}

		/**
		 * Whatever to include blockquotes in {@link Document}s. The default is
		 * {@code false}, which means all blockquotes are in separate documents.
		 * @param includeBlockquote flag to include blockquotes into paragraph document or
		 * create new with blockquote only
		 * @return this builder
		 */
		public Builder withIncludeBlockquote(boolean includeBlockquote) {
			this.includeBlockquote = includeBlockquote;
			return this;
		}

		/**
		 * Adds this additional metadata to the all built {@link Document}s.
		 * @return this builder
		 */
		public Builder withAdditionalMetadata(String key, Object value) {
			Assert.notNull(key, "key must not be null");
			Assert.notNull(value, "value must not be null");
			this.additionalMetadata.put(key, value);
			return this;
		}

		/**
		 * Adds this additional metadata to the all built {@link Document}s.
		 * @return this builder
		 */
		public Builder withAdditionalMetadata(Map<String, Object> additionalMetadata) {
			Assert.notNull(additionalMetadata, "additionalMetadata must not be null");
			this.additionalMetadata = additionalMetadata;
			return this;
		}

		/**
		 * @return the immutable configuration
		 */
		public MarkdownDocumentParserConfig build() {
			return new MarkdownDocumentParserConfig(this);
		}

	}

}
