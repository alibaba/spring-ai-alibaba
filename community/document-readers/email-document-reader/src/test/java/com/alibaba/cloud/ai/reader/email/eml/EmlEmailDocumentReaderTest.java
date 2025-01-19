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
package com.alibaba.cloud.ai.reader.email.eml;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test cases for EmlEmailDocumentReader Tests various email scenarios including: 1.
 * GitHub pull request email 2. HTML recruitment email 3. Code review comment email
 *
 * @author brianxiadong
 * @since 2024-01-19
 */
class EmlEmailDocumentReaderTest {

	@Test
	void should_read_pull_request_email() throws IOException {
		// Given
		ClassPathResource emailResource = new ClassPathResource("1.eml");
		EmlEmailDocumentReader reader = new EmlEmailDocumentReader(emailResource.getFile().getAbsolutePath());

		// When
		List<Document> documents = reader.get();

		// Then
		assertNotNull(documents);
		assertEquals(2, documents.size());

		Document emailDoc = documents.get(0);
		Map<String, Object> metadata = emailDoc.getMetadata();

		// Verify metadata
		assertEquals("Re: [test/project] Feat : Document Reader , close #123 (PR #456)", metadata.get("subject"));
		assertEquals("notifications@example.com", metadata.get("from"));
		assertEquals("John smith", metadata.get("from_name"));
		assertEquals("project@noreply.example.com", metadata.get("to"));
		assertEquals("Test/project", metadata.get("to_name"));
		assertEquals("Thu, 16 Jan 2025 13:50:25 +0800", metadata.get("date"));

		// Verify content
		String content = emailDoc.getText();
		assertTrue(content.contains("@reviewer approved this pull request"));
	}

	@Test
	void should_read_html_recruitment_email() throws IOException {
		// Given
		ClassPathResource emailResource = new ClassPathResource("2.eml");
		EmlEmailDocumentReader reader = new EmlEmailDocumentReader(emailResource.getFile().getAbsolutePath());

		// When
		List<Document> documents = reader.get();

		// Then
		assertNotNull(documents);
		assertEquals(1, documents.size());

		Document emailDoc = documents.get(0);
		Map<String, Object> metadata = emailDoc.getMetadata();

		// Verify metadata
		assertEquals("您有新的通信评论", metadata.get("subject"));
		assertEquals("hr@example.com", metadata.get("from"));
		assertEquals("作人参加通信", metadata.get("from_name"));
		assertEquals("test@example.com", metadata.get("to"));
		assertEquals("Fri, 17 Jan 2025 12:30:37 +0800", metadata.get("date"));

		// Verify content type
		assertEquals("text/html; charset=\"utf-8\"", metadata.get("content_type"));

		// Verify content (HTML should be parsed)
		String content = emailDoc.getText();
		assertTrue(content.contains("工作地点：北京中心区"));
		assertTrue(content.contains("工作时间：周一至 周五 9:00-18:00"));
	}

	@Test
	void should_read_code_review_comment_email() throws IOException {
		// Given
		ClassPathResource emailResource = new ClassPathResource("3.eml");
		EmlEmailDocumentReader reader = new EmlEmailDocumentReader(emailResource.getFile().getAbsolutePath());

		// When
		List<Document> documents = reader.get();

		// Then
		assertNotNull(documents);
		assertEquals(2, documents.size()); // 应该生成两个文档

		// 验证第一个文档（邮件正文）
		Document emailDoc = documents.get(0);
		Map<String, Object> metadata = emailDoc.getMetadata();

		// Verify metadata
		assertEquals("Re: [test/project] feat(document-readers): add new feature (PR #789)", metadata.get("subject"));
		assertEquals("notifications@example.com", metadata.get("from"));
		assertEquals("Reviewer", metadata.get("from_name"));
		assertEquals("project@noreply.example.com", metadata.get("to"));
		assertEquals("Test/project", metadata.get("to_name"));
		assertEquals("Fri, 17 Jan 2025 22:52:00 +0800", metadata.get("date"));

		// Verify content
		String content = emailDoc.getText();
		assertTrue(content.contains("@reviewer commented on this pull request"));
		assertTrue(content.contains("是否需要删除这个依赖吗？"));

		// 验证第二个文档（评论内容）
		Document commentDoc = documents.get(1);
		// 验证评论内容
		String commentContent = commentDoc.getText();
		assertTrue(commentContent.contains("是否需要删除这个依赖吗？"));
	}

