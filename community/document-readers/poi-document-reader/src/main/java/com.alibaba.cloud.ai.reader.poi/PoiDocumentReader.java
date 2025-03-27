package com.alibaba.cloud.ai.reader.poi;

import org.apache.poi.extractor.ExtractorFactory;
import org.apache.poi.extractor.POITextExtractor;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import com.alibaba.cloud.ai.reader.poi.AbstractDocumentReader;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Parses Microsoft Office file into a {@link Document} using Apache POI library. This
 * parser supports various file formats, including doc, docx, ppt, pptx, xls, and xlsx.
 * For detailed information on supported formats, please refer to the
 * <a href="https://poi.apache.org/">official Apache POI website</a>.
 *
 * @author HeYQ
 * @since 1.0.0
 */
public class PoiDocumentReader extends AbstractDocumentReader implements DocumentReader {

	/**
	 * Metadata key representing the source of the document.
	 */
	public static final String METADATA_SOURCE = "source";

	private final Resource resource;

	private final ExtractedTextFormatter textFormatter;

	public PoiDocumentReader(String resourceUrl) {
		this(resourceUrl, ExtractedTextFormatter.defaults());
	}

	public PoiDocumentReader(String resourceUrl, ExtractedTextFormatter textFormatter) {
		this(new DefaultResourceLoader().getResource(resourceUrl), textFormatter);
	}

	public PoiDocumentReader(Resource resource) {
		this(resource, ExtractedTextFormatter.defaults());
	}

	public PoiDocumentReader(Resource resource, ExtractedTextFormatter textFormatter) {
		super(resource);
		this.resource = resource;
		this.textFormatter = textFormatter;
	}

	/**
	 * Extracts and returns the list of documents from the resource.
	 * @return List of extracted {@link Document}
	 */
	@Override
	public List<Document> get() {
		try (POITextExtractor extractor = ExtractorFactory.createExtractor(resource.getInputStream())) {
			String text = extractor.getText();
			return List.of(toDocument(text));
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
		Document doc = new Document(docText);
		doc.getMetadata().put(METADATA_SOURCE, resourceName());
		return doc;
	}

}
