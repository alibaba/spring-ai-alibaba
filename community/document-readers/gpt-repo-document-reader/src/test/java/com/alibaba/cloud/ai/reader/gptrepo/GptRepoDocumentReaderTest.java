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
 * GptRepoDocumentReader的单元测试类
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
		// 创建一个模拟的Git仓库结构
		repoPath = tempDir.resolve("test-repo");
		Files.createDirectories(repoPath);

		// 创建测试文件
		createTestFile(repoPath.resolve("src/main/java/TestFile.java"), TEST_FILE_CONTENT);
		createTestFile(repoPath.resolve("src/main/python/test.py"), TEST_PYTHON_CONTENT);
		createTestFile(repoPath.resolve(".gptignore"), "*.log\n*.tmp\n");
	}

	/**
	 * 测试基本的文档读取功能
	 */
	@Test
	void testBasicDocumentReading() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		assertNotNull(documents);
		assertFalse(documents.isEmpty());
		assertTrue(documents.size() >= 2); // Should have at least two files read

		// 验证文档内容
		boolean foundJavaFile = false;
		boolean foundPythonFile = false;

		for (Document doc : documents) {
			String content = doc.getContent();
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
	 * 测试文件合并功能
	 */
	@Test
	void testConcatenatedReading() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), true, // concatenate
				null, "UTF-8");

		List<Document> documents = reader.get();

		assertEquals(1, documents.size(), "Should only have one concatenated document");
		Document doc = documents.get(0);

		// 验证合并后的文档包含所有文件内容
		String content = doc.getContent();
		assertTrue(content.contains(TEST_FILE_CONTENT));
		assertTrue(content.contains(TEST_PYTHON_CONTENT));
		assertTrue(content.contains("----")); // Verify separator exists
	}

	/**
	 * 测试文件扩展名过滤功能
	 */
	@Test
	void testExtensionFiltering() {
		// 只读取Java文件
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), false, Arrays.asList("java"),
				"UTF-8");

		List<Document> documents = reader.get();

		assertFalse(documents.isEmpty());
		assertEquals(1, documents.size(), "Should only find one Java file");

		Document doc = documents.get(0);
		assertTrue(doc.getContent().contains("TestFile.java"));
		assertFalse(doc.getContent().contains("test.py"));
	}

	/**
	 * 测试.gptignore文件功能
	 */
	@Test
	void testIgnorePatterns() throws IOException {
		// 创建一个应该被忽略的文件
		createTestFile(repoPath.resolve("test.log"), "This should be ignored");

		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		// 验证.log文件被忽略
		for (Document doc : documents) {
			assertFalse(doc.getContent().contains("test.log"), "Should not contain ignored .log file");
		}
	}

	/**
	 * 测试元数据
	 */
	@Test
	void testDocumentMetadata() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		assertFalse(documents.isEmpty());
		Document doc = documents.get(0);

		// 验证元数据
		assertNotNull(doc.getMetadata());
		assertTrue(doc.getMetadata().containsKey("source"));
		assertEquals(repoPath.toString(), doc.getMetadata().get("source"));
	}

	/**
	 * 测试无效路径处理
	 */
	@Test
	void testInvalidPath() {
		Path invalidPath = tempDir.resolve("non-existent-repo");
		GptRepoDocumentReader reader = new GptRepoDocumentReader(invalidPath.toString());

		assertThrows(RuntimeException.class, () -> reader.get());
	}

	/**
	 * 辅助方法：创建测试文件
	 */
	private void createTestFile(Path filePath, String content) throws IOException {
		Files.createDirectories(filePath.getParent());
		Files.writeString(filePath, content);
	}

	/**
	 * 测试自定义前导文本
	 */
	@Test
	void testCustomPreamble() {
		String customPreamble = "This is a custom preamble text.\n";
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), false, null, "UTF-8",
				customPreamble);

		List<Document> documents = reader.get();
		assertFalse(documents.isEmpty());
		Document doc = documents.get(0);
		assertTrue(doc.getContent().startsWith(customPreamble));
	}

	/**
	 * 测试文件元数据
	 */
	@Test
	void testFileMetadata() {
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString());
		List<Document> documents = reader.get();

		assertFalse(documents.isEmpty());

		// 查找Java文件的文档
		Optional<Document> javaDoc = documents.stream()
			.filter(doc -> doc.getContent().contains("TestFile.java"))
			.findFirst();

		assertTrue(javaDoc.isPresent());
		Document doc = javaDoc.get();

		// 验证元数据
		Map<String, Object> metadata = doc.getMetadata();
		assertNotNull(metadata);
		assertEquals("TestFile.java", metadata.get("file_name"));
		assertEquals("src/main/java", metadata.get("directory"));
		assertEquals("src/main/java/TestFile.java", metadata.get("file_path"));
		assertEquals(repoPath.toString(), metadata.get("source"));
	}

	@Test
	void testLocalRepo() {
		String customPreamble = "Repository content analysis:\n";
		GptRepoDocumentReader reader = new GptRepoDocumentReader("/path/to/repo", true,
				Collections.singletonList("your_file_extention"), "UTF-8", null);

		List<Document> documents = reader.get();
		assertFalse(documents.isEmpty());
		Document doc = documents.get(0);
		assertTrue(doc.getContent().startsWith(customPreamble));

		// 打印文档数量和第一个文档的元数据
		System.out.println("Total documents: " + documents.size());
		if (!documents.isEmpty()) {
			System.out.println("First document metadata: " + doc.getMetadata());
		}
	}

	/**
	 * 测试不同编码的文件读取
	 */
	@Test
	void testDifferentEncoding() throws IOException {
		// 创建一个包含中文内容的文件
		String chineseContent = "这是一个测试文件\n包含中文内容";
		Path chineseFile = repoPath.resolve("src/main/resources/chinese.txt");
		Files.createDirectories(chineseFile.getParent());
		Files.write(chineseFile, chineseContent.getBytes("GBK"));

		// 使用GBK编码读取（应该成功）
		GptRepoDocumentReader reader = new GptRepoDocumentReader(repoPath.toString(), false,
				Collections.singletonList("txt"), "GBK");

		List<Document> documents = reader.get();

		// 查找中文文件的文档
		Optional<Document> chineseDoc = documents.stream()
			.filter(doc -> doc.getContent().contains("chinese.txt"))
			.findFirst();

		assertTrue(chineseDoc.isPresent());
		Document doc = chineseDoc.get();
		assertTrue(doc.getContent().contains(chineseContent));

		// 使用错误的编码读取（应该抛出异常）
		reader = new GptRepoDocumentReader(repoPath.toString(), false, Collections.singletonList("txt"), "UTF-8");

		// 验证使用错误的编码时会抛出异常
		assertThrows(RuntimeException.class, reader::get,
				"Reading GBK encoded file with UTF-8 encoding should throw an exception");
	}

}