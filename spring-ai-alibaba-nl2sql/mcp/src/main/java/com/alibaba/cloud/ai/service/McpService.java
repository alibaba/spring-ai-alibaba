package com.alibaba.cloud.ai.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class McpService {

	@Autowired
	private Nl2SqlService nl2SqlService;

	/**
	 * 从数据库中获取问题所需要的数据
	 * @return 从数据库中获取问题所需要的数据
	 */
	@Tool(description = "从数据库中获取问题所需要的数据")
	public String nl2Sql(String input) throws Exception {
		String sql = nl2SqlService.nl2sql(input);
		return nl2SqlService.executeSql(sql);
	}

}
