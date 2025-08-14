/*
 * Copyright 2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.runtime.domain.workflow.inner.TimeoutConfig;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.domain.HttpDeleteWithBody;
import com.alibaba.cloud.ai.studio.core.base.domain.RpcResult;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.*;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import javax.net.ssl.SSLContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * HTTP client manager for handling HTTP requests. Provides methods for making HTTP
 * requests with different methods (GET, POST, PUT, DELETE, etc.) and supports various
 * content types (JSON, form data, etc.).
 *
 * @since 1.0.0.3
 */
@Component
public class HttpClientManager implements InitializingBean {

	/** Main HTTP client instance */
	private CloseableHttpClient httpClient = null;

	/** HTTP client instance for workflow operations */
	private CloseableHttpClient workflowHttpClient = null;

	/** Maximum total connections */
	private static final int maxTotal = 200;

	/** Maximum connections per route */
	private static final int defaultMaxPerRoute = 200;

	/** Connection request timeout in ms */
	private static final int connectionRequestTimeout = 200;

	/** Connection timeout in ms */
	private static final int connectTimeout = 2000;

	/** Socket timeout in ms */
	private static final int socketTimeout = 15000;

	/** Socket timeout for orchestration in ms */
	private static final int socketTimeoutForOrchestra = 5000;

	/** Socket timeout for orchestration in ms */
	private static final int socketTimeoutForOrchestraWhiteList = 60000;

