/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.documentation.graph.examples;

import com.alibaba.cloud.ai.graph.checkpoint.savers.redis.RedisSaver;

import org.redisson.Redisson;
import org.redisson.config.Config;

public class TimeTravelRedisExample {

	public static void main(String[] args) throws Exception {
		var config = new Config();
		config.useSingleServer().setAddress(args.length == 0 ? "redis://localhost:6379" : args[0]);
		var client = Redisson.create(config);
		try {
			var graph = TimeTravelExample.createGraph(RedisSaver.builder().redisson(client).build());
			TimeTravelExample.replayAndEdit(graph);
		}
		finally {
			client.shutdown();
		}
	}

}
