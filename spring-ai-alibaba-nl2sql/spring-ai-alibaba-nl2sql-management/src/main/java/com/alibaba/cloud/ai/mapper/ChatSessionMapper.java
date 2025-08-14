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

import com.alibaba.cloud.ai.entity.ChatSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat Session Mapper Interface
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {

	/**
	 * Query session list by agent ID
	 */
	@Select("SELECT * FROM chat_session WHERE agent_id = #{agentId} AND status != 'deleted' ORDER BY is_pinned DESC, update_time DESC")
	List<ChatSession> selectByAgentId(@Param("agentId") Integer agentId);

	/**
	 * Query session details by session ID
	 */
	@Select("SELECT * FROM chat_session WHERE id = #{sessionId} AND status != 'deleted'")
	ChatSession selectBySessionId(@Param("sessionId") String sessionId);

	/**
	 * Soft delete all sessions for an agent
	 */
	@Update("UPDATE chat_session SET status = 'deleted', update_time = #{updateTime} WHERE agent_id = #{agentId}")
	int softDeleteByAgentId(@Param("agentId") Integer agentId, @Param("updateTime") LocalDateTime updateTime);

	/**
	 * Update session time
	 */
	@Update("UPDATE chat_session SET update_time = #{updateTime} WHERE id = #{sessionId}")
	int updateSessionTime(@Param("sessionId") String sessionId, @Param("updateTime") LocalDateTime updateTime);

	/**
	 * Update session pinned status
	 */
	@Update("UPDATE chat_session SET is_pinned = #{isPinned}, update_time = #{updateTime} WHERE id = #{sessionId}")
	int updatePinStatus(@Param("sessionId") String sessionId, @Param("isPinned") boolean isPinned,
			@Param("updateTime") LocalDateTime updateTime);

	/**
	 * Update session title
	 */
	@Update("UPDATE chat_session SET title = #{title}, update_time = #{updateTime} WHERE id = #{sessionId}")
	int updateTitle(@Param("sessionId") String sessionId, @Param("title") String title,
			@Param("updateTime") LocalDateTime updateTime);

	/**
	 * Soft delete session
	 */
	@Update("UPDATE chat_session SET status = 'deleted', update_time = #{updateTime} WHERE id = #{sessionId}")
	int softDeleteById(@Param("sessionId") String sessionId, @Param("updateTime") LocalDateTime updateTime);

}
