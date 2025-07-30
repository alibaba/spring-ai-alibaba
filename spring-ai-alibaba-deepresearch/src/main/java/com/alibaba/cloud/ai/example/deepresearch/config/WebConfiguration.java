package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class WebConfiguration {

	@Bean
	public FilterRegistrationBean<CorsFilter> corsFilter() {
		CorsConfiguration configuration  = new CorsConfiguration();
		configuration.setAllowCredentials(false);
		configuration.addAllowedOrigin("*");
		configuration.addAllowedHeader("*");
		configuration.addAllowedMethod("*");

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return new FilterRegistrationBean<>(new CorsFilter(source));
	}

}
