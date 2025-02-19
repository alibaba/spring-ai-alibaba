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
package com.alibaba.cloud.ai.reader.arxiv;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * arXiv文档阅读器测试类
 *
 * @author brianxiadong
 */
public class ArxivDocumentReaderTest {

	private static final String TEST_QUERY = "cat:cs.AI AND ti:\"artificial intelligence\"";

	private static final int MAX_SIZE = 2;

	@Test
	public void testDocumentReader() {
		// 创建文档阅读器
		ArxivDocumentReader reader = new ArxivDocumentReader(TEST_QUERY, MAX_SIZE);

		// 获取文档
		List<Document> documents = reader.get();

		// 验证结果
		assertFalse(documents.isEmpty(), "应该返回至少一个文档");

		// 验证第一个文档的元数据
		Document firstDoc = documents.get(0);
		assertNotNull(firstDoc.getContent(), "文档内容不应为空");

		// 验证元数据
		var metadata = firstDoc.getMetadata();
		assertNotNull(metadata.get(ArxivResource.ENTRY_ID), "应包含文章ID");
		assertNotNull(metadata.get(ArxivResource.TITLE), "应包含标题");
		assertNotNull(metadata.get(ArxivResource.AUTHORS), "应包含作者");
		assertNotNull(metadata.get(ArxivResource.SUMMARY), "应包含摘要");
		assertNotNull(metadata.get(ArxivResource.CATEGORIES), "应包含分类");
		assertNotNull(metadata.get(ArxivResource.PRIMARY_CATEGORY), "应包含主分类");
		assertNotNull(metadata.get(ArxivResource.PDF_URL), "应包含PDF URL");

		// 验证分类
		@SuppressWarnings("unchecked")
		List<String> categories = (List<String>) metadata.get(ArxivResource.CATEGORIES);
		assertTrue(categories.contains("cs.AI"), "应该包含cs.AI分类");
	}

	@Test
	public void testGetSummaries() {
		// 创建文档阅读器
		ArxivDocumentReader reader = new ArxivDocumentReader(TEST_QUERY, MAX_SIZE);

		// 获取摘要文档列表
		List<Document> documents = reader.getSummaries();

		// 验证结果
		assertFalse(documents.isEmpty(), "应该返回至少一个文档");

		// 验证第一个文档
		Document firstDoc = documents.get(0);

		// 验证内容（摘要）
		assertNotNull(firstDoc.getContent(), "文档内容（摘要）不应为空");
		assertFalse(firstDoc.getContent().trim().isEmpty(), "文档内容（摘要）不应为空字符串");

		// 验证元数据
		var metadata = firstDoc.getMetadata();
		assertNotNull(metadata.get(ArxivResource.ENTRY_ID), "应包含文章ID");
		assertNotNull(metadata.get(ArxivResource.TITLE), "应包含标题");
		assertNotNull(metadata.get(ArxivResource.AUTHORS), "应包含作者");
		assertNotNull(metadata.get(ArxivResource.SUMMARY), "应包含摘要");
		assertNotNull(metadata.get(ArxivResource.CATEGORIES), "应包含分类");
		assertNotNull(metadata.get(ArxivResource.PRIMARY_CATEGORY), "应包含主分类");
		assertNotNull(metadata.get(ArxivResource.PDF_URL), "应包含PDF URL");

		// 验证摘要内容与元数据中的摘要一致
		assertEquals(firstDoc.getContent(), metadata.get(ArxivResource.SUMMARY), "文档内容应该与元数据中的摘要一致");
	}

}
