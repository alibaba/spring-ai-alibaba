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

import com.alibaba.cloud.ai.reader.arxiv.client.*;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * arXiv客户端测试类
 *
 * @author brianxiadong
 */
public class ArxivClientTest {

	@Test
	public void testBasicSearch() throws IOException {
		// 创建客户端
		ArxivClient client = new ArxivClient();

		// 创建搜索
		ArxivSearch search = new ArxivSearch();
		search.setQuery("cat:cs.AI AND ti:\"artificial intelligence\"");
		search.setMaxResults(5);

		// 执行搜索
		Iterator<ArxivResult> results = client.results(search, 0);

		// 验证结果
		List<ArxivResult> resultList = new ArrayList<>();
		results.forEachRemaining(resultList::add);

		assertEquals(5, resultList.size(), "应该返回5个结果");

		// 验证第一个结果的基本信息
		ArxivResult firstResult = resultList.get(0);
		assertNotNull(firstResult.getEntryId(), "文章ID不应为空");
		assertNotNull(firstResult.getTitle(), "标题不应为空");
		assertNotNull(firstResult.getAuthors(), "作者列表不应为空");
		assertFalse(firstResult.getAuthors().isEmpty(), "作者列表不应为空");
		assertNotNull(firstResult.getSummary(), "摘要不应为空");
		assertNotNull(firstResult.getCategories(), "分类列表不应为空");
		assertFalse(firstResult.getCategories().isEmpty(), "分类列表不应为空");
		assertTrue(firstResult.getCategories().contains("cs.AI"), "应该包含cs.AI分类");
	}

	@Test
	public void testSearchWithIdList() throws IOException {
		ArxivClient client = new ArxivClient();

		ArxivSearch search = new ArxivSearch();
		List<String> idList = new ArrayList<>();
		idList.add("2501.01639v1"); // 替换为实际存在的文章ID
		search.setIdList(idList);

		Iterator<ArxivResult> results = client.results(search, 0);

		List<ArxivResult> resultList = new ArrayList<>();
		results.forEachRemaining(resultList::add);

		assertFalse(resultList.isEmpty(), "应该至少返回一个结果");
		assertEquals("2501.01639v1", resultList.get(0).getShortId(), "应该返回指定ID的文章");
	}

	@Test
	public void testSearchWithSorting() throws IOException {
		ArxivClient client = new ArxivClient();

		ArxivSearch search = new ArxivSearch();
		search.setQuery("cat:cs.AI AND ti:\"artificial intelligence\"");
		search.setMaxResults(10);
		search.setSortBy(ArxivSortCriterion.SUBMITTED_DATE);
		search.setSortOrder(ArxivSortOrder.DESCENDING);

		Iterator<ArxivResult> results = client.results(search, 0);

		List<ArxivResult> resultList = new ArrayList<>();
		results.forEachRemaining(resultList::add);

		assertEquals(10, resultList.size(), "应该返回10个结果");

		// 验证结果是按提交日期降序排序的
		for (int i = 1; i < resultList.size(); i++) {
			assertTrue(
					resultList.get(i - 1).getPublished().isAfter(resultList.get(i).getPublished())
							|| resultList.get(i - 1).getPublished().equals(resultList.get(i).getPublished()),
					"结果应该按提交日期降序排序");
		}
	}

	@Test
	public void testPagination() throws IOException {
		ArxivClient client = new ArxivClient();

		ArxivSearch search = new ArxivSearch();
		search.setQuery("cat:math");
		search.setMaxResults(15);

		// 获取第一页
		Iterator<ArxivResult> firstPage = client.results(search, 0);
		List<ArxivResult> firstPageResults = new ArrayList<>();
		firstPage.forEachRemaining(firstPageResults::add);

		// 获取第二页
		Iterator<ArxivResult> secondPage = client.results(search, 10);
		List<ArxivResult> secondPageResults = new ArrayList<>();
		secondPage.forEachRemaining(secondPageResults::add);

		assertEquals(10, firstPageResults.size(), "第一页应该返回10个结果");
		assertEquals(5, secondPageResults.size(), "第二页应该返回5个结果");

		// 验证两页的结果不重复
		for (ArxivResult firstPageResult : firstPageResults) {
			for (ArxivResult secondPageResult : secondPageResults) {
				assertNotEquals(firstPageResult.getEntryId(), secondPageResult.getEntryId(), "不同页的结果不应重复");
			}
		}
	}

	@Test
	public void testDownloadPdf() throws IOException {
		// 创建客户端
		ArxivClient client = new ArxivClient();

		// 搜索一篇特定的论文
		ArxivSearch search = new ArxivSearch();
		search.setQuery("cat:cs.AI AND ti:\"artificial intelligence\"");
		search.setMaxResults(1);

		// 获取搜索结果
		Iterator<ArxivResult> results = client.results(search, 0);
		assertTrue(results.hasNext(), "应该至少有一个搜索结果");

		ArxivResult result = results.next();
		assertNotNull(result.getPdfUrl(), "PDF URL不应为空");

		// 测试使用默认文件名下载
		Path defaultPath = client.downloadPdf(result, "/Users/your_name/Documents/test");
		assertTrue(Files.exists(defaultPath), "PDF文件应该已下载");
		assertTrue(Files.size(defaultPath) > 0, "PDF文件不应为空");

		// 测试使用自定义文件名下载
		String customFilename = "test_download.pdf";
		Path customPath = client.downloadPdf(result, "/Users/your_name/Documents/test", customFilename);
		assertTrue(Files.exists(customPath), "使用自定义文件名的PDF文件应该已下载");
		assertTrue(Files.size(customPath) > 0, "使用自定义文件名的PDF文件不应为空");
		assertEquals(customFilename, customPath.getFileName().toString(), "文件名应该匹配自定义名称");

		// 验证两个文件内容相同
		assertArrayEquals(Files.readAllBytes(defaultPath), Files.readAllBytes(customPath), "两次下载的文件内容应该相同");
	}

}
