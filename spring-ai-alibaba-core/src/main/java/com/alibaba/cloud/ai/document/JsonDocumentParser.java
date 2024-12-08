package com.alibaba.cloud.ai.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.EmptyJsonMetadataGenerator;
import org.springframework.ai.reader.JsonMetadataGenerator;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 * @author HeYQ
 * @since 2024-12-08 21:13
 */

public class JsonDocumentParser implements DocumentParser {

	private final JsonMetadataGenerator jsonMetadataGenerator;

	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * The key from the JSON that we will use as the text to parse into the Document text
	 */
	private final List<String> jsonKeysToUse;

	public JsonDocumentParser(String... jsonKeysToUse) {
		this(new EmptyJsonMetadataGenerator(), jsonKeysToUse);
	}

	public JsonDocumentParser(JsonMetadataGenerator jsonMetadataGenerator, String... jsonKeysToUse) {
		Objects.requireNonNull(jsonKeysToUse, "keys must not be null");
		Objects.requireNonNull(jsonMetadataGenerator, "jsonMetadataGenerator must not be null");
		this.jsonMetadataGenerator = jsonMetadataGenerator;
		this.jsonKeysToUse = List.of(jsonKeysToUse);
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		try {
			JsonNode rootNode = this.objectMapper.readTree(inputStream);

			if (rootNode.isArray()) {
				return StreamSupport.stream(rootNode.spliterator(), true)
					.map(jsonNode -> parseJsonNode(jsonNode, this.objectMapper))
					.toList();
			}
			else {
				return Collections.singletonList(parseJsonNode(rootNode, this.objectMapper));
			}
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Document parseJsonNode(JsonNode jsonNode, ObjectMapper objectMapper) {
		Map<String, Object> item = objectMapper.convertValue(jsonNode, new TypeReference<Map<String, Object>>() {

		});
		var sb = new StringBuilder();

		this.jsonKeysToUse.stream()
			.filter(item::containsKey)
			.forEach(key -> sb.append(key).append(": ").append(item.get(key)).append(System.lineSeparator()));

		Map<String, Object> metadata = this.jsonMetadataGenerator.generate(item);
		String content = sb.isEmpty() ? item.toString() : sb.toString();
		return new Document(content, metadata);
	}

	protected List<Document> get(JsonNode rootNode) {
		if (rootNode.isArray()) {
			return StreamSupport.stream(rootNode.spliterator(), true)
				.map(jsonNode -> parseJsonNode(jsonNode, this.objectMapper))
				.toList();
		}
		else {
			return Collections.singletonList(parseJsonNode(rootNode, this.objectMapper));
		}
	}

	/**
	 * Retrieves documents from the JSON resource using a JSON Pointer.
	 * @param pointer A JSON Pointer string (RFC 6901) to locate the desired element
	 * @return A list of Documents parsed from the located JSON element
	 * @throws RuntimeException if the JSON cannot be parsed or the pointer is invalid
	 */
	public List<Document> get(String pointer, InputStream inputStream) {
		try {
			JsonNode rootNode = this.objectMapper.readTree(inputStream);
			JsonNode targetNode = rootNode.at(pointer);

			if (targetNode.isMissingNode()) {
				throw new IllegalArgumentException("Invalid JSON Pointer: " + pointer);
			}

			return get(targetNode);
		}
		catch (IOException e) {
			throw new RuntimeException("Error reading JSON resource", e);
		}
	}

}
