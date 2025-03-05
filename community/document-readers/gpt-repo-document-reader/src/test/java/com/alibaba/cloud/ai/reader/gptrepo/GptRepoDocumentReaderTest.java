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
package com.alibaba.cloud.ai.reader.gptrepo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.document.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for GptRepoDocumentReader
 *
 * @author brianxiadong
 */
class GptRepoDocumentReaderTest {

	@TempDir
	Path tempDir;

	private Path repoPath;

	private static final String TEST_FILE_CONTENT = "public class TestFile {\n    // This is a test file\n}";

	private static final String TEST_PYTHON_CONTENT = "def test_function():\n    # This is a test function\n    pass";

	@BeforeEach
	void setUp() throws IOException {
		// Create a mock Git repository structure
		repoPath = tempDir.resolve("test-repo");
		Files.createDirectories(repoPath);

		// Create test files
		createTestFile(repoPath.resolve("src/main/java/TestFile.java"), TEST_FILE_CONTENT);
		createTestFile(repoPath.resolve("src/main/python/test.py"), TEST_PYTHON_CONTENT);
		createTestFile(repoPath.resolve(".gptignore"), "*.log\n*.tmp\n");
	}

	/**
	 * Test basic document reading functionality
	 */
	@Test
	void testBasicDocumentReading() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		assertNotNull(documents);
		assertFalse(documents.isEmpty());
		assertTrue(documents.size() >= 2); // Should have at least two files read

		// Verify document content
		boolean foundJavaFile = false;
		boolean foundPythonFile = false;

		for (Document doc : documents) {
			String content = doc.getText();
			if (content.contains("TestFile.java")) {
				foundJavaFile = true;
				assertTrue(content.contains(TEST_FILE_CONTENT));
			}
			if (content.contains("test.py")) {
				foundPythonFile = true;
				assertTrue(content.contains(TEST_PYTHON_CONTENT));
			}
		}

