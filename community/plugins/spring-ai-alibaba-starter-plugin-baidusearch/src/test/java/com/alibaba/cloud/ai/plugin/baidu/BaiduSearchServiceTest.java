package com.alibaba.cloud.ai.plugin.baidu;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BaiduSearchServiceTest {
    
    private BaiduSearchService baiduSearchService;
    
    @BeforeEach
    public void setUp() {
        baiduSearchService = new BaiduSearchService();
    }
    
    @Test
    public void testSearch() {
        BaiduSearchService.Request request = new BaiduSearchService.Request("阿里巴巴上市时间", 10);
        BaiduSearchService.Response response = baiduSearchService.apply(request);
        
        assertNotNull(response);
        assertFalse(response.results().isEmpty());
        assertEquals(10, response.results().size());
        
        for (BaiduSearchService.SearchResult result : response.results()) {
            assertNotNull(result.title());
            assertNotNull(result.abstractText());
        }
    }
}