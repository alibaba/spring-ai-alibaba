package com.alibaba.cloud.ai.reader.yuque;

import com.alibaba.cloud.ai.reader.DocumentParser;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * @author YunLong
 */
public class YuQueDocumentReader implements DocumentReader {

    private DocumentReader parser;

    private final YuQueResource yuQueResource;

    public YuQueDocumentReader(YuQueResource yuQueResource, DocumentParser parserType) {
        this(yuQueResource, parserType.getParser(yuQueResource));
    }

    public YuQueDocumentReader(YuQueResource yuQueResource, DocumentParser parserType, ExtractedTextFormatter formatter) {
        this(yuQueResource, parserType.getParser(yuQueResource, formatter));
    }

    public YuQueDocumentReader(YuQueResource yuQueResource, DocumentReader parser) {
        this.yuQueResource = yuQueResource;
        this.parser = parser;
    }

    @Override
    public List<Document> get() {
        List<Document> documents = parser.get();
        String source = yuQueResource.getResourcePath();

        for (Document doc : documents) {
            doc.getMetadata().put(YuQueResource.SOURCE, source);
        }

        return documents;
    }



}
