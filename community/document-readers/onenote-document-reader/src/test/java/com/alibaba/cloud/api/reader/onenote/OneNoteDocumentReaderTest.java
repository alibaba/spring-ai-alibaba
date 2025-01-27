package com.alibaba.cloud.api.reader.onenote;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author sparkle6979l
 * @version 1.0
 * @data 2025/1/27 19:59
 */
public class OneNoteDocumentReaderTest {

    private static final String TEST_ACCESS_TOKEN = System.getenv("ONENOTE_ACCESS_TOKEN");

    private static final String TEST_NOTEBOOK_ID = "${notebookId}";

    private static final String TEST_SECTION_ID = "${sectionId}";

    private static final String TEST_PAGE_ID = "${pageId}";

    private OneNoteDocumentReader oneNoteDocumentReader;

    @Test
    public void test_load_page(){

        // Create page reader
        OneNoteResource oneNoteResource = OneNoteResource.builder()
                .resourceId(TEST_PAGE_ID)
                .resourceType(OneNoteResource.ResourceType.PAGE)
                .build();
        OneNoteDocumentReader oneNoteDocumentReader = new OneNoteDocumentReader(TEST_ACCESS_TOKEN, oneNoteResource);

        List<Document> documents = oneNoteDocumentReader.get();
        // then
        assertThat(documents).isNotEmpty();
        Document document = documents.get(0);

        // Verify metadata
        assertThat(document.getMetadata()).containsKey(OneNoteResource.SOURCE);
        assertThat(document.getMetadata().get("resourceType")).isEqualTo(OneNoteResource.ResourceType.PAGE.name());
        assertThat(document.getMetadata().get("resourceId")).isEqualTo(TEST_PAGE_ID);

        // Verify content
        String content = document.getContent();
        assertThat(content).isNotEmpty();
    }


    @Test
    public void test_load_section(){
        String testSectionId = "0-4F3ACAF53591DCC0!2862";

        // Create page reader
        OneNoteResource oneNoteResource = OneNoteResource.builder()
                .resourceId(testSectionId)
                .resourceType(OneNoteResource.ResourceType.SECTION)
                .build();
        OneNoteDocumentReader oneNoteDocumentReader = new OneNoteDocumentReader(TEST_ACCESS_TOKEN, oneNoteResource);

        List<Document> documents = oneNoteDocumentReader.get();
        // then
        assertThat(documents).isNotEmpty();
        Document document = documents.get(0);

        // Verify metadata
        assertThat(document.getMetadata()).containsKey(OneNoteResource.SOURCE);
        assertThat(document.getMetadata().get("resourceType")).isEqualTo(OneNoteResource.ResourceType.SECTION.name());
        assertThat(document.getMetadata().get("resourceId")).isEqualTo(testSectionId);

        // Verify content
        String content = document.getContent();
        assertThat(content).isNotEmpty();
    }


    @Test
    public void test_load_notebook(){
        String testNoteBookId = "0-4F3ACAF53591DCC0!2860";

        // Create page reader
        OneNoteResource oneNoteResource = OneNoteResource.builder()
                .resourceId(testNoteBookId)
                .resourceType(OneNoteResource.ResourceType.NOTEBOOK)
                .build();
        OneNoteDocumentReader oneNoteDocumentReader = new OneNoteDocumentReader(TEST_ACCESS_TOKEN, oneNoteResource);

        List<Document> documents = oneNoteDocumentReader.get();
        // then
        assertThat(documents).isNotEmpty();
        Document document = documents.get(0);

        // Verify metadata
        assertThat(document.getMetadata()).containsKey(OneNoteResource.SOURCE);
        assertThat(document.getMetadata().get("resourceType")).isEqualTo(OneNoteResource.ResourceType.NOTEBOOK.name());
        assertThat(document.getMetadata().get("resourceId")).isEqualTo(testNoteBookId);

        // Verify content
        String content = document.getContent();
        assertThat(content).isNotEmpty();
    }


}