		assertTrue(foundJavaFile, "Should find Java test file");
		assertTrue(foundPythonFile, "Should find Python test file");
	}

	/**
	 * Test file concatenation functionality
	 */
	@Test
	void testConcatenatedReading() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), true, // concatenate
				null, "UTF-8");

		List<Document> documents = reader.get();

		assertEquals(1, documents.size(), "Should only have one concatenated document");
		Document doc = documents.get(0);

		// Verify concatenated document contains all file contents
		String content = doc.getText();
		assertTrue(content.contains(TEST_FILE_CONTENT));
		assertTrue(content.contains(TEST_PYTHON_CONTENT));
		assertTrue(content.contains("----")); // Verify separator exists
	}

	/**
	 * Test file extension filtering functionality
	 */
	@Test
	void testExtensionFiltering() {
		// Only read Java files
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), false, Arrays.asList("java"),
				"UTF-8");

		List<Document> documents = reader.get();

		assertFalse(documents.isEmpty());
		assertEquals(1, documents.size(), "Should only find one Java file");

		Document doc = documents.get(0);
		assertTrue(doc.getText().contains("TestFile.java"));
		assertFalse(doc.getText().contains("test.py"));
	}

	/**
	 * Test .gptignore file functionality
	 */
	@Test
	void testIgnorePatterns() throws IOException {
		// Create a file that should be ignored
		createTestFile(repoPath.resolve("test.log"), "This should be ignored");

		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		// Verify .log file is ignored
		for (Document doc : documents) {
			assertFalse(doc.getText().contains("test.log"), "Should not contain ignored .log file");
		}
	}

	/**
	 * Test metadata
	 */
	@Test
	void testDocumentMetadata() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		assertFalse(documents.isEmpty());
		Document doc = documents.get(0);

		// Verify metadata
		assertNotNull(doc.getMetadata());
		assertTrue(doc.getMetadata().containsKey("source"));
		assertEquals(repoPath.toString(), doc.getMetadata().get("source"));
	}

	/**
	 * Test invalid path handling
	 */
	@Test
	void testInvalidPath() {
		Path invalidPath = tempDir.resolve("non-existent-repo");
		GptRepoDocumentReader reader = new GptRepoDocumentReader(invalidPath.toString());

		assertThrows(RuntimeException.class, () -> reader.get());
	}

	/**
	 * Helper method: Create test file
	 */
	private void createTestFile(Path filePath, String content) throws IOException {
		Files.createDirectories(filePath.getParent());
		Files.writeString(filePath, content);
	}

	/**
	 * Test custom preamble text
	 */
	@Test
	void testCustomPreamble() {
		String customPreamble = "This is a custom preamble text.\n";
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), false, null, "UTF-8",
				customPreamble);

		List<Document> documents = reader.get();
		assertFalse(documents.isEmpty());
		Document doc = documents.get(0);
		assertTrue(doc.getText().startsWith(customPreamble));
	}

	/**
	 * Test file metadata
	 */
	@Test
	void testFileMetadata() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		assertFalse(documents.isEmpty());

		// Find Java file document
		Optional<Document> javaDoc = documents.stream()
			.filter(doc -> doc.getText().contains("TestFile.java"))
			.findFirst();

		assertTrue(javaDoc.isPresent());
		Document doc = javaDoc.get();

		// Verify metadata
		Map<String, Object> metadata = doc.getMetadata();
		assertNotNull(metadata);
		assertEquals("TestFile.java", metadata.get("file_name"));
		assertEquals("src/main/java", metadata.get("directory"));
		assertEquals("src/main/java/TestFile.java", metadata.get("file_path"));
		assertEquals(repoPath.toString(), metadata.get("source"));
	}

	@Test
	void testLocalRepo() throws IOException {
		// Create a specific test file for this test
		Path localRepoPath = tempDir.resolve("local-test-repo");
		Files.createDirectories(localRepoPath);

		// Create a sample file with specific content
		String sampleFileContent = "# Sample Repository\n\nThis is a sample repository file for testing.\n\n## Features\n\n- Feature 1\n- Feature 2\n- Feature 3";
		createTestFile(localRepoPath.resolve("README.md"), sampleFileContent);

		// Create a Java file
		String javaFileContent = "package com.example;\n\n/**\n * Sample Java class\n */\npublic class Sample {\n    public static void main(String[] args) {\n        System.out.println(\"Hello, World!\");\n    }\n}";
		createTestFile(localRepoPath.resolve("src/main/java/com/example/Sample.java"), javaFileContent);

		// Create a custom preamble
		String customPreamble = "Repository content analysis:\n";

		// Initialize the reader with the local repository path
		GptRepoDocumentReader reader = new GptRepoDocumentReader(localRepoPath.toString(), true,
				Arrays.asList("md", "java"), "UTF-8", customPreamble);

		// Get documents
		List<Document> documents = reader.get();

		// Verify results
		assertFalse(documents.isEmpty());
		assertEquals(1, documents.size(), "Should have one concatenated document");

		Document doc = documents.get(0);
		assertTrue(doc.getText().startsWith(customPreamble), "Document should start with custom preamble");
		assertTrue(doc.getText().contains(sampleFileContent), "Document should contain README.md content");
		assertTrue(doc.getText().contains(javaFileContent), "Document should contain Sample.java content");

		// Verify metadata
		assertNotNull(doc.getMetadata());
		assertEquals(localRepoPath.toString(), doc.getMetadata().get("source"));

		// Print document information for debugging
		System.out.println("Total documents: " + documents.size());
		System.out.println("Document metadata: " + doc.getMetadata());
	}

	/**
	 * Test reading files with different encodings
	 */
	@Test
	void testDifferentEncoding() throws IOException {
		// Create a file with Chinese content
		String chineseContent = "这是一个测试文件\n包含中文内容";
		Path chineseFile = repoPath.resolve("src/main/resources/chinese.txt");
		Files.createDirectories(chineseFile.getParent());
		Files.write(chineseFile, chineseContent.getBytes("GBK"));

		// Read with GBK encoding (should succeed)
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), false,
				Collections.singletonList("txt"), "GBK");

		List<Document> documents = reader.get();

		// Find Chinese file document
		Optional<Document> chineseDoc = documents.stream()
			.filter(doc -> doc.getText().contains("chinese.txt"))
			.findFirst();

		assertTrue(chineseDoc.isPresent());
		Document doc = chineseDoc.get();
		assertTrue(doc.getText().contains(chineseContent));

		// Read with wrong encoding (should throw exception)
		reader = new GptRepoDocumentReader(repoPath.toString(), false, Collections.singletonList("txt"), "UTF-8");

		// Verify exception is thrown when using wrong encoding
		assertThrows(RuntimeException.class, reader::get,
				"Reading GBK encoded file with UTF-8 encoding should throw an exception");
	}

}
