package com.alibaba.cloud.ai.memory.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Title es memory entity.<br>
 * Description conversation interaction related information about es.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "conversation_memory")
public class ConversationMemoryForES {

	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private String conversationId;

	private String content;

	@Field(type = FieldType.Keyword)
	private String memoryType;

	@Field(type = FieldType.Long)
	private long createdAt;

	@Field(type = FieldType.Long)
	private long updatedAt;

}