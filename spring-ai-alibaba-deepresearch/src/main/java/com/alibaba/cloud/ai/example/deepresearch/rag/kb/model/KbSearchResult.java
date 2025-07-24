package com.alibaba.cloud.ai.example.deepresearch.rag.kb.model;

import java.util.Map;

/**
 * 知识库搜索结果
 *
 * @author hupei
 */
public record KbSearchResult(String id, String title, String content, String url, Double score,
		Map<String, Object> metadata) {
	// 无参构造函数对应的静态工厂方法
	public static KbSearchResult empty() {
		return new KbSearchResult(null, null, null, null, null, null);
	}

	// 两个参数构造函数对应的静态工厂方法
	public static KbSearchResult of(String title, String content) {
		return new KbSearchResult(null, title, content, null, null, null);
	}

	// 五个参数构造函数（id, title, content, url, score）
	public static KbSearchResult of(String id, String title, String content, String url, Double score) {
		return new KbSearchResult(id, title, content, url, score, null);
	}
}
