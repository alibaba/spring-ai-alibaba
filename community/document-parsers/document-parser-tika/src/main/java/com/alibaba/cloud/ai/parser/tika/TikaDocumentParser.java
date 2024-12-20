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
package com.alibaba.cloud.ai.parser.tika;

import com.alibaba.cloud.ai.document.DocumentParser;
import org.apache.tika.exception.ZeroByteFileException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Parses files into {@link Document}s using Apache Tika library, automatically detecting
 * the file format. This parser supports various file formats, including PDF, DOC, PPT,
 * XLS. For detailed information on supported formats, please refer to the
 * <a href="https://tika.apache.org/2.9.1/formats.html">Apache Tika documentation</a>.
 *
 * @author HeYQ
 * @since 2024-12-02 11:32
 */

public class TikaDocumentParser implements DocumentParser {

	private static final int NO_WRITE_LIMIT = -1;

	public static final Supplier<Parser> DEFAULT_PARSER_SUPPLIER = AutoDetectParser::new;

	public static final Supplier<Metadata> DEFAULT_METADATA_SUPPLIER = Metadata::new;

	public static final Supplier<ParseContext> DEFAULT_PARSE_CONTEXT_SUPPLIER = ParseContext::new;

	public static final Supplier<ContentHandler> DEFAULT_CONTENT_HANDLER_SUPPLIER = () -> new BodyContentHandler(
			NO_WRITE_LIMIT);

	private final Supplier<Parser> parserSupplier;

	private final Supplier<ContentHandler> contentHandlerSupplier;

	private final Supplier<Metadata> metadataSupplier;

	private final Supplier<ParseContext> parseContextSupplier;

	private final ExtractedTextFormatter textFormatter;

	public TikaDocumentParser() {
		this((Supplier<Parser>) null, null, null, null, ExtractedTextFormatter.defaults());
	}

	public TikaDocumentParser(ExtractedTextFormatter textFormatter) {
		this((Supplier<Parser>) null, null, null, null, textFormatter);
	}

	public TikaDocumentParser(Supplier<ContentHandler> contentHandlerSupplier, ExtractedTextFormatter textFormatter) {
		this((Supplier<Parser>) null, contentHandlerSupplier, null, null, textFormatter);
	}

	public TikaDocumentParser(Supplier<Parser> parserSupplier, Supplier<ContentHandler> contentHandlerSupplier,
			Supplier<Metadata> metadataSupplier, Supplier<ParseContext> parseContextSupplier) {
		this(parserSupplier, contentHandlerSupplier, metadataSupplier, parseContextSupplier,
				ExtractedTextFormatter.defaults());
	}

	/**
	 * Creates an instance of an {@code ApacheTikaDocumentParser} with the provided
	 * suppliers for Tika components. If some of the suppliers are not provided
	 * ({@code null}), the defaults will be used.
	 * @param parserSupplier Supplier for Tika parser to use. Default:
	 * {@link AutoDetectParser}
	 * @param contentHandlerSupplier Supplier for Tika content handler. Default:
	 * {@link BodyContentHandler} without write limit
	 * @param metadataSupplier Supplier for Tika metadata. Default: empty {@link Metadata}
	 * @param parseContextSupplier Supplier for Tika parse context. Default: empty
	 * {@link ParseContext}
	 * @param textFormatter Formatter for extracted text. Default:
	 * {@link ExtractedTextFormatter#defaults()}
	 */
	public TikaDocumentParser(Supplier<Parser> parserSupplier, Supplier<ContentHandler> contentHandlerSupplier,
			Supplier<Metadata> metadataSupplier, Supplier<ParseContext> parseContextSupplier,
			ExtractedTextFormatter textFormatter) {
		this.parserSupplier = getOrDefault(parserSupplier, () -> DEFAULT_PARSER_SUPPLIER);
		this.contentHandlerSupplier = getOrDefault(contentHandlerSupplier, () -> DEFAULT_CONTENT_HANDLER_SUPPLIER);
		this.metadataSupplier = getOrDefault(metadataSupplier, () -> DEFAULT_METADATA_SUPPLIER);
		this.parseContextSupplier = getOrDefault(parseContextSupplier, () -> DEFAULT_PARSE_CONTEXT_SUPPLIER);
		this.textFormatter = textFormatter;
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try {
			Parser parser = parserSupplier.get();
			ContentHandler contentHandler = contentHandlerSupplier.get();
			Metadata metadata = metadataSupplier.get();
			ParseContext parseContext = parseContextSupplier.get();

			parser.parse(inputStream, contentHandler, metadata, parseContext);
			String text = contentHandler.toString();

			if (Objects.isNull(text)) {
				throw new ZeroByteFileException("The content is blank!");
			}

			return Collections.singletonList(toDocument(text));
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Converts the given text to a {@link Document}.
	 * @param docText Text to be converted
	 * @return Converted document
	 */
	private Document toDocument(String docText) {
		docText = Objects.requireNonNullElse(docText, "");
		docText = this.textFormatter.format(docText);
		return new Document(docText);
	}

	private static <T> T getOrDefault(T value, Supplier<T> defaultValueSupplier) {
		return value != null ? value : defaultValueSupplier.get();
	}

}
