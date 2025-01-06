package com.alibaba.cloud.ai.reader.arxiv;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * arXiv客户端测试类
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
        idList.add("2101.00123");  // 替换为实际存在的文章ID
        search.setIdList(idList);
        
        Iterator<ArxivResult> results = client.results(search, 0);
        
        List<ArxivResult> resultList = new ArrayList<>();
        results.forEachRemaining(resultList::add);
        
        assertFalse(resultList.isEmpty(), "应该至少返回一个结果");
        assertEquals("2101.00123", resultList.get(0).getShortId(), "应该返回指定ID的文章");
    }

    @Test
    public void testSearchWithSorting() throws IOException {
        ArxivClient client = new ArxivClient();
        
        ArxivSearch search = new ArxivSearch();
        search.setQuery("cat:physics");
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
                resultList.get(i-1).getPublished().isAfter(resultList.get(i).getPublished()) ||
                resultList.get(i-1).getPublished().equals(resultList.get(i).getPublished()),
                "结果应该按提交日期降序排序"
            );
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
                assertNotEquals(
                    firstPageResult.getEntryId(),
                    secondPageResult.getEntryId(),
                    "不同页的结果不应重复"
                );
            }
        }
    }
} 