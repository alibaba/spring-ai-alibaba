package com.alibaba.cloud.ai.memory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Title memory entity.<br>
 * Description conversation interaction related information .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

	private String role;

	private String content;

	private Integer inputTokens;

	private Integer outputTokens;

	private Integer totalTokens;

	private Long createdAt;

}
