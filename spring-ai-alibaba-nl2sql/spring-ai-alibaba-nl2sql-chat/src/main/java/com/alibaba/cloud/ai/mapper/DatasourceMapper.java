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

import com.alibaba.cloud.ai.entity.Datasource;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 数据源 Mapper 接口
 *
 * @author Alibaba Cloud AI
 */
@Mapper
public interface DatasourceMapper extends BaseMapper<Datasource> {

	/**
	 * 根据状态查询数据源列表
	 */
	@Select("SELECT * FROM datasource WHERE status = #{status} ORDER BY create_time DESC")
	List<Datasource> selectByStatus(@Param("status") String status);

	/**
	 * 根据类型查询数据源列表
	 */
	@Select("SELECT * FROM datasource WHERE type = #{type} ORDER BY create_time DESC")
	List<Datasource> selectByType(@Param("type") String type);

	/**
	 * 获取数据源统计信息 - 按状态统计
	 */
	@Select("SELECT status, COUNT(*) as count FROM datasource GROUP BY status")
	List<Map<String, Object>> selectStatusStats();

	/**
	 * 获取数据源统计信息 - 按类型统计
	 */
	@Select("SELECT type, COUNT(*) as count FROM datasource GROUP BY type")
	List<Map<String, Object>> selectTypeStats();

	/**
	 * 获取数据源统计信息 - 按测试状态统计
	 */
	@Select("SELECT test_status, COUNT(*) as count FROM datasource GROUP BY test_status")
	List<Map<String, Object>> selectTestStatusStats();

}