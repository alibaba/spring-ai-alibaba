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
public class YuqueQueryDocService implements Function<YuqueQueryDocService.queryDocRequest, YuqueQueryDocService.queryDocResponse> {

    /**
     * Applies this function to the given argument.
     *
     * @param queryDocRequest the function argument
     * @return the function result
     */
    private final WebClient webClient;

    public YuqueQueryDocService (YuqueProperties yuqueProperties) {

        this.webClient = WebClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-Auth-Token", yuqueProperties.getAuthToken()).build();

    }

    @Override
    public queryDocResponse apply (queryDocRequest queryDocRequest) {
        Mono<queryDocResponse> queryDocResponseMono = webClient.method(HttpMethod.POST)
                .uri("/repos/{book_id}/docs/{id}", queryDocRequest.bookId, queryDocRequest.id)
                .retrieve()
                .bodyToMono(queryDocResponse.class);

        return queryDocResponseMono.block();
    }

    protected record queryDocRequest(@JsonProperty("slug") String slug, @JsonProperty("title") String title,
                                     String bookId, String id) {}


    protected record queryDocResponse(@JsonProperty("id") String id,
                                      @JsonProperty("docId") String docId,
                                      @JsonProperty("slug") String slug,
                                      @JsonProperty("title") String title,
                                      @JsonProperty("userId") String userId,
                                      @JsonProperty("user") UserSerializer user,
                                      @JsonProperty("draft") String draft,
                                      @JsonProperty("body") String body,
                                      @JsonProperty("bodyAsl") String bodyAsl,
                                      @JsonProperty("bodyHtml") String bodyHtml,
                                      @JsonProperty("createdAt") String createdAt,
                                      @JsonProperty("updatedAt") String updatedAt
    ) {}

    protected record UserSerializer(@JsonProperty String id, @JsonProperty String type, @JsonProperty String login,
                                    @JsonProperty String name) {}
}