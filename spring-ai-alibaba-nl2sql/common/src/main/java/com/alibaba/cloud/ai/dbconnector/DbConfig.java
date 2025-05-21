package com.alibaba.cloud.ai.dbconnector;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("chatbi.dbconfig")
public class DbConfig {

	private String schema;

	private String url;

	private String username;

	private String password;

	private String connectionType;

	private String dialectType;

}
