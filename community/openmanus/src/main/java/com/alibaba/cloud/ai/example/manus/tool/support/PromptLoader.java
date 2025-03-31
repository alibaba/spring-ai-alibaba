package com.alibaba.cloud.ai.example.manus.tool.support;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;

/**
 * @author wangheng
 * @description @TODO
 * @date 2025/3/28 14:59
 */
public class PromptLoader {

	/**
	 * load prompt form classpath file
	 * @param fileName file name
	 * @return file content
	 */
	public static String loadPromptFromClasspath(String fileName) {
		try {
			ClassPathResource resource = new ClassPathResource(fileName);
			return new BufferedReader(new InputStreamReader(resource.getInputStream())).lines()
				.collect(Collectors.joining(System.lineSeparator()));
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