	@Test
	void should_read_email_with_attachments() throws IOException {
		// Given
		ClassPathResource emailResource = new ClassPathResource("4.eml");
		EmlEmailDocumentReader reader = new EmlEmailDocumentReader(emailResource.getFile().getAbsolutePath(), true);

		// When
		List<Document> documents = reader.get();

		// Then
		assertNotNull(documents);
		assertEquals(3, documents.size());

		// 验证邮件正文
		Document emailDoc = documents.get(0);
		Map<String, Object> metadata = emailDoc.getMetadata();

		// Verify metadata
		assertEquals("附件测试", metadata.get("subject"));
		assertEquals("Sun, 19 Jan 2025 18:06:31 +0800", metadata.get("date"));

		// Verify content
		String content = emailDoc.getText();
		assertTrue(content.contains("测试下html类型的附件"));

		// 验证附件
		Document attachmentDoc = documents.get(2);
		Map<String, Object> attachmentMetadata = attachmentDoc.getMetadata();

		assertEquals("chat.html", attachmentMetadata.get("filename"));
		assertEquals("text/html; name=\"chat.html\"", attachmentMetadata.get("content_type"));
		assertTrue(attachmentDoc.getText().contains("ChatGPT Data Export"));
	}

	@Test
	void should_decode_q_encoded_subject() throws IOException {
		// Given
		String testContent = "Subject: =?utf-8?Q?Re:_[test/project]_feat(document-readers)?= =?utf-8?Q?:_add_new_feature_(PR_#789)?=\n"
				+ "From: notifications@example.com\n" + "To: test@example.com\n"
				+ "Date: Thu, 17 Jan 2025 12:30:37 +0800\n" + "Content-Type: text/plain; charset=\"UTF-8\"\n\n"
				+ "This is a test email with Q-encoded subject.";

		File tempFile = File.createTempFile("test", ".eml");
		Files.writeString(tempFile.toPath(), testContent);

		EmlEmailDocumentReader reader = new EmlEmailDocumentReader(tempFile.getAbsolutePath());

		try {
			// When
			List<Document> documents = reader.get();

			// Then
			assertNotNull(documents);
			assertEquals(1, documents.size());

			Document emailDoc = documents.get(0);
			Map<String, Object> metadata = emailDoc.getMetadata();

			// 验证解码后的主题
			assertEquals("Re: [test/project] feat(document-readers): add new feature (PR #789)",
					metadata.get("subject"));
		}
		finally {
			tempFile.delete();
		}
	}

	@Test
	void should_handle_base64_encoded_email_headers() throws IOException {
		// Given
		String testContent = "From: =?utf-8?B?5L2c5Lq65Y+C5Yqg6YCa5L+h?= <hr@example.com>\n" + "To: test@example.com\n"
				+ "Subject: Test Email\n" + "Date: Thu, 17 Jan 2025 12:30:37 +0800\n"
				+ "Content-Type: text/plain; charset=\"UTF-8\"\n\n"
				+ "This is a test email with Base64 encoded headers.";

		File tempFile = File.createTempFile("test", ".eml");
		Files.writeString(tempFile.toPath(), testContent);

		EmlEmailDocumentReader reader = new EmlEmailDocumentReader(tempFile.getAbsolutePath());

		try {
			// When
			List<Document> documents = reader.get();

			// Then
			assertNotNull(documents);
			assertEquals(1, documents.size());

			Document emailDoc = documents.get(0);
			Map<String, Object> metadata = emailDoc.getMetadata();

			assertEquals("hr@example.com", metadata.get("from"));
			assertEquals("Thu, 17 Jan 2025 12:30:37 +0800", metadata.get("date"));
			assertTrue(emailDoc.getText().contains("This is a test email with Base64 encoded headers"));
		}
		finally {
			tempFile.delete();
		}
	}

}
