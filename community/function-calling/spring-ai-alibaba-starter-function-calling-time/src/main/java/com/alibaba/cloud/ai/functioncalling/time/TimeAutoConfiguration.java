package com.alibaba.cloud.ai.functioncalling.time;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

/**
 * @author chengle
 */
@ConditionalOnClass({ GetCurrentLocalTimeService.class, GetCurrentTimeByTimeZoneIdService.class })
@ConditionalOnProperty(prefix = "spring.ai.alibaba.functioncalling.time", name = "enabled", havingValue = "true")
public class TimeAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the current local time.")
	public GetCurrentLocalTimeService getCurrentLocalTimeFunction() {
		return new GetCurrentLocalTimeService();
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the time of a specified city.")
	public GetCurrentTimeByTimeZoneIdService getCityTimeFunction() {
		return new GetCurrentTimeByTimeZoneIdService();
	}

}