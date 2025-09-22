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

import com.alibaba.cloud.ai.entity.Agent;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * Agent Mapper Interface
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface AgentMapper extends BaseMapper<Agent> {

	@Select("""
			SELECT * FROM agent ORDER BY create_time DESC
			""")
	List<Agent> findAll();

	@Select("""
			SELECT * FROM agent WHERE id = #{id}
			""")
	Agent findById(Long id);

	@Select("""
			SELECT * FROM agent WHERE status = #{status} ORDER BY create_time DESC
			""")
	List<Agent> findByStatus(String status);

	@Select("""
			SELECT * FROM agent
			WHERE (name LIKE CONCAT('%', #{keyword}, '%') 
				   OR description LIKE CONCAT('%', #{keyword}, '%') 
				   OR tags LIKE CONCAT('%', #{keyword}, '%'))
			ORDER BY create_time DESC
			""")
	List<Agent> searchByKeyword(@Param("keyword") String keyword);

	@Insert("""
			INSERT INTO agent (name, description, avatar, status, prompt, category, admin_id, tags, create_time, update_time, human_review_enabled)
			VALUES (#{name}, #{description}, #{avatar}, #{status}, #{prompt}, #{category}, #{adminId}, #{tags}, #{createTime}, #{updateTime}, #{humanReviewEnabled})
			""")
	@Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
	int insert(Agent agent);

	@Update("""
			UPDATE agent SET 
				name = #{name}, 
				description = #{description}, 
				avatar = #{avatar}, 
				status = #{status}, 
				prompt = #{prompt},
				category = #{category}, 
				admin_id = #{adminId}, 
				tags = #{tags}, 
				update_time = #{updateTime}, 
				human_review_enabled = #{humanReviewEnabled} 
			WHERE id = #{id}
			""")
	int update(Agent agent);

	@Delete("""
			DELETE FROM agent WHERE id = #{id}
			""")
	int deleteById(Long id);
}
