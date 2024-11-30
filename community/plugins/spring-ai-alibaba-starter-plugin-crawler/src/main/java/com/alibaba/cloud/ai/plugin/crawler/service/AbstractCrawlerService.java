package com.alibaba.cloud.ai.plugin.crawler.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import com.alibaba.cloud.ai.plugin.crawler.exception.CrawlerServiceException;
import com.alibaba.cloud.ai.plugin.crawler.util.UrlValidator;

import org.springframework.http.HttpMethod;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public abstract class AbstractCrawlerService implements CrawlerService {

	protected Boolean preCheck(String targetUrl) {

		return !((targetUrl != null && !targetUrl.isEmpty()) || UrlValidator.isValidUrl(targetUrl));
	}

	protected HttpURLConnection initHttpURLConnection(URL url, Map<String, String> optionHeaders) throws IOException {

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod(HttpMethod.POST.name());

		for (Map.Entry<String, String> entry : optionHeaders.entrySet()) {
			connection.setRequestProperty(entry.getKey(), entry.getValue());
		}

		return connection;
	}

	protected String getResponse(HttpURLConnection connection) throws IOException {

		StringBuilder response = new StringBuilder();
		int responseCode = connection.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {

				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
			}
		}
		else {
			throw new CrawlerServiceException("Request failed with response code: " + responseCode);
		}

		return response.toString();
	}

}
