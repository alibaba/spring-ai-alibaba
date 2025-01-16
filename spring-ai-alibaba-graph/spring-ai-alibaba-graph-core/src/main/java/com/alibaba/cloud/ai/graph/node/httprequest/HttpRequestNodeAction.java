package com.alibaba.cloud.ai.graph.node.httprequest;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.node.AbstractNode;
import com.alibaba.cloud.ai.graph.state.NodeState;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.util.Map;
import java.util.Objects;

/**
 * @author 北极星
 */
@Slf4j
@Data
@NoArgsConstructor
public class HttpRequestNodeAction extends AbstractNode implements NodeAction {

    private final WebClient webClient = WebClient.create();

    private final RetryTemplate retryTemplate = RetryUtils.DEFAULT_RETRY_TEMPLATE;

    private String url;

    private String method;

    private Map<String, Object> headers;

    private Map<String, Object> params;

    private JSONObject body;

    private TimeOut timeout;

    private String title;

    public HttpRequestNodeAction (String url, String method, Map<String, Object> headers, Map<String, Object> params, JSONObject body, TimeOut timeout, String title) {
        this.url = url;
        this.method = method;
        this.headers = headers;
        this.params = params;
        this.body = body;
        this.timeout = timeout;
        this.title = title;
    }

    @Override
    public Map<String, Object> apply (NodeState state) {
        return this.retryTemplate.execute(context -> executeRequest(), (context) -> {
            log.error("over the maximum retrying count {}, Http Request failure .", context.getRetryCount());
            return Map.of("statusCode", 1000, "body", new JSONObject(), "success", "fail");
        });
    }

    private String buildUri () {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

        // 添加查询参数
        if (params != null && !params.isEmpty()) {
            params.forEach((key, value) -> builder.queryParam(key, value));
        }

        return builder.build().toUriString();
    }

    private WebClient prepareRequest () {

        WebClient instance = WebClient.create(buildUri());

        // 添加超时设置
        if (timeout != null) {
            HttpClient httpClient = HttpClient.create().tcpConfiguration(client -> client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeout.connectTimeout).doOnConnected(conn -> conn.addHandlerFirst(new ReadTimeoutHandler(timeout.getReadTimeout())).addHandlerLast(new WriteTimeoutHandler(timeout.getWriteTimeout()))));
            instance.mutate().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
        }

        // 添加请求头
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((key, value) -> instance.head().header(key, value.toString()));
        }
        return instance;
    }

    private Map<String, Object> executeRequest () {

        WebClient requestSpec = prepareRequest();
        WebClient.ResponseSpec responseSpec = executeWithBody(requestSpec);
        return handleResponse(responseSpec);
    }

    private WebClient.ResponseSpec executeWithBody (WebClient instance) {

        if (body != null && !body.isEmpty()) {
            WebClient.RequestBodyUriSpec bodySpec = instance.method(HttpMethod.valueOf(method.toUpperCase()));
            return bodySpec.bodyValue(body.toString()).retrieve();
        }
        return instance.head().retrieve();
    }

    private Map<String, Object> handleResponse (WebClient.ResponseSpec responseSpec) {

        String responseBody = String.valueOf(responseSpec.onStatus(status -> status.is4xxClientError() || status.is5xxServerError(), clientResponse -> {
            return (Mono<? extends Throwable>) Map.of("statusCode", 1000, "body", new JSONObject(), "success", "failure");
        }).bodyToFlux(Map.class));

        return Map.of("statusCode", Objects.requireNonNull(responseSpec.toBodilessEntity().block()).getStatusCode().value(), "body", responseBody != null ? new JSONObject(responseBody) : new JSONObject(), "success", true);
    }

    @Data
    @NoArgsConstructor
    public static class TimeOut {
        private int connectTimeout;
        private int readTimeout;
        private int writeTimeout;
    }

    public static class Builder {

        private String url;

        private String method;

        private Map<String, Object> headers;

        private Map<String, Object> params;

        private JSONObject body;

        private RuntimeException customException;

        private TimeOut timeout;

        private String title;

        public Builder () {

        }

        public Builder setUrl (String url) {
            this.url = url;
            return this;
        }

        public Builder setMethod (String method) {
            this.method = method;
            return this;
        }

        public Builder setHeaders (Map<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public Builder setParams (Map<String, Object> params) {
            this.params = params;
            return this;
        }

        public Builder setBody (JSONObject body) {
            this.body = body;
            return this;
        }

        public Builder setCustomException (RuntimeException customException) {
            this.customException = customException;
            return this;
        }

        public Builder setTimeout (TimeOut timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder setTitle (String title) {
            this.title = title;
            return this;
        }

        public HttpRequestNodeAction build () {
            return new HttpRequestNodeAction();
        }
    }
}
