package com.alibaba.cloud.ai.functioncalling.yuque;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.function.Function;

import static com.alibaba.cloud.ai.functioncalling.yuque.YuqueProperties.BASE_URL;

/**
 * @author 北极星
 */
public class YuqueDeleteDocService implements Function<YuqueDeleteDocService.deleteDocRequest, YuqueDeleteDocService.deleteDocResponse> {

    private final WebClient webClient;

    public YuqueDeleteDocService (YuqueProperties yuqueProperties) {
        this.webClient = WebClient.builder().baseUrl(BASE_URL).defaultHeader("X-Auth-Token", yuqueProperties.getAuthToken()).build();

    }

    @Override
    public YuqueDeleteDocService.deleteDocResponse apply (YuqueDeleteDocService.deleteDocRequest deleteDocRequest) {
        Mono<YuqueDeleteDocService.deleteDocResponse> deleteDocResponseMono = webClient.method(HttpMethod.DELETE).uri("/repos/{book_id}/docs/{id}", deleteDocRequest.bookId, deleteDocRequest.id).retrieve().bodyToMono(YuqueDeleteDocService.deleteDocResponse.class);

        return deleteDocResponseMono.block();
    }

    protected record deleteDocRequest(@JsonProperty("bookId") String bookId, @JsonProperty("id") String id) {}


    protected record deleteDocResponse(@JsonProperty("id") String id, @JsonProperty("slug") String slug,
                                       @JsonProperty("type") String type,
                                       @JsonProperty("description") String description,
                                       @JsonProperty("cover") String cover, @JsonProperty("user_id") String user_id,
                                       @JsonProperty("book_id") String book_id,
                                       @JsonProperty("last_editor_id") String last_editor_id,
                                       @JsonProperty("format") String format,
                                       @JsonProperty("body_draft") String body_draft,
                                       @JsonProperty("body_sheet") String body_sheet,
                                       @JsonProperty("body_table") String body_table,
                                       @JsonProperty("body_html") String body_html,
                                       @JsonProperty("public") int isPublic, @JsonProperty("status") String status,
                                       @JsonProperty("likes_count") int likes_count,
                                       @JsonProperty("read_count") int read_count,
                                       @JsonProperty("comments_count") int comments_count,
                                       @JsonProperty("word_count") int word_count,
                                       @JsonProperty("created_at") String createdAt,
                                       @JsonProperty("updated_at") String updatedAt) {}

}
