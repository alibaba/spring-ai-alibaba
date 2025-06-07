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
import org.junit.jupiter.api.condition.DisabledIf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for arXiv client
 *
 * @author brianxiadong
 */

@DisabledIf("GithubCI")
public class ArxivClientTest {

	/**
	 * Check if the tests are running in Local. In GitHub CI environment, this test not
	 * running.
	 */
	static boolean GithubCI() {
		return "true".equals(System.getenv("ENABLE_TEST_CI"));
	}

	@Test
	public void testBasicSearch() throws IOException {
		// Create client
		ArxivClient client = new ArxivClient();

		// Create search
		ArxivSearch search = new ArxivSearch();
		search.setQuery("cat:cs.AI AND ti:\"artificial intelligence\"");
		search.setMaxResults(5);

		// Execute search
		Iterator<ArxivResult> results = client.results(search, 0);

		// Verify results
		List<ArxivResult> resultList = new ArrayList<>();
		results.forEachRemaining(resultList::add);

		assertEquals(5, resultList.size(), "Should return 5 results");

		// Verify basic information of the first result
		ArxivResult firstResult = resultList.get(0);
		assertNotNull(firstResult.getEntryId(), "Article ID should not be null");
		assertNotNull(firstResult.getTitle(), "Title should not be null");
		assertNotNull(firstResult.getAuthors(), "Author list should not be null");
		assertFalse(firstResult.getAuthors().isEmpty(), "Author list should not be empty");
		assertNotNull(firstResult.getSummary(), "Summary should not be null");
		assertNotNull(firstResult.getCategories(), "Category list should not be null");
		assertFalse(firstResult.getCategories().isEmpty(), "Category list should not be empty");
		assertTrue(firstResult.getCategories().contains("cs.AI"), "Should contain cs.AI category");
	}

	@Test
	public void testSearchWithIdList() throws IOException {
		ArxivClient client = new ArxivClient();

		ArxivSearch search = new ArxivSearch();
		List<String> idList = new ArrayList<>();
		idList.add("2501.01639v1"); // Replace with an actual existing article ID
		search.setIdList(idList);

		Iterator<ArxivResult> results = client.results(search, 0);

		List<ArxivResult> resultList = new ArrayList<>();
		results.forEachRemaining(resultList::add);

		assertFalse(resultList.isEmpty(), "Should return at least one result");
		assertEquals("2501.01639v1", resultList.get(0).getShortId(), "Should return the article with the specified ID");
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

		assertEquals(10, resultList.size(), "Should return 10 results");

		// Verify results are sorted by submission date in descending order
		for (int i = 1; i < resultList.size(); i++) {
			assertTrue(
					resultList.get(i - 1).getPublished().isAfter(resultList.get(i).getPublished())
							|| resultList.get(i - 1).getPublished().equals(resultList.get(i).getPublished()),
					"Results should be sorted by submission date in descending order");
		}
	}

	@Test
	public void testPagination() throws IOException {
		// Create client with longer delay to avoid rate limiting
		ArxivClient client = new ArxivClient(20, 5.0f, 3); // Set page size to 20

		ArxivSearch search = new ArxivSearch();
		// Use a more specific and reliable query that should always return results
		search.setQuery("cat:cs.AI AND ti:\"machine learning\"");
		// Set max results to a number larger than page size to ensure we get multiple
		// pages
		search.setMaxResults(50); // Request more than one page
		// Set sort order to ensure consistent results
		search.setSortBy(ArxivSortCriterion.RELEVANCE);
		search.setSortOrder(ArxivSortOrder.DESCENDING);

		// Get first page
		Iterator<ArxivResult> firstPage = client.results(search, 0);
		List<ArxivResult> firstPageResults = new ArrayList<>();

		// Only take at most pageSize (20) items from the iterator
		int count = 0;
		while (firstPage.hasNext() && count < 20) {
			firstPageResults.add(firstPage.next());
			count++;
		}

		// Print debug information
		System.out.println("First page results count: " + firstPageResults.size());

		// Verify we have results from first page
		assertTrue(firstPageResults.size() > 0, "First page should return results");
		assertTrue(firstPageResults.size() <= 20, "First page should not exceed page size");

		// Get second page
		Iterator<ArxivResult> secondPage = client.results(search, firstPageResults.size());
		List<ArxivResult> secondPageResults = new ArrayList<>();

		// Take only up to 20 results from second page
		count = 0;
		while (secondPage.hasNext() && count < 20) {
			secondPageResults.add(secondPage.next());
			count++;
		}

		System.out.println("Second page results count: " + secondPageResults.size());

		// Verify we have results from second page
		assertTrue(secondPageResults.size() > 0, "Second page should return results");
		assertTrue(secondPageResults.size() <= 20, "Second page should not exceed page size");

		// Verify results are different
		Set<String> firstPageIds = firstPageResults.stream().map(ArxivResult::getEntryId).collect(Collectors.toSet());
		Set<String> secondPageIds = secondPageResults.stream().map(ArxivResult::getEntryId).collect(Collectors.toSet());

		// Check for any overlap between pages
		Set<String> intersection = new HashSet<>(firstPageIds);
		intersection.retainAll(secondPageIds);
		assertTrue(intersection.isEmpty(), "Pages should not have overlapping results");
	}

	@Test
	public void testDownloadPdf() throws IOException {
		// Create client
		ArxivClient client = new ArxivClient();

		// Search for a specific paper
		ArxivSearch search = new ArxivSearch();
		search.setQuery("cat:cs.AI AND ti:\"artificial intelligence\"");
		search.setMaxResults(1);

		// Get search results
		Iterator<ArxivResult> results = client.results(search, 0);
		assertTrue(results.hasNext(), "Should have at least one search result");

		ArxivResult result = results.next();
		assertNotNull(result.getPdfUrl(), "PDF URL should not be null");

		// Create temporary directory for testing
		Path tempDir = Files.createTempDirectory("arxiv-test");

		// Test download with default filename
		Path defaultPath = client.downloadPdf(result, tempDir.toString());
		assertTrue(Files.exists(defaultPath), "PDF file should be downloaded");
		assertTrue(Files.size(defaultPath) > 0, "PDF file should not be empty");

		// Test download with custom filename
		String customFilename = "test_download.pdf";
		Path customPath = client.downloadPdf(result, tempDir.toString(), customFilename);
		assertTrue(Files.exists(customPath), "PDF file with custom filename should be downloaded");
		assertTrue(Files.size(customPath) > 0, "PDF file with custom filename should not be empty");
		assertEquals(customFilename, customPath.getFileName().toString(), "Filename should match the custom name");

		// Verify both files have the same content
		assertArrayEquals(Files.readAllBytes(defaultPath), Files.readAllBytes(customPath),
				"Both downloaded files should have the same content");

		// Clean up temporary files
		Files.deleteIfExists(defaultPath);
		Files.deleteIfExists(customPath);
		Files.deleteIfExists(tempDir);
	}

}
