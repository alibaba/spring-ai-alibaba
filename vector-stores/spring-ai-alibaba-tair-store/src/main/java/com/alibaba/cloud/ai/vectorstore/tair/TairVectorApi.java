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
package com.alibaba.cloud.ai.vectorstore.tair;

import com.aliyun.tair.tairvector.TairVector;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Provides an API for interacting with Tair Vector, extending the functionality of the
 * {@link TairVector} class. This class is designed to manage vector operations using a
 * Jedis connection pool. For initialization details of Jedis and JedisPool, please refer
 * to the Tair official documentation and GitHub.
 * <p>
 * Official Documentation: <a href="https://help.aliyun.com/zh/redis/">Tair
 * Documentation</a> <br>
 * GitHub Repository:
 * <a href="https://github.com/tair-opensource/alibabacloud-tairjedis-sdk">TairJedis SDK
 * GitHub</a>
 * </p>
 *
 * @author fuyou.lxm
 * @since 1.0.0-M3
 */
public class TairVectorApi extends TairVector {

	/**
	 * Constructs a new instance of the {@link TairVectorApi} class using a single Jedis
	 * instance.
	 * @param jedis the Jedis instance used to interact with the Tair Vector service
	 */
	public TairVectorApi(Jedis jedis) {
		super(jedis);
	}

	/**
	 * Initializes a new instance of the {@link TairVectorApi} class using a Jedis
	 * connection pool.
	 * @param jedisPool the Jedis connection pool to be used for connecting to the Tair
	 * Vector service
	 */
	public TairVectorApi(JedisPool jedisPool) {
		super(jedisPool);
	}

}