	@Override
	public void afterPropertiesSet() throws Exception {
		// Initialize SSL connection factory
		LayeredConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(SSLContext.getDefault());
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
			.register("https", sslsf)
			.register("http", new PlainConnectionSocketFactory())
			.build();

		// Create HTTP client connection pool
		PoolingHttpClientConnectionManager clientConnectionManager = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);
		clientConnectionManager.setMaxTotal(maxTotal);
		clientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);

		// Configure redirect strategy
		LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();

		// Initialize main HTTP client
		HttpClientBuilder httpClientBuilder = HttpClients.custom();
		httpClientBuilder.setConnectionManager(clientConnectionManager);
		httpClientBuilder.setRedirectStrategy(redirectStrategy);
		httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
		RequestConfig requestConfig = RequestConfig.custom()
			.setSocketTimeout(socketTimeout)
			.setConnectTimeout(connectTimeout)
			.setConnectionRequestTimeout(connectionRequestTimeout)
			.build();
		httpClientBuilder.setDefaultRequestConfig(requestConfig);
		httpClient = httpClientBuilder.build();

		// Initialize workflow HTTP client
		PoolingHttpClientConnectionManager orchestraClientConnectionManager = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);
		orchestraClientConnectionManager.setMaxTotal(maxTotal);
		orchestraClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);

		HttpClientBuilder ochestraHttpClientBuilder = HttpClients.custom();
		ochestraHttpClientBuilder.disableAutomaticRetries();
		ochestraHttpClientBuilder.setConnectionManager(orchestraClientConnectionManager);
		ochestraHttpClientBuilder.setRedirectStrategy(redirectStrategy);

		// Configure no retry strategy for workflow client
		ServiceUnavailableRetryStrategy noRetryStrategy = new ServiceUnavailableRetryStrategy() {
			@Override
			public boolean retryRequest(HttpResponse response, int executionCount, HttpContext context) {
				return false;
			}

			@Override
			public long getRetryInterval() {
				return 0; // Not used as retries are disabled
			}
		};

		ochestraHttpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));
		RequestConfig ochestraRequestConfig = RequestConfig.custom()
			.setSocketTimeout(socketTimeoutForOrchestra)
			.setConnectTimeout(connectTimeout)
			.setConnectionRequestTimeout(connectionRequestTimeout)
			.build();
		ochestraHttpClientBuilder.setDefaultRequestConfig(ochestraRequestConfig);
		workflowHttpClient = ochestraHttpClientBuilder.build();
	}

	/**
	 * Supported HTTP methods
	 */
	public enum Method {

		POST("post"), GET("get"), PUT("put"), DELETE("delete"), PATCH("patch"), HEAD("head"), OPTIONS("options"),
		TRACE("trace");

		@Getter
		private final String value;

		Method(String value) {
			this.value = value;
		}

	}

	public HttpClientManager() {
	}

	public void close() {
		if (httpClient != null) {
			try {
				httpClient.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				LogUtils.error("http client close has err!", e);
			}
		}
		if (workflowHttpClient != null) {
			try {
				workflowHttpClient.close();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				LogUtils.error("orchestraHttpClient client close has err!", e);
			}
		}
	}

	/******************** get方法 begin ********************/
	public RpcResult doGet(String url, Map<String, Object> parameters) {
		return doGet(url, Maps.newHashMap(), parameters);
	}

	public RpcResult doGet(String url, Map<String, Object> headers, Map<String, Object> parameters) {
		return doGetWithRequestId(UUID.randomUUID().toString(), url, headers, parameters);
	}

	public RpcResult doGetWithRequestId(String requestId, String url, Map<String, Object> headers,
			Map<String, Object> parameters) {
		try {
			HttpUriRequest request = buildGetRequest(url, headers, parameters);
			CloseableHttpResponse response = httpClient.execute(request);
			RpcResult RpcResult = processResponse(response);
			LogUtils.info("HttpClientManager", "doGet", requestId, url, headers, parameters, RpcResult);
			return RpcResult;
		}
		catch (Exception e) {
			LogUtils.error("HttpClient doGet error requestId={}, url={}, headers={}, parameters={}", requestId, url,
					JsonUtils.toJson(headers), JsonUtils.toJson(parameters), e);
			return RpcResult.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	public RpcResult doPostJson(String url, Map<String, Object> headers, Map<String, Object> parameters) {
		return doPostJsonWithRequestId(UUID.randomUUID().toString(), url, headers, parameters);
	}

	public RpcResult doPostJson(String url, Map<String, Object> parameters) {
		return doPostJson(url, Maps.newHashMap(), parameters);
	}

	public RpcResult doPostJsonWithRequestId(String requestId, String url, Map<String, Object> headers,
			Map<String, Object> parameters) {
		try {
			HttpUriRequest request = buildPostJsonRequest(url, headers, parameters);
			CloseableHttpResponse response = httpClient.execute(request);
			RpcResult RpcResult = processResponse(response);
			LogUtils.info("HttpClientManager", "doPostJson", url, headers, parameters, RpcResult);
			return RpcResult;
		}
		catch (Exception e) {
			LogUtils.error("HttpClient doPostJson error requestId={}, url={}, headers={}, parameters={}", requestId,
					url, JsonUtils.toJson(headers), JsonUtils.toJson(parameters), e);
			return RpcResult.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	public RpcResult doPutJson(String url, Map<String, Object> headers, Map<String, Object> parameters) {
		return doPutJsonWithRequestId(UUID.randomUUID().toString(), url, headers, parameters);
	}

	public RpcResult doPutJsonWithRequestId(String requestId, String url, Map<String, Object> headers,
			Map<String, Object> parameters) {

		try {
			HttpUriRequest request = buildPutJsonRequest(url, headers, parameters);
			CloseableHttpResponse response = httpClient.execute(request);
			RpcResult RpcResult = processResponse(response);
			LogUtils.info("HttpClientManager", "doPutJson", url, headers, parameters, RpcResult);
			return RpcResult;
		}
		catch (Exception e) {
			LogUtils.error("HttpClient doPutJson error requestId={},url={},headers={},parameters={}", requestId, url,
					JsonUtils.toJson(headers), JsonUtils.toJson(parameters), e);
			return RpcResult.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	public RpcResult doDeleteJson(String url, Map<String, Object> headers, Map<String, Object> parameters) {
		return doDeleteJsonWithRequestId(UUID.randomUUID().toString(), url, headers, parameters);
	}

	public RpcResult doDeleteJsonWithRequestId(String requestId, String url, Map<String, Object> headers,
			Map<String, Object> parameters) {

		try {
			HttpUriRequest request = buildDeleteJsonRequest(url, headers, parameters);
			CloseableHttpResponse response = httpClient.execute(request);
			RpcResult RpcResult = processResponse(response);
			LogUtils.info("HttpClientManager", "doDeleteJson", url, headers, parameters, RpcResult);
			return RpcResult;
		}
		catch (Exception e) {
			LogUtils.error("HttpClient doDeleteJson error.url={},headers={},parameters={}", url,
					JsonUtils.toJson(headers), JsonUtils.toJson(parameters), e);
			return RpcResult.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	public RpcResult doPostForm(String url, Map<String, Object> parameters) {
		return doPostForm(url, Maps.newHashMap(), parameters);
	}

	public RpcResult doPostForm(String url, Map<String, Object> headers, Map<String, Object> parameters) {
		return doPostFormWithRequestId(UUID.randomUUID().toString(), url, headers, parameters);
	}

	public RpcResult doPostFormWithRequestId(String requestId, String url, Map<String, Object> headers,
			Map<String, Object> parameters) {

		try {
			HttpUriRequest request = buildPostFormRequest(url, headers, parameters);
			CloseableHttpResponse response = httpClient.execute(request);
			RpcResult RpcResult = processResponse(response);
			LogUtils.info("HttpClientManager", "doPostForm", url, headers, parameters, RpcResult);
			return RpcResult;
		}
		catch (Exception e) {
			LogUtils.error("HttpClientManager", "doPostForm", "request error ,url={},headers={},parameters={}", url,
					JsonUtils.toJson(headers), JsonUtils.toJson(parameters), e);
			return RpcResult.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	public Flux<String> doPostFlux(String requestId, String url, Map<String, Object> headers,
			Map<String, Object> parameters) {
		return Flux.create(sink -> {
			try {
				HttpUriRequest request = buildPostJsonRequest(url, headers, parameters);
				CloseableHttpResponse response = httpClient.execute(request);
				BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
				String line;
				while ((line = reader.readLine()) != null) {
					// parse SSE data
					if (line.startsWith("data: ")) {
						String data = line.substring(6);
						sink.next(data);
					}
					LogUtils.monitor("HttpClientManager", "doPostFlux", 0L, requestId, request, line);
				}
			}
			catch (Exception e) {
				sink.error(e);
			}
			finally {
				sink.complete();
			}
		});
	}

	/**
	 * Builds a GET request with the specified URL, headers and parameters
	 */
	public HttpUriRequest buildGetRequest(String url, Map<String, Object> header, Map<String, Object> parameters) {
		String encodedContent = encodingParams(parameters, StandardCharsets.UTF_8.toString());
		url += (null == encodedContent) ? "" : ("?" + encodedContent);
		HttpGet request = new HttpGet(url);
		addHeader(request, header);
		return request;
	}

	/**
	 * Builds a POST request with JSON body
	 */
	public HttpUriRequest buildPostJsonRequest(String url, Map<String, Object> header, Map<String, Object> parameters) {
		HttpPost request = new HttpPost(url);
		addHeader(request, header);
		StringEntity entity = new StringEntity(JsonUtils.toJson(parameters), StandardCharsets.UTF_8);
		request.setEntity(entity);
		return request;
	}

	/**
	 * Builds encoded content from parameters list
	 */
	private String buildEncodedContent(List<Map<String, Object>> parameters) {
		if (null == parameters || parameters.isEmpty()) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		for (Map<String, Object> param : parameters) {
			String key = MapUtils.getString(param, "key");
			Object value = param.get("value");
			if (Objects.isNull(key) || Objects.isNull(value)) {
				continue;
			}
			String val = "" + value;
			try {
				sb.append(key).append("=").append(URLEncoder.encode(val, StandardCharsets.UTF_8.toString()));
			}
			catch (UnsupportedEncodingException e) {
			}
			sb.append("&");
		}
		String encodedContent = sb.toString();
		return encodedContent.substring(0, encodedContent.length() - 1);
	}

	/**
	 * Adds headers to the API node request
	 */
	private void addHeaderForAPINode(HttpMessage request, Map<String, Object> header) {
		try {
			if (header != null) {
				for (String key : header.keySet()) {
					request.setHeader(key, "" + header.get(key));
				}
			}
		}
		catch (Exception e) {
			LogUtils.error("addHeader error.header={}", JsonUtils.toJson(header), e);
		}
	}

	/**
	 * Builds a PUT request with JSON body
	 */
	public HttpUriRequest buildPutJsonRequest(String url, Map<String, Object> header, Map<String, Object> parameters) {
		HttpPut request = new HttpPut(url);
		addHeader(request, header);
		StringEntity entity = new StringEntity(JsonUtils.toJson(parameters), StandardCharsets.UTF_8);
		request.setEntity(entity);
		return request;
	}

	/**
	 * Builds a DELETE request with JSON body
	 */
	public HttpUriRequest buildDeleteJsonRequest(String url, Map<String, Object> header,
			Map<String, Object> parameters) {
		HttpDeleteWithBody request = new HttpDeleteWithBody(url);
		addHeader(request, header);
		StringEntity entity = new StringEntity(JsonUtils.toJson(parameters), StandardCharsets.UTF_8);
		request.setEntity(entity);
		return request;
	}

	/**
	 * Builds a POST request with form data
	 */
	public HttpUriRequest buildPostFormRequest(String url, Map<String, Object> header, Map<String, Object> parameters) {
		HttpPost request = new HttpPost(url);

		if (header != null) {
			for (String key : header.keySet()) {
				request.addHeader(key, "" + header.get(key));
			}
		}

		List<NameValuePair> formParams = new ArrayList<>();
		for (String key : parameters.keySet()) {
			formParams.add(new BasicNameValuePair(key, parameters.get(key).toString()));
		}
		HttpEntity entity = new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8);
		request.setEntity(entity);
		return request;
	}

	/**
	 * Adds headers to the HTTP request
	 */
	private void addHeader(HttpMessage request, Map<String, Object> header) {
		try {
			// Add default headers if not present
			if (ArrayUtils.isEmpty(request.getHeaders(HttpHeaders.CONTENT_TYPE))) {
				request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
			}

			if (ArrayUtils.isEmpty(request.getHeaders(HttpHeaders.ACCEPT))) {
				request.addHeader(HttpHeaders.ACCEPT, "application/json;charset=UTF-8");
			}

			if (header != null) {
				for (String key : header.keySet()) {
					request.addHeader(key, "" + header.get(key));
				}
			}
		}
		catch (Exception e) {
			LogUtils.error("addHeader error.header={}", JsonUtils.toJson(header), e);
		}
	}

	/**
	 * Processes the HTTP response and converts it to RpcResult
	 */
	private RpcResult processResponse(CloseableHttpResponse response) throws Exception {
		int statusCode = response.getStatusLine().getStatusCode();
		String result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

		if (statusCode >= HttpStatus.SC_OK && statusCode <= HttpStatus.SC_MULTIPLE_CHOICES) {
			return RpcResult.success(result);
		}

		RpcResult RpcResult = new RpcResult();
		RpcResult.setResponse(result);
		RpcResult.setSuccess(false);
		RpcResult.setCode(statusCode);
		RpcResult.setMessage(result);
		RpcResult.setOriginResponse(result);

		return RpcResult;
	}

	/**
	 * Encodes parameters for URL
	 */
	public static String encodingParams(Map<String, Object> params, String encode) {
		StringBuilder sb = new StringBuilder();
		if (null == params || params.isEmpty()) {
			return null;
		}
		for (Map.Entry<String, Object> entry : params.entrySet()) {
			if (Objects.isNull(entry.getValue())) {
				continue;
			}

			String val = "" + entry.getValue();
			if (StringUtils.isEmpty(val)) {
				continue;
			}

			try {
				sb.append(entry.getKey()).append("=").append(URLEncoder.encode(val, encode));
			}
			catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			sb.append("&");
		}

		return sb.toString();
	}

	/**
	 * Executes API node request with specified method and parameters
	 */
	public RpcResult doApiNodeWithRequestId(Method method, String requestId, String url, Map<String, Object> headers,
			List<Map<String, Object>> parameters, Map<String, Object> body, Map<String, Object> form,
			TimeoutConfig timeoutConfig) {
		try {
			HttpRequestBase request = buildApiNodeRequest(method, url, headers, parameters, body, form, timeoutConfig);
			CloseableHttpResponse response = workflowHttpClient.execute(request);
			RpcResult rpcResult = processResponse(response);
			LogUtils.info("HttpClientManager", method.getValue(), url, headers, parameters, body, form, rpcResult);
			return rpcResult;
		}
		catch (Exception e) {
			LogUtils.error(
					"HttpClient " + method.getValue()
							+ " error requestId={},url={},headers={},parameters={},body={},form={}",
					requestId, url, JsonUtils.toJson(headers), JsonUtils.toJson(parameters), JsonUtils.toJson(body),
					JsonUtils.toJson(form), e);
			return RpcResult.error(HttpStatus.SC_INTERNAL_SERVER_ERROR, e.getMessage());
		}
	}

	/**
	 * Builds API node request with specified method and parameters
	 */
	public HttpRequestBase buildApiNodeRequest(Method method, String url, Map<String, Object> header,
			List<Map<String, Object>> parameters, Map<String, Object> body, Map<String, Object> form,
			TimeoutConfig timeoutConfig) {
		String encodedUrl = buildUrl(url, buildEncodedContent(parameters));
		HttpRequestBase request;

		switch (method) {
			case GET:
				request = new HttpGet(encodedUrl);
				break;
			case POST:
				HttpPost postRequest = new HttpPost(encodedUrl);
				setEntityByType(postRequest, MapUtils.getString(body, "type"), form);
				request = postRequest;
				break;
			case PUT:
				HttpPut putRequest = new HttpPut(encodedUrl);
				setEntityByType(putRequest, MapUtils.getString(body, "type"), form);
				request = putRequest;
				break;
			case DELETE:
				if (body != null && form != null && !form.isEmpty()) {
					HttpDeleteWithBody deleteRequest = new HttpDeleteWithBody(encodedUrl);
					setEntityByType(deleteRequest, MapUtils.getString(body, "type"), form);
					request = deleteRequest;
				}
				else {
					request = new HttpDelete(encodedUrl);
				}
				break;
			case PATCH:
				HttpPatch patchRequest = new HttpPatch(encodedUrl);
				setEntityByType(patchRequest, MapUtils.getString(body, "type"), form);
				request = patchRequest;
				break;
			default:
				throw new IllegalArgumentException("Unsupported HTTP method: " + method);
		}
		addHeaderForAPINode(request, header);
		// Set timeout configuration
		if (timeoutConfig != null && timeoutConfig.getRead() != null) {
			RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(timeoutConfig.getRead() * 1000)
				.setConnectionRequestTimeout(timeoutConfig.getRead() * 1000)
				.setSocketTimeout(timeoutConfig.getRead() * 1000)
				.build();
			request.setConfig(requestConfig);
		}
		return request;
	}

	/**
	 * Builds complete URL with encoded content
	 */
	private String buildUrl(String url, String encodedContent) {
		if (encodedContent == null || encodedContent.isEmpty()) {
			return url;
		}

		if (url.contains("?")) {
			return url + "&" + encodedContent;
		}
		else {
			return url + "?" + encodedContent;
		}
	}

	/**
	 * Sets request entity based on content type
	 */
	private void setEntityByType(HttpEntityEnclosingRequestBase request, String type, Map<String, Object> form) {
		if (form == null || form.isEmpty()) {
			StringEntity entity = new StringEntity("", StandardCharsets.UTF_8);
			request.setEntity(entity);
			return;
		}

		if ("raw".equalsIgnoreCase(type)) {
			String data = form.get("raw").toString();
			StringEntity entity = new StringEntity(data, StandardCharsets.UTF_8);
			entity.setContentType("text/plain");
			request.setEntity(entity);
		}
		else if ("form-data".equalsIgnoreCase(type)) {
			List<NameValuePair> formParams = new ArrayList<>();
			for (String key : form.keySet()) {
				formParams.add(new BasicNameValuePair(key, form.get(key).toString()));
			}
			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, StandardCharsets.UTF_8);
			entity.setContentType("application/x-www-form-urlencoded");
			request.setEntity(entity);
		}
		else if ("json".equalsIgnoreCase(type)) {
			String data = form.get("json").toString();
			StringEntity entity = new StringEntity(data, StandardCharsets.UTF_8);
			entity.setContentType("application/json");
			request.setEntity(entity);
		}
		else {
			StringEntity entity = new StringEntity("", StandardCharsets.UTF_8);
			request.setEntity(entity);
		}
	}

}
