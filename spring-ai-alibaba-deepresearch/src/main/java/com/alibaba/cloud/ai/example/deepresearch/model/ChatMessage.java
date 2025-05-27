package com.alibaba.cloud.ai.example.deepresearch.model;

import java.util.List;

/**
 * @author yingzi
 * @date 2025/5/27 16:10
 */

public record ChatMessage(String role, List<ContentItem> conents) {
	public record ContentItem(
			/**
			 * 指明类型是text or image
			 */
			String type, String text, String imageUrl) {
	}
}
