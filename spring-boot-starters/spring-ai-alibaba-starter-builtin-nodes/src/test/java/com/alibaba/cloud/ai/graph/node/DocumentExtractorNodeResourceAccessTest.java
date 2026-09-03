/*
 * Copyright 2026 the original author or authors.
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

import com.alibaba.cloud.ai.graph.OverAllState;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentExtractorNodeResourceAccessTest {

	@Test
	void readsLocalFileWithinConfiguredRoot() throws Exception {
		Path root = Files.createTempDirectory("document-extractor-root-");
		Path document = root.resolve("input document.txt");
		Files.writeString(document, "safe content");

		try {
			DocumentExtractorNode node = DocumentExtractorNode.builder()
				.paramsKey("file")
				.localFileRoot(root)
				.build();

			Map<String, Object> result = node.apply(new OverAllState(Map.of("file", document.toString())));

			assertEquals("safe content", result.get("text"));
		}
		finally {
			Files.deleteIfExists(document);
			Files.deleteIfExists(root);
		}
	}

	@Test
	void rejectsLocalFileOutsideConfiguredRoot() throws Exception {
		Path root = Files.createTempDirectory("document-extractor-root-");
		Path outsideFile = Files.createTempFile("document-extractor-outside-", ".txt");
		Files.writeString(outsideFile, "secret");

		try {
			DocumentExtractorNode node = DocumentExtractorNode.builder()
				.paramsKey("file")
				.localFileRoot(root)
				.build();

			assertThrows(RuntimeException.class,
					() -> node.apply(new OverAllState(Map.of("file", outsideFile.toString()))));
		}
		finally {
			Files.deleteIfExists(outsideFile);
			Files.deleteIfExists(root);
		}
	}

	@Test
	void rejectsSymlinkThatResolvesOutsideConfiguredRoot() throws Exception {
		Path root = Files.createTempDirectory("document-extractor-root-");
		Path outsideFile = Files.createTempFile("document-extractor-outside-", ".txt");
		Path symlink = root.resolve("linked.txt");
		Files.writeString(outsideFile, "secret");
		Files.createSymbolicLink(symlink, outsideFile);

		try {
			DocumentExtractorNode node = DocumentExtractorNode.builder()
				.paramsKey("file")
				.localFileRoot(root)
				.build();

			assertThrows(RuntimeException.class,
					() -> node.apply(new OverAllState(Map.of("file", symlink.toString()))));
		}
		finally {
			Files.deleteIfExists(symlink);
			Files.deleteIfExists(outsideFile);
			Files.deleteIfExists(root);
		}
	}

	@Test
	void rejectsLoopbackRemoteResourceEvenWhenRemoteResourcesAreEnabled() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse().setBody("internal response"));
			server.start();
			DocumentExtractorNode node = DocumentExtractorNode.builder()
				.paramsKey("file")
				.allowRemoteResources(true)
				.build();

			assertThrows(RuntimeException.class,
					() -> node.apply(new OverAllState(Map.of("file", server.url("/internal.txt").toString()))));
			assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS));
		}
	}

	@Test
	void disablesRemoteResourcesByDefault() {
		DocumentExtractorNode node = DocumentExtractorNode.builder().paramsKey("file").build();

		assertThrows(RuntimeException.class,
				() -> node.apply(new OverAllState(Map.of("file", "https://example.com/document.txt"))));
	}

	@Test
	void rejectsAlibabaCloudMetadataAddress() throws Exception {
		assertTrue(DocumentExtractorNode.isBlockedRemoteAddress(InetAddress.getByName("100.100.100.200")));
	}

	@Test
	void countsSkippedBytesTowardRemoteResourceLimit() throws Exception {
		try (DocumentExtractorNode.LimitedInputStream inputStream = new DocumentExtractorNode.LimitedInputStream(
				new ByteArrayInputStream(new byte[11]), 10)) {
			assertEquals(10, inputStream.skip(10));
			assertThrows(IOException.class, inputStream::read);
		}
	}

}
