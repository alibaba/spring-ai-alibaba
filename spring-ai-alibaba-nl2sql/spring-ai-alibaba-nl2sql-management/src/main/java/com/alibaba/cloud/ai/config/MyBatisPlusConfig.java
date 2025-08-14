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

package com.alibaba.cloud.ai.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;

/**
 * MyBatis Plus Configuration Class
 *
 * @author Alibaba Cloud AI
 */
@Configuration
@MapperScan("com.alibaba.cloud.ai.mapper")
public class MyBatisPlusConfig {

	/**
	 * MyBatis Plus Interceptor Configuration. Add pagination plugin, optimistic locking
	 * plugin, and anti-full-table update/delete plugin
	 */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

		// Optimistic locking plugin
		interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

		return interceptor;
	}

	/**
	 * Meta object handler for automatic field filling
	 */
	@Bean
	public MetaObjectHandler metaObjectHandler() {
		return new MetaObjectHandler() {
			@Override
			public void insertFill(MetaObject metaObject) {
				this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
				this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
			}

			@Override
			public void updateFill(MetaObject metaObject) {
				this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
			}
		};
	}

}
