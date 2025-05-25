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
package com.alibaba.cloud.ai.parser.markdown;

import com.alibaba.cloud.ai.document.DocumentParser;
import com.alibaba.cloud.ai.parser.markdown.config.MarkdownDocumentParserConfig;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.Code;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author HeYQ
 * @since 2024-12-08 21:32
 */

public class MarkdownDocumentParser implements DocumentParser {

	/**
	 * Configuration to a parsing process.
	 */
	private final MarkdownDocumentParserConfig config;

	/**
	 * Markdown parser.
	 */
	private final Parser parser;

	public MarkdownDocumentParser() {
		this(MarkdownDocumentParserConfig.defaultConfig());
	}

	/**
	 * Create a new {@link MarkdownDocumentParser} instance.
	 *
	 */
	public MarkdownDocumentParser(MarkdownDocumentParserConfig config) {
		this.config = config;
		this.parser = Parser.builder().build();
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try (var input = inputStream) {
			Node node = this.parser.parseReader(new InputStreamReader(input));

			DocumentVisitor documentVisitor = new DocumentVisitor(this.config);
			node.accept(documentVisitor);

			return documentVisitor.getDocuments();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * A convenient class for visiting handled nodes in the Markdown document.
	 */
	static class DocumentVisitor extends AbstractVisitor {

		private final List<Document> documents = new ArrayList<>();

		private final List<String> currentParagraphs = new ArrayList<>();

		private final MarkdownDocumentParserConfig config;

		private Document.Builder currentDocumentBuilder;

		DocumentVisitor(MarkdownDocumentParserConfig config) {
			this.config = config;
		}

		/**
		 * Visits the document node and initializes the current document builder.
		 */
		@Override
		public void visit(org.commonmark.node.Document document) {
			this.currentDocumentBuilder = Document.builder();
			super.visit(document);
		}

		@Override
		public void visit(Heading heading) {
			buildAndFlush();
			super.visit(heading);
		}

		@Override
		public void visit(ThematicBreak thematicBreak) {
			if (this.config.horizontalRuleCreateDocument) {
				buildAndFlush();
			}
			super.visit(thematicBreak);
		}

		@Override
		public void visit(SoftLineBreak softLineBreak) {
			translateLineBreakToSpace();
			super.visit(softLineBreak);
		}

		@Override
		public void visit(HardLineBreak hardLineBreak) {
			translateLineBreakToSpace();
			super.visit(hardLineBreak);
		}

		@Override
		public void visit(ListItem listItem) {
			translateLineBreakToSpace();
			super.visit(listItem);
		}

		@Override
		public void visit(BlockQuote blockQuote) {
			if (!this.config.includeBlockquote) {
				buildAndFlush();
			}

			translateLineBreakToSpace();
			this.currentDocumentBuilder.metadata("category", "blockquote");
			super.visit(blockQuote);
		}

		@Override
		public void visit(Code code) {
			this.currentParagraphs.add(code.getLiteral());
			this.currentDocumentBuilder.metadata("category", "code_inline");
			super.visit(code);
		}

		@Override
		public void visit(FencedCodeBlock fencedCodeBlock) {
			if (!this.config.includeCodeBlock) {
				buildAndFlush();
			}

			translateLineBreakToSpace();
			this.currentParagraphs.add(fencedCodeBlock.getLiteral());
			this.currentDocumentBuilder.metadata("category", "code_block");
			this.currentDocumentBuilder.metadata("lang", fencedCodeBlock.getInfo());

			buildAndFlush();

			super.visit(fencedCodeBlock);
		}

		@Override
		public void visit(Text text) {
			if (text.getParent() instanceof Heading heading) {
				this.currentDocumentBuilder.metadata("category", "header_%d".formatted(heading.getLevel()))
					.metadata("title", text.getLiteral());
			}
			else {
				this.currentParagraphs.add(text.getLiteral());
			}

			super.visit(text);
		}

		public List<Document> getDocuments() {
			buildAndFlush();

			return this.documents;
		}

		private void buildAndFlush() {
			if (!this.currentParagraphs.isEmpty()) {
				String content = String.join("", this.currentParagraphs);

				Document.Builder builder = this.currentDocumentBuilder.text(content);

				this.config.additionalMetadata.forEach(builder::metadata);

				Document document = builder.build();

				this.documents.add(document);

				this.currentParagraphs.clear();
			}
			this.currentDocumentBuilder = Document.builder();
		}

		private void translateLineBreakToSpace() {
			if (!this.currentParagraphs.isEmpty()) {
				this.currentParagraphs.add(" ");
			}
		}

	}

}
