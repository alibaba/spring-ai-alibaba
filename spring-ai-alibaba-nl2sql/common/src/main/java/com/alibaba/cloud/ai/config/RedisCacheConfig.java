package com.alibaba.cloud.ai.config;

import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
@Configuration
public class RedisCacheConfig {

	/**
	 * 自定义 RedisCacheManager
	 * <p>
	 * 修改 Redis 序列化方式，默认 JdkSerializationRedisSerializer
	 * @param redisConnectionFactory {@link RedisConnectionFactory}
	 * @param cacheProperties {@link CacheProperties}
	 * @return {@link RedisCacheManager}
	 */
	@Bean
	public RedisCacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory,
			CacheProperties cacheProperties) {
		return RedisCacheManager.builder(RedisCacheWriter.nonLockingRedisCacheWriter(redisConnectionFactory))
			.cacheDefaults(redisCacheConfiguration(cacheProperties))
			.build();
	}

	/**
	 * 自定义 RedisCacheConfiguration
	 * @param cacheProperties {@link CacheProperties}
	 * @return {@link RedisCacheConfiguration}
	 */
	@Bean
	RedisCacheConfiguration redisCacheConfiguration(CacheProperties cacheProperties) {

		RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig();

		config = config
			.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.string()));
		config = config
			.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(RedisSerializer.json()));

		CacheProperties.Redis redisProperties = cacheProperties.getRedis();

		if (redisProperties.getTimeToLive() != null) {
			config = config.entryTtl(redisProperties.getTimeToLive());
		}
		if (!redisProperties.isCacheNullValues()) {
			config = config.disableCachingNullValues();
		}
		if (!redisProperties.isUseKeyPrefix()) {
			config = config.disableKeyPrefix();
		}
		config = config.computePrefixWith(name -> name + ":");// 覆盖默认key双冒号
																// CacheKeyPrefix#prefixed
		return config;
	}

}
