package com.alibaba.cloud.ai.functioncalling.yuque;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;

import static com.alibaba.cloud.ai.functioncalling.yuque.YuqueProperties.BASE_URL;

/**
 * @author 北极星
 */
public class YuqueQueryBookService implements Function<YuqueQueryBookService.queryBookRequest, YuqueQueryBookService.queryBookResponse> {

    private final WebClient webClient;

    public YuqueQueryBookService (YuqueProperties yuqueProperties) {
        this.webClient = WebClient.builder().baseUrl(BASE_URL).defaultHeader("X-Auth-Token", yuqueProperties.getAuthToken()).build();

    }

    @Override
    public queryBookResponse apply (queryBookRequest queryBookRequest) {
        Mono<YuqueQueryBookService.queryBookResponse> queryBookResponseMono = webClient.method(HttpMethod.GET)
                .uri("/{book_id}/docs", queryBookRequest.bookId).retrieve().bodyToMono(queryBookResponse.class);
        return queryBookResponseMono.block();
    }

    protected record queryBookRequest(@JsonProperty("slug") String slug, @JsonProperty("title") String title,
                                      String bookId, String id) {}

    protected record queryBookResponse(@JsonProperty("meta") int meta, @JsonProperty("data") List<data> data) {}

    protected record data(@JsonProperty("id") String id, @JsonProperty("docId") String docId,
                          @JsonProperty("slug") String slug, @JsonProperty("title") String title,
                          @JsonProperty("userId") String userId,
                          @JsonProperty("user") YuqueQueryDocService.UserSerializer user,
                          @JsonProperty("draft") String draft, @JsonProperty("body") String body,
                          @JsonProperty("bodyAsl") String bodyAsl,
                          @JsonProperty("bodyHtml") String bodyHtml,
                          @JsonProperty("createdAt") String createdAt,
                          @JsonProperty("updatedAt") String updatedAt
    ) {}
}
