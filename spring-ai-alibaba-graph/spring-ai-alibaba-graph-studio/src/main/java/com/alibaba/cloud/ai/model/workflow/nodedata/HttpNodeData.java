package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.graph.node.HttpNode.AuthConfig;
import com.alibaba.cloud.ai.graph.node.HttpNode.HttpRequestNodeBody;
import com.alibaba.cloud.ai.graph.node.HttpNode.RetryConfig;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import org.springframework.http.HttpMethod;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Http 节点的数据模型，包含 Builder 的全部可配置项。
 */
public class HttpNodeData extends NodeData {

    /** HTTP 方法，默认 GET */
    private HttpMethod method = HttpMethod.GET;

    /** 请求 URL */
    private String url;

    /** 请求头 */
    private Map<String, String> headers = Collections.emptyMap();

    /** 查询参数 */
    private Map<String, String> queryParams = Collections.emptyMap();

    /** 请求体配置 */
    private HttpRequestNodeBody body = new HttpRequestNodeBody();

    /** 鉴权配置 */
    private AuthConfig authConfig;

    /** 重试配置 */
    private RetryConfig retryConfig = new RetryConfig(3, 1000, true);

    /** 响应写入的状态变量 Key */
    private String outputKey;

    public HttpNodeData(List<VariableSelector> inputs,
                        List<com.alibaba.cloud.ai.model.Variable> outputs,
                        HttpMethod method,
                        String url,
                        Map<String, String> headers,
                        Map<String, String> queryParams,
                        HttpRequestNodeBody body,
                        AuthConfig authConfig,
                        RetryConfig retryConfig,
                        String outputKey) {
        super(inputs, outputs);
        this.method = method;
        this.url = url;
        this.headers = headers != null ? headers : Collections.emptyMap();
        this.queryParams = queryParams != null ? queryParams : Collections.emptyMap();
        this.body = body != null ? body : new HttpRequestNodeBody();
        this.authConfig = authConfig;
        this.retryConfig = retryConfig != null
            ? retryConfig
            : new RetryConfig(3, 1000, true);
        this.outputKey = outputKey;
    }

    /** 向下兼容的简化构造，未指定时使用默认值。 */
    public HttpNodeData(List<VariableSelector> inputs,
                        List<com.alibaba.cloud.ai.model.Variable> outputs) {
        this(inputs, outputs, HttpMethod.GET, null,
             Collections.emptyMap(), Collections.emptyMap(),
             new HttpRequestNodeBody(), null,
             new RetryConfig(3, 1000, true), null);
    }

    // === getters & setters ===

    public HttpMethod getMethod() {
        return method;
    }

    public void setMethod(HttpMethod method) {
        this.method = method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(Map<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public HttpRequestNodeBody getBody() {
        return body;
    }

    public void setBody(HttpRequestNodeBody body) {
        this.body = body;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(AuthConfig authConfig) {
        this.authConfig = authConfig;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public void setRetryConfig(RetryConfig retryConfig) {
        this.retryConfig = retryConfig;
    }

    public String getOutputKey() {
        return outputKey;
    }

    public void setOutputKey(String outputKey) {
        this.outputKey = outputKey;
    }
}
