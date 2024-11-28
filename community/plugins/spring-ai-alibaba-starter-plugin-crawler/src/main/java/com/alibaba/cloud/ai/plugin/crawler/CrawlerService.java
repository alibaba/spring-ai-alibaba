package com.alibaba.cloud.ai.plugin.crawler;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author chengle
 *
 */
public class CrawlerService implements Function<CrawlerService.Request, CrawlerService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(CrawlerService.class);

	@Override
	public Response apply(Request request) {
		try {
			String url = request.url;

			Document document = Jsoup.connect(url)
				.userAgent(
						"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
				.timeout(10000)
				.get();

			Set<String> uniqueChineseTexts = new HashSet<>();

			Elements paragraphs = document.select("p, div");

			for (Element paragraph : paragraphs) {
				String text = paragraph.text();
				String chineseSegment = extractChinese(text);
				if (!chineseSegment.isEmpty()) {
					uniqueChineseTexts.add(chineseSegment);
				}
			}

			String chineseText = setToStringWithNewLines(uniqueChineseTexts);

			logger.error("chineseText={}", chineseText.toString());
			return new Response(chineseText.toString());
		}
		catch (Exception e) {
			logger.error("CrawlerService||apply||error={}", e);
			return new Response(null);
		}
	}

	public static String extractChinese(String text) {
		StringBuilder chineseText = new StringBuilder();
		Pattern pattern = Pattern.compile("[\\u4e00-\\u9fa5]");
		Matcher matcher = pattern.matcher(text);

		while (matcher.find()) {
			chineseText.append(matcher.group());
		}
		return chineseText.toString();
	}

	public static String setToStringWithNewLines(Set<String> set) {
		StringBuilder result = new StringBuilder();
		for (String s : set) {
			result.append(s).append("\n");
		}
		return result.toString();
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("")
	public record Request(@JsonProperty(required = true,
			value = "url") @JsonPropertyDescription("The url link, such as https://www.baidu.com ") String url) {
	}

	public record Response(String description) {
	}

}
