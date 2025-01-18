package com.alibaba.cloud.ai.reader.bilibili;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @Author: XiaoYunTao
 * @Date: 2025/1/18
 */
public class BilibiliDocumentReaderTest {

    private static final Logger logger = LoggerFactory.getLogger(BilibiliDocumentReader.class);

    @Test
    void bilibiliDocumentReaderTest() {
        BilibiliDocumentReader bilibiliDocumentReader = new BilibiliDocumentReader("https://www.bilibili.com/video/BV1KMwgeKECx/?t=7&vd_source=3069f51b168ac07a9e3c4ba94ae26af5");
        List<Document> documents = bilibiliDocumentReader.get();
        logger.info("documents: {}", documents);
    }
}
