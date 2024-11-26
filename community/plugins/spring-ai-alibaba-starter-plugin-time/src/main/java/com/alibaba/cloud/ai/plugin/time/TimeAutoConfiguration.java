package com.alibaba.cloud.ai.plugin.time;

import com.alibaba.cloud.ai.plugin.time.service.GetCurrentLocalTimeService;
import com.alibaba.cloud.ai.plugin.time.service.GetCurrentTimeByTimeZoneIdService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

@AutoConfiguration
public class TimeAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the current local time")
	public GetCurrentLocalTimeService getCurrentLocalTime() {
		return new GetCurrentLocalTimeService();
	}

	@Bean
	@ConditionalOnMissingBean
	@Description("Get the time of a specified city")
	public GetCurrentTimeByTimeZoneIdService getCityTime() {
		return new GetCurrentTimeByTimeZoneIdService();
	}

}