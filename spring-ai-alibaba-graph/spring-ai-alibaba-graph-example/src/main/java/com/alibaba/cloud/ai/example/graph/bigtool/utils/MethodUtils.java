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

package com.alibaba.cloud.ai.example.graph.bigtool.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.example.graph.bigtool.agent.Tool;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Utility class for converting Java methods to LangChain tools
 */
public class MethodUtils {

	// Cache for retrieved Javadoc to avoid repeated requests
	private static final Map<String, String> JAVADOC_CACHE = new ConcurrentHashMap<>();
	static ConcurrentHashMap<String, String> methodMap;

	static {
		try {
			methodMap = fetchMathMethodJavadoc();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert a Java method to a LangChain tool
	 * @param method The Java method to convert
	 * @return The converted Tool object, or null if conversion fails
	 */
	public static Tool convertMethodToTool(Method method) {
		if (method == null) {
			return null;
		}

		try {
			// Get method name and parameter information
			String methodName = method.getName();
			Class<?>[] paramTypes = method.getParameterTypes();

			// Get method Javadoc description - try different sources in layers
			String javadoc = getMethodJavadoc(method);

			// Create method description
			StringBuilder description = new StringBuilder();

			if (javadoc != null && !javadoc.isEmpty()) {
				// Use retrieved Javadoc
				description.append(javadoc);
			}
			else {
				// If Javadoc can't be retrieved, create a basic description
				description.append(methodName).append(": ");
				description.append("A method that accepts ");

				if (paramTypes.length == 0) {
					description.append("no parameters");
				}
				else {
					for (int i = 0; i < paramTypes.length; i++) {
						if (i > 0) {
							description.append(", ");
						}
						description.append(getEnglishTypeName(paramTypes[i]));
					}
				}
			}

			// Add parameter type information as supplement
			description.append(". Parameter types: ");
			if (paramTypes.length == 0) {
				description.append("none");
			}
			else {
				for (int i = 0; i < paramTypes.length; i++) {
					if (i > 0) {
						description.append(", ");
					}
					description.append(paramTypes[i].getSimpleName());
				}
			}

			// Add return type information
			description.append(", return type: ").append(method.getReturnType().getSimpleName());

			// Create tool object
			return new Tool(methodName, description.toString(), args -> {
				try {
					// Ensure parameter count matches
					if (args.length != paramTypes.length) {
						throw new IllegalArgumentException(
								"Expected " + paramTypes.length + " arguments, got " + args.length);
					}

					// Execute method
					return method.invoke(null, args);
				}
				catch (Exception e) {
					throw new RuntimeException("Error invoking method: " + methodName, e);
				}
			}, method.getParameterTypes());

		}
		catch (Exception e) {
			return null;
		}
	}

	/**
	 * Get Javadoc description for a method - try different sources in layers
	 * @param method The method to get documentation for
	 * @return The method's Javadoc description, or null if unavailable
	 */
	private static String getMethodJavadoc(Method method) {
		for (String s : methodMap.keySet()) {
			if (s.contains(method.getName())) {
				return methodMap.get(s);
			}
		}
		return null;
	}

	/**
	 * Fetch Javadoc for Math class methods from Oracle's online documentation
	 * @return Method Javadoc descriptions
	 */
	private static ConcurrentHashMap<String, String> fetchMathMethodJavadoc() throws Exception {

		// Network request implementation to get documentation (simplified version)
		try {
			// Build URL
			String urlStr = "https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html";

			URL url = new URL(urlStr);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);

			ConcurrentHashMap<String, String> stringObjectHashMap = new ConcurrentHashMap<>();
			int status = connection.getResponseCode();
			if (status == 200) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = reader.readLine()) != null) {
					content.append(line);
				}
				reader.close();

				Document parse = Jsoup.parse(content.toString());
				Elements tbody = parse.select("table.memberSummary").get(1).select("tbody");
				for (Element element : tbody.select("tr")) {
					String text = element.select("td.colFirst").text();
					String text1 = element.select("th.colSecond").text();
					String text2 = element.select("td.colLast").text();
					System.out.println(text + "\t" + text1 + "\t" + text2);
					stringObjectHashMap.put(text1, text2);
				}
			}

			System.out.println("Retrieved method descriptions:" + stringObjectHashMap);
			return stringObjectHashMap;
		}
		catch (Exception e) {
			System.out.println("Failed to retrieve online documentation: " + e.getMessage());
		}

		return null;
	}

	/**
	 * Get English description of a type
	 */
	private static String getEnglishTypeName(Class<?> type) {
		if (type == double.class || type == Double.class) {
			return "double precision floating point";
		}
		else if (type == float.class || type == Float.class) {
			return "single precision floating point";
		}
		else if (type == int.class || type == Integer.class) {
			return "integer";
		}
		else if (type == long.class || type == Long.class) {
			return "long integer";
		}
		else if (type == boolean.class || type == Boolean.class) {
			return "boolean";
		}
		else if (type == char.class || type == Character.class) {
			return "character";
		}
		else if (type == byte.class || type == Byte.class) {
			return "byte";
		}
		else if (type == short.class || type == Short.class) {
			return "short integer";
		}
		else {
			return type.getSimpleName();
		}
	}

}
