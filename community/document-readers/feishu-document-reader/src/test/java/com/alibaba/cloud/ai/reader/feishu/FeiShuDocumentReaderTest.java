package com.alibaba.cloud.ai.reader.feishu;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

public class FeiShuDocumentReaderTest {
   private static final Logger log = LoggerFactory.getLogger(FeiShuDocumentReaderTest.class);

    private FeiShuDocumentReader feiShuDocumentReader;
    private FeiShuResource feiShuResource;

    void create() {
        feiShuResource = FeiShuResource.builder()
                .appId("cli_a7c6b258ae3c9013")
                .appSecret("KeifrxEeKvGHXJKxkOFRrfteovAOHwFy")
                .build();
    }
    @Test
    void feiShuDocumentTest() {
        create();
        feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource);
        List<Document> documentList = feiShuDocumentReader.get();
        log.info("result:{}", documentList);
    }
    @Test
    void feiShuDocumentTestByUserToken() {
        create();
        feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, "u-esTKL7nYJ0Sa60TNcQflx9h41.6wk4lFgG00llS2w4oy");
        List<Document> documentList = feiShuDocumentReader.get();
        log.info("result:{}", documentList);
    }
    @Test
    void feiShuDocumentTestByUserTokenAndDocumentId() {
        create();
        feiShuDocumentReader = new FeiShuDocumentReader(feiShuResource, "u-esTKL7nYJ0Sa60TNcQflx9h41.6wk4lFgG00llS2w4oy", "QdVwdxUKaoVuk5xGe34cm8PonBf");
        List<Document> documentList = feiShuDocumentReader.get();
        log.info("result:{}", documentList);
    }
}
