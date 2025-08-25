/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.config;

import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Title redisson config.<br>
 * Description redisson config.<br>
 *
 * @since 1.0.0.3
 */

@Configuration
public class RedissonConfig {

	@Value("${spring.data.redis.host}")
	private String host;

	@Value("${spring.data.redis.port:6379}")
	private Integer port;

	@Value("${spring.data.redis.password:}")
	private String password;

	@Value("${spring.data.redis.database:0}")
	private Integer database;

	@Bean
	public RedissonClient redissonClient() {
		Config config = new Config();

		// use custom codec
		var codec = new JsonJacksonCodec(JsonUtils.getObjectMapper());
		config.setCodec(codec);

		config.useSingleServer().setAddress("redis://" + host + ":" + port).setDatabase(database);

		if (password != null && !password.isEmpty()) {
			config.useSingleServer().setPassword(password);
		}

		return Redisson.create(config);
	}

}
