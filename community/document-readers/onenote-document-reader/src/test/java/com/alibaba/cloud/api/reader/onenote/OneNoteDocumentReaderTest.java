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

    private static final String ONENOTE_ACCESS_TOKEN = System.getenv("ONENOTE_ACCESS_TOKEN");

    private static final String TEST_ACCESS_TOKEN = "EwB4A8l6BAAUBKgm8k1UswUNwklmy2v7U/S+1fEAAba2iydLSDlzJFhBCED/MW+mm4ImR6QHSsyiFeXYWJmTP69wZWHUBDR4XEWV9QMV6CPiperBohvrg5Yeek2Xd8llq9QylEcZruZG4sYuLAs4v0xupNgSas1htKO6Ii+7o3YvhX7XFVbdXE024NXCGvvUssXsUwK684DAfG2ZOoO1hMzsWJlfULR8Sz94F02kORdP2UqtCTMiSmGQgZrcTh4aro3VO0M84lDXqQf1GywIGTI2NFQ8FjLegQ2sn5zYiEARlAsik7LSCuMwWvWzZdtz0sxoSxHczIQJi7qjvXfSL5T+4kqERhuOJEp2/JNcA9g33aXhk/3TNuHC7ZQVn5EQZgAAEHpM1GKBHyQf0Kvwxy9sEwpAAo4InGesijwB4BV/NmKuhNKlk6Ef0oslNtt/QS22ZF+yZb6VAkQK2JGzIRBmHncGRmikhchhqftrqo0sE+bCkAgRXa8S73cF00dqah0Va3T+aI/ep3gDNfovc8q3KrIZBGhFM0cme5/bMToyzvKTBBnooaKYXwvSU5gH/WnhbOQoQ+5B55wjlxXSG87FdTEQUoer60InA0wE1yQECukhIlWotKB3gCUzMpf3lZby3DX7eYASBdXz1r+3vuWGbD09Kg+e8KjR0xFrl2W9T9y9abzGMFvDRnZa/w6n4aR7anTqj1OX3VYvIx7PX6sZ0PgKW06dJyBDejHKhyUhBvMlRXIVWjkCqnQeIGghzUkvnxSYIpG5I44wUtSYvLCWRmGKe7g7ylMV+BGm6eabvwXpESWfPQiUCh2M1nQNLxwv5chCdi4zHdFIE4mq1l5G5d7HPCDxxQoJlSOBGcshYkqfMm9y4BSbTz50as0LWxOo+tPw5ko4I39DtTrTqarLpxEyq94cem6r1mRUB5DTQ6ITeYXeZnzHkCsTdFEgUxMnPqGrpz+WuuuBdxeabttup2dlVLuv8istKddcoCd71xyV02r7iMWi0wz1+Xr3VBUhMln7E3dRUOoSlaVI1lMxHdaqbIIkdYEfPR/N7tJcawShp6wwZHE5XyoZ6D2NRQ5auih/bnVd/u6ACpWwPyDTSO1VumgQyzCYHJkwF6d+gEoE3y3QF32VDN0srw2q2r6jiwaT+vi/jPklXGxh9aG0OqAQLX4C";
    private OneNoteDocumentReader oneNoteDocumentReader;

    @Test
    public void test_load_page(){
        String TEST_PAGE_ID = "0-03ec86ad7070ce4193ecc4dbd76e4e41!1-4F3ACAF53591DCC0!2997";

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
