/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.jina.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.alibaba.cloud.ai.plugin.jina.JinaProperties;
import com.alibaba.cloud.ai.plugin.jina.constant.JinaConstants;
import com.alibaba.cloud.ai.plugin.jina.exception.JinaServiceException;
import com.alibaba.cloud.ai.plugin.jina.util.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 *
 * https://jina.ai/reader/
 */

public class JinaService {

	private static final Logger logger = LoggerFactory.getLogger(JinaService.class);

	private final JinaProperties jinaProperties;

	public JinaService(JinaProperties jinaProperties) {
		this.jinaProperties = jinaProperties;
	}

	public String spiderByJina(String target) {

		if (!UrlValidator.isValidUrl(target)) {
			throw new JinaServiceException("Parameter error, please check the target URL and token");
		}

		String encodedUrl = URLEncoder.encode(target, StandardCharsets.UTF_8);
		logger.info("Jina service target URL: {}", encodedUrl);

		String url = JinaConstants.BASE_URL + target;
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(jinaProperties.getToken());
		headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);

		return run(url, headers);
	}

	public String run(String url, HttpHeaders headers) {

		StringBuilder response = new StringBuilder();

		try {
			URL targetUrl = URI.create(url).toURL();
			HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
			connection.setRequestMethod(HttpMethod.GET.name());

			for (String key : headers.keySet()) {
				connection.setRequestProperty(key, headers.getFirst(key));
			}

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
					String inputLine;
					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
				}
			} else {
				throw new JinaServiceException("Request failed with response code: " + responseCode);
			}
		} catch (Exception e) {
			throw new JinaServiceException("Request failed, please check the target URL and token: " + e.getMessage());
		}

		return response.toString();
	}

}
