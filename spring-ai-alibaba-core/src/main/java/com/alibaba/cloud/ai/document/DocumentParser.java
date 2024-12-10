package com.alibaba.cloud.ai.document;

import org.springframework.ai.document.Document;

import java.io.InputStream;
import java.util.List;

/**
 * @author HeYQ
 * @since 2024-12-02 11:25
 */

public interface DocumentParser {

	/**
	 * Parses a given {@link InputStream} into a {@link Document}. The specific
	 * implementation of this method will depend on the type of the document being parsed.
	 * <p>
	 * Note: This method does not close the provided {@link InputStream} - it is the
	 * caller's responsibility to manage the lifecycle of the stream.
	 * @param inputStream The {@link InputStream} that contains the content of the
	 * {@link Document}.
	 * @return The parsed {@link Document}.
	 */
	List<Document> parse(InputStream inputStream);

}
