/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.mapper;

import com.alibaba.cloud.ai.entity.ChatMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * Chat Message Mapper Interface
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {

	/**
	 * Query message list by session ID
	 */
	@Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY create_time ASC")
	List<ChatMessage> selectBySessionId(@Param("sessionId") String sessionId);

	/**
	 * Query message count by session ID
	 */
	@Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
	int countBySessionId(@Param("sessionId") String sessionId);

	/**
	 * Query message list by session ID and role
	 */
	@Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} AND role = #{role} ORDER BY create_time ASC")
	List<ChatMessage> selectBySessionIdAndRole(@Param("sessionId") String sessionId, @Param("role") String role);

}
