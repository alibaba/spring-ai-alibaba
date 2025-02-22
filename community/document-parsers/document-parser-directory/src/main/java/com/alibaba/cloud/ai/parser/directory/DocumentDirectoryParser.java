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
package com.alibaba.cloud.ai.parser.directory;

/**
 * @author HeYQ
 * @since 2025-02-07 16:40
 */

import com.alibaba.cloud.ai.document.DocumentParser;
import org.springframework.ai.document.Document;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DocumentDirectoryParser implements DocumentParser {

	private final String path;

	private final List<String> globPatterns;

	private final List<String> excludePatterns;

	private final boolean loadHidden;

	private final DocumentParser documentParser;

	private final boolean recursive;

	private final int sampleSize;

	private final boolean randomizeSample;

	private final Integer sampleSeed;

	private DocumentDirectoryParser(Builder builder) {
		this.path = builder.path;
		this.globPatterns = builder.globPatterns;
		this.excludePatterns = builder.excludePatterns;
		this.loadHidden = builder.loadHidden;
		this.documentParser = builder.documentParser;
		this.recursive = builder.recursive;
		this.sampleSize = builder.sampleSize;
		this.randomizeSample = builder.randomizeSample;
		this.sampleSeed = builder.sampleSeed;
	}

	public List<Document> parse() {
		Path dirPath = Paths.get(path);
		if (!Files.exists(dirPath)) {
			throw new RuntimeException("Directory not found: " + path);
		}
		if (!Files.isDirectory(dirPath)) {
			throw new RuntimeException("Expected directory, got file: " + path);
		}

		List<Path> filePaths = findFiles(dirPath);
		if (sampleSize > 0) {
			filePaths = sampleFiles(filePaths);
		}

		Stream<Path> fileStream = filePaths.stream();

		return fileStream.flatMap(this::loadDocumentsFromFile).filter(Objects::nonNull).collect(Collectors.toList());
	}

	private List<Path> findFiles(Path dir) {
		try (Stream<Path> stream = recursive ? Files.walk(dir) : Files.list(dir)) {

			return stream.filter(this::isValidFile).collect(Collectors.toList());
		}
		catch (IOException e) {
			throw new RuntimeException("Error listing files", e);
		}
	}

	private boolean isValidFile(Path path) {
		if (!Files.isRegularFile(path)) {
			return false;
		}
		if (!loadHidden && isHidden(path)) {
			return false;
		}
		if (matchesExclude(path)) {
			return false;
		}
		return matchesGlob(path);
	}

	private boolean isHidden(Path path) {
		try {
			return Files.isHidden(path) || path.toString().contains(FileSystems.getDefault().getSeparator() + ".");
		}
		catch (IOException e) {
			return false;
		}
	}

	private boolean matchesGlob(Path path) {
		return globPatterns.stream()
			.anyMatch(
					pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(path.getFileName()));
	}

	private boolean matchesExclude(Path path) {

		return excludePatterns.stream()
			.anyMatch(
					pattern -> FileSystems.getDefault().getPathMatcher("glob:" + pattern).matches(path.getFileName()));
	}

	private Stream<Document> loadDocumentsFromFile(Path file) {
		try {
			return documentParser.parse(new FileInputStream(file.toFile())).stream();
		}
		catch (Exception e) {
			throw new RuntimeException("Error loading file: " + file, e);
		}
	}

	private List<Path> sampleFiles(List<Path> files) {
		if (randomizeSample) {
			Random rand = sampleSeed != null ? new Random(sampleSeed) : new Random();
			Collections.shuffle(files, rand);
		}
		return files.subList(0, Math.min(sampleSize, files.size()));
	}

	@Override
	public List<org.springframework.ai.document.Document> parse(InputStream inputStream) {
		return List.of();
	}

	public static class Builder {

		private final String path;

		private List<String> globPatterns = Collections.singletonList("*.*");

		private List<String> excludePatterns = new ArrayList<>();

		private boolean loadHidden = false;

		private DocumentParser documentParser;

		private boolean recursive = false;

		private int sampleSize = 0;

		private boolean randomizeSample = false;

		private Integer sampleSeed = null;

		public Builder(String path) {
			this.path = path;
		}

		public Builder glob(String... patterns) {
			this.globPatterns = Arrays.asList(patterns);
			return this;
		}

		public Builder exclude(String... patterns) {
			this.excludePatterns = Arrays.asList(patterns);
			return this;
		}

		public Builder loadHidden(boolean loadHidden) {
			this.loadHidden = loadHidden;
			return this;
		}

		public Builder documentParser(DocumentParser loader) {
			this.documentParser = loader;
			return this;
		}

		public Builder recursive(boolean recursive) {
			this.recursive = recursive;
			return this;
		}

		public Builder sample(int size, boolean randomize, Integer seed) {
			this.sampleSize = size;
			this.randomizeSample = randomize;
			this.sampleSeed = seed;
			return this;
		}

		public DocumentDirectoryParser build() {
			return new DocumentDirectoryParser(this);
		}

	}

}
