/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multimodal.workflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;

import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

@Component
class ImageArtifactStore {

	private static final int MAX_ENCODED_LENGTH = 28 * 1024 * 1024;

	private final Path inputs;

	private final Path artifacts;

	ImageArtifactStore(@Value("${example.multimodal.artifact-directory}") String directory) {
		Path root = Path.of(directory);
		this.inputs = root.resolve("inputs");
		this.artifacts = root.resolve("artifacts");
	}

	static Media inputMedia(byte[] bytes) throws IOException {
		return Media.builder().mimeType(imageType(bytes)).data(bytes).build();
	}

	void stageInput(UUID id, Media image) throws IOException {
		write(inputs, id, image.getDataAsByteArray());
	}

	Media loadInput(UUID id) throws IOException {
		return inputMedia(Files.readAllBytes(inputs.resolve(id.toString())));
	}

	void releaseInput(UUID id) throws IOException {
		Files.deleteIfExists(inputs.resolve(id.toString()));
	}

	UUID save(String base64) throws IOException {
		if (base64 == null || base64.isBlank() || base64.length() > MAX_ENCODED_LENGTH) {
			throw new IllegalStateException("Expected a bounded Base64 image from the image model");
		}
		byte[] bytes;
		try {
			bytes = Base64.getDecoder().decode(base64);
			imageType(bytes);
		}
		catch (IllegalArgumentException e) {
			throw new IllegalStateException("The image model returned invalid PNG or JPEG data", e);
		}
		UUID id = UUID.randomUUID();
		write(artifacts, id, bytes);
		return id;
	}

	Path path(UUID id) {
		return artifacts.resolve(id.toString());
	}

	Artifact describe(UUID id) throws IOException {
		Path path = path(id);
		try (var input = Files.newInputStream(path)) {
			String mimeType = imageType(input.readNBytes(16)).toString();
			return new Artifact(id, mimeType, Files.size(path), "/api/artifacts/" + id);
		}
	}

	private static MimeType imageType(byte[] bytes) throws IOException {
		String type = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(bytes));
		if (MimeTypeUtils.IMAGE_PNG_VALUE.equals(type) || MimeTypeUtils.IMAGE_JPEG_VALUE.equals(type)) {
			return MimeTypeUtils.parseMimeType(type);
		}
		throw new IllegalArgumentException("Upload a PNG or JPEG image");
	}

	private static void write(Path directory, UUID id, byte[] bytes) throws IOException {
		Files.createDirectories(directory);
		Path temporary = Files.createTempFile(directory, ".image-", ".tmp");
		try {
			Files.write(temporary, bytes);
			Files.move(temporary, directory.resolve(id.toString()), StandardCopyOption.ATOMIC_MOVE);
		}
		finally {
			Files.deleteIfExists(temporary);
		}
	}

	record Artifact(UUID id, String mimeType, long sizeBytes, String url) {
	}

}
