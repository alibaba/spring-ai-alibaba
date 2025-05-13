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
package com.alibaba.cloud.ai.reader.obsidian;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Resource implementation for accessing Obsidian vault files
 *
 * @author xiadong
 * @since 2024-01-06
 */
public class ObsidianResource implements Resource {

	public static final String SOURCE = "source";

	public static final String MARKDOWN_EXTENSION = ".md";

	private final Path vaultPath;

	private final Path filePath;

	private final InputStream inputStream;

	/**
	 * Constructor for single file
	 * @param vaultPath Path to Obsidian vault
	 * @param filePath Path to markdown file
	 */
	public ObsidianResource(Path vaultPath, Path filePath) {
		Assert.notNull(vaultPath, "VaultPath must not be null");
		Assert.notNull(filePath, "FilePath must not be null");
		Assert.isTrue(Files.exists(vaultPath), "Vault directory does not exist: " + vaultPath);
		Assert.isTrue(Files.exists(filePath), "File does not exist: " + filePath);
		Assert.isTrue(filePath.toString().endsWith(MARKDOWN_EXTENSION), "File must be a markdown file: " + filePath);

		this.vaultPath = vaultPath;
		this.filePath = filePath;
		try {
			this.inputStream = new FileInputStream(filePath.toFile());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to create input stream for file: " + filePath, e);
		}
	}

	/**
	 * Find all markdown files in the vault Recursively searches through all
	 * subdirectories Only includes .md files and ignores hidden files/directories
	 * @param vaultPath Root path of the Obsidian vault
	 * @return List of ObsidianResource for each markdown file
	 */
	public static List<ObsidianResource> findAllMarkdownFiles(Path vaultPath) {
		Assert.notNull(vaultPath, "VaultPath must not be null");
		Assert.isTrue(Files.exists(vaultPath), "Vault directory does not exist: " + vaultPath);
		Assert.isTrue(Files.isDirectory(vaultPath), "VaultPath must be a directory: " + vaultPath);

		List<ObsidianResource> resources = new ArrayList<>();
		try (Stream<Path> paths = Files.walk(vaultPath)) {
			paths
				// Only include .md files
				.filter(path -> path.toString().endsWith(MARKDOWN_EXTENSION))
				// Ignore hidden files and files in hidden directories
				.filter(path -> {
					Path relativePath = vaultPath.relativize(path);
					String[] pathParts = relativePath.toString().split("/");
					for (String part : pathParts) {
						if (part.startsWith(".")) {
							return false;
						}
					}
					return true;
				})
				// Only include regular files (not directories)
				.filter(Files::isRegularFile)
				.forEach(path -> resources.add(new ObsidianResource(vaultPath, path)));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to walk vault directory: " + vaultPath, e);
		}
		return resources;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private Path vaultPath;

		private Path filePath;

		public Builder vaultPath(Path vaultPath) {
			this.vaultPath = vaultPath;
			return this;
		}

		public Builder filePath(Path filePath) {
			this.filePath = filePath;
			return this;
		}

		public ObsidianResource build() {
			Assert.notNull(vaultPath, "VaultPath must not be null");
			Assert.notNull(filePath, "FilePath must not be null");
			return new ObsidianResource(vaultPath, filePath);
		}

	}

	@Override
	public boolean exists() {
		return Files.exists(filePath);
	}

	@Override
	public URL getURL() throws IOException {
		return filePath.toUri().toURL();
	}

	@Override
	public URI getURI() throws IOException {
		return filePath.toUri();
	}

	@Override
	public File getFile() throws IOException {
		return filePath.toFile();
	}

	@Override
	public long contentLength() throws IOException {
		return Files.size(filePath);
	}

	@Override
	public long lastModified() throws IOException {
		return Files.getLastModifiedTime(filePath).toMillis();
	}

	@Override
	public Resource createRelative(String relativePath) throws IOException {
		Path newPath = filePath.resolveSibling(relativePath);
		return new ObsidianResource(vaultPath, newPath);
	}

	@Override
	public String getFilename() {
		return filePath.getFileName().toString();
	}

	@Override
	public String getDescription() {
		return "Obsidian resource [vault=" + vaultPath + ", file=" + filePath + "]";
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

	public Path getVaultPath() {
		return vaultPath;
	}

	public Path getFilePath() {
		return filePath;
	}

	public String getSource() {
		return vaultPath.relativize(filePath).toString();
	}

}
