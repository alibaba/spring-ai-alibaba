package com.alibaba.cloud.ai.memory.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * Title mysql memory entity.<br>
 * Description conversation interaction related information about mysql.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Data
@TableName("conversation_memory")
public class ConversationMemoryForMySQL {

	@TableId(type = IdType.AUTO)
	private String id;

	private String conversationId;

	private String content;

	private String memoryType;

	private Date createdAt;

	private Date updatedAt;

}