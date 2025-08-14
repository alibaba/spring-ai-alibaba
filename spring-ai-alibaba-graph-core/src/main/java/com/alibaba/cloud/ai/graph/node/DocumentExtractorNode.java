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
package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.document.JsonDocumentParser;
import com.alibaba.cloud.ai.document.TextDocumentParser;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.parser.bshtml.BsHtmlDocumentParser;
import com.alibaba.cloud.ai.parser.markdown.MarkdownDocumentParser;
import com.alibaba.cloud.ai.parser.tika.TikaDocumentParser;
import com.alibaba.cloud.ai.parser.yaml.YamlDocumentParser;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.ai.document.Document;
import org.springframework.ai.util.json.JsonParser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author HeYQ
 * @since 2025-05-02 17:03
 */
public class DocumentExtractorNode implements NodeAction {

	private final String paramsKey;

	private final String outputKey;

	private final List<String> fileList;

	private final boolean inputIsArray;

	private final Map<String, Function<InputStream, List<Document>>> extractors = new HashMap<>();

	public DocumentExtractorNode(String paramsKey, String outputKey, List<String> fileList, boolean inputIsArray) {
		this.paramsKey = paramsKey;
		this.outputKey = outputKey;
		this.fileList = fileList;
		this.inputIsArray = inputIsArray;
		extractors.put("txt", inputStream -> new TextDocumentParser().parse(inputStream));
		extractors.put("markdown", inputStream -> new MarkdownDocumentParser().parse(inputStream));
		extractors.put("md", inputStream -> new MarkdownDocumentParser().parse(inputStream));
		extractors.put("html", inputStream -> new BsHtmlDocumentParser().parse(inputStream));
		extractors.put("htm", inputStream -> new BsHtmlDocumentParser().parse(inputStream));
		extractors.put("xml", inputStream -> new BsHtmlDocumentParser().parse(inputStream));
		extractors.put("json", inputStream -> new JsonDocumentParser().parse(inputStream));
		extractors.put("yaml", inputStream -> new YamlDocumentParser().parse(inputStream));
		extractors.put("yml", inputStream -> new YamlDocumentParser().parse(inputStream));
		extractors.put("pdf", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("doc", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("docx", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("csv", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("xls", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("xlsx", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("ppt", inputStream -> new TikaDocumentParser().parse(inputStream));
		extractors.put("pptx", inputStream -> new TikaDocumentParser().parse(inputStream));
	}

	/**
	 * 支持本地或者网络获取输入流
	 */
	private InputStream getInputStream(String filePath) throws IOException {
		URI uri;
		if (filePath.startsWith("http://") || filePath.startsWith("https://") || filePath.startsWith("ftp://")) {
			uri = URI.create(filePath);
		}
		else {
			uri = Paths.get(filePath).toUri();
		}

		if (uri.getScheme().equals("file")) {
			return new BufferedInputStream(Files.newInputStream(Paths.get(uri)));
		}
		else {
			return new BufferedInputStream(uri.toURL().openStream());
		}
	}

	private List<String> getDocument(List<String> fileList) {
		return fileList.stream().map(String::trim).map(file -> {
			try (InputStream inputStream = this.getInputStream(file.trim())) {
				return this.extractTextByFileExtension(inputStream, getFileExtension(file));
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to parse test file: " + file, e);
			}
		}).toList();
	}

	@Override
	public Map<String, Object> apply(OverAllState state) throws Exception {
		if (paramsKey == null && fileList == null) {
			throw new RuntimeException("File variable not found for selector");
		}
		List<String> fileList;
		Object fileObj = state.value(paramsKey).orElse(this.fileList);
		if (this.inputIsArray) {
			if (fileObj instanceof List<?>) {
				fileList = (List<String>) fileObj;
			}
			else if (fileObj instanceof String[]) {
				fileList = Arrays.asList((String[]) fileObj);
			}
			else {
				// 尝试以Json字符串解析，如果失败则说明输入无效
				try {
					fileList = JsonParser.fromJson(fileObj.toString(), new TypeReference<List<String>>() {
					});
				}
				catch (Exception ignore) {
					fileList = null;
				}
			}
			if (fileList == null || fileList.isEmpty()) {
				throw new RuntimeException("Variable fileList is not an ArrayFileSegment");
			}
		}
		else {
			// 单个文件，直接添加进列表
			fileList = List.of(fileObj.toString());
		}
		List<String> documentContents = this.getDocument(fileList);

		String key = Optional.ofNullable(this.outputKey).orElse("text");
		if (!this.inputIsArray) {
			return Map.of(key, documentContents.get(0));
		}
		else {
			return Map.of(key, documentContents);
		}
	}

	private String extractTextByFileExtension(InputStream fileContent, String fileExtension) {

		Function<InputStream, List<Document>> extractor = this.extractors.get(fileExtension);
		if (extractor == null) {
			throw new RuntimeException("Unsupported Extension Type: " + fileExtension);
		}

		return extractor.apply(fileContent).get(0).getText();
	}

	private String getFileExtension(String filePath) {
		Path path = Paths.get(filePath);
		String fileName = path.getFileName().toString();
		int dotIndex = fileName.lastIndexOf('.');

		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String paramsKey;

		private String outputKey;

		private List<String> fileList;

		private boolean inputIsArray = false;

		public Builder paramsKey(String paramsKey) {
			this.paramsKey = paramsKey;
			return this;
		}

		public Builder outputKey(String outputKey) {
			this.outputKey = outputKey;
			return this;
		}

		public Builder fileList(List<String> fileList) {
			this.fileList = fileList;
			return this;
		}

		public Builder inputIsArray(boolean inputIsArray) {
			this.inputIsArray = inputIsArray;
			return this;
		}

		public DocumentExtractorNode build() {
			return new DocumentExtractorNode(paramsKey, outputKey, fileList, inputIsArray);
		}

	}

}
