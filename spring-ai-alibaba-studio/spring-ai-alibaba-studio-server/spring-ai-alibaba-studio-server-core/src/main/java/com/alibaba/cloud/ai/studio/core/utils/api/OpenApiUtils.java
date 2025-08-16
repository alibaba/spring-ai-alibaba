/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.utils.api;

import com.alibaba.cloud.ai.studio.runtime.domain.tool.ApiParameter;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.InputSchema;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Plugin;
import com.alibaba.cloud.ai.studio.runtime.domain.plugin.Tool;
import com.alibaba.cloud.ai.studio.runtime.domain.tool.ToolCallSchema;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.error.YAMLException;

import java.util.*;

/**
 * Utility class for handling OpenAPI operations. Provides methods for parsing, converting
 * and manipulating OpenAPI specifications.
 *
 * @since 1.0.0.3
 */
public class OpenApiUtils {

	/** Default value description text */
	public static String DEFAULT_VALUE_WORD = "If the parameter value cannot be determined or generated";

	/** Parameter locations that don't require request body */
	private static final List<String> NO_BODY_PARAMS_LOCATION = Arrays.asList("Path", "Query", "Header");

	/** Connector used in tool fields */
	private static final String TOOL_FIELDS_CONNECTOR = "__";

	/** OpenAPI extension name for user-passed parameters */
	@Getter
	private static final String DEFINED_EXTENSION = "x-source";

	/** Token type identifier */
	@Getter
	private static final String TOKEN_TYPE = "token";

	/** User source extension value */
	@Getter
	private static final String EXTENSION_USER_SOURCE = "user";

	/** Maximum recursion depth limit */
	private static final int MAX_DEPTH = 10;

	/**
	 * Converts first letter of string to uppercase
	 * @param str Input string
	 * @return String with first letter capitalized
	 */
	private static String firstLetterToUpperCase(String str) {
		if ("integer".equals(str)) {
			return "Number";
		}

		if (StringUtils.isNotBlank(str)) {
			String first = str.substring(0, 1);
			String remainder = str.substring(1);
			return first.toUpperCase() + remainder;
		}

		return str;
	}

	/**
	 * Converts OpenAPI parameters to YAML parameters
	 * @param parameters List of OpenAPI parameters
	 * @param inputYamlParamList List to store converted parameters
	 */
	private static void paramToYamlParam(List<Parameter> parameters, List<ApiParameter> inputYamlParamList) {
		if (parameters == null) {
			return;
		}

		parameters.forEach(item -> {
			ApiParameter paramInfo = new ApiParameter();
			paramInfo.setKey(item.getName());
			paramInfo.setRequired(item.getRequired() != null && item.getRequired());
			paramInfo.setDescription(item.getDescription());
			Schema schema = item.getSchema();
			String type = schema.getType();

			if ("object".equals(type)) {
				throw new RuntimeException("Param Not Support Object");
			}

			if ("array".equals(type)) {
				Schema schemaItem = schema.getItems();
				String itemType = schemaItem.getType();
				if ("object".equals(itemType)) {
					throw new RuntimeException("Param Not Support Array<Object>");
				}
				paramInfo.setType("Array<" + firstLetterToUpperCase(type) + ">");
			}
			else {
				paramInfo.setType(firstLetterToUpperCase(type));
			}

			paramInfo.setLocation(firstLetterToUpperCase(item.getIn()));
			if (schema.getDefault() != null) {
				paramInfo.setDefaultValue(schema.getDefault().toString());
			}

			// if need to pass extended info
			if (schema.getExtensions() != null) {
				Object paramSource = schema.getExtensions().get(DEFINED_EXTENSION);
				if (paramSource instanceof String && String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
					paramInfo.setUserInput(true);
				}
			}

			inputYamlParamList.add(paramInfo);
		});
	}

	/**
	 * Parses OpenAPI schema to form format
	 * @param yamlString OpenAPI YAML string
	 * @param formatCheck Whether to check format
	 * @return Map of path to Tool objects
	 */
	public static Map<String, Tool> parseSchemaToForm(String yamlString, boolean formatCheck) {
		long start = System.currentTimeMillis();
		ParseOptions parseOptions = new ParseOptions();
		parseOptions.setResolve(true);

		parseOptions.setResolveFully(true);
		SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlString, null, parseOptions);

		Map<String, Tool> pathMap = new HashMap<>();
		try {
			if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty() && formatCheck) {
				List<String> errMessages = parseResult.getMessages();
				errMessages.add(yamlString);
				throw new YAMLException(StringUtils.join(errMessages, "\n"));
			}

			OpenAPI openAPI = parseResult.getOpenAPI();
			String server = openAPI.getServers().get(0).getUrl();

			Paths paths = openAPI.getPaths();
			// parse parameters
			paths.forEach((path, pathItem) -> {
				Tool tool = new Tool();
				pathMap.put(path, tool);

				Tool.ToolConfig toolConfig = new Tool.ToolConfig();
				tool.setConfig(toolConfig);

				toolConfig.setPath(path);
				toolConfig.setServer(server);

				// description
				String description = pathItem.getDescription();
				tool.setDescription(description);

				// parameters
				List<Parameter> parameters = pathItem.getParameters();
				List<ApiParameter> inputYamlParamList = new ArrayList<>();
				paramToYamlParam(parameters, inputYamlParamList);
				Operation operation = null;
				if (pathItem.getPost() != null) {
					operation = pathItem.getPost();
					toolConfig.setRequestMethod("Post");
					if (operation.getParameters() != null) {
						paramToYamlParam(operation.getParameters(), inputYamlParamList);
					}

					RequestBody requestBody = operation.getRequestBody();
					if (requestBody != null) {
						Content content = requestBody.getContent();
						toolConfig.setContentType(content.keySet().iterator().next());
						content.values().forEach(e -> {
							Schema schema = e.getSchema();
							String type = schema.getType();
							if (!"object".equals(type)) {
								throw new RuntimeException("RequestBody Only Support object Type");
							}

							Map<String, Schema> properties = schema.getProperties();
							List<String> requiredKeys = schema.getRequired();
							properties.forEach((name, nameSchema) -> {
								ApiParameter yamlParamInfo = new ApiParameter();
								yamlParamInfo.setKey(name);
								yamlParamInfo.setDescription(nameSchema.getDescription());
								yamlParamInfo.setLocation("Body");

								if (requiredKeys != null && requiredKeys.contains(name)) {
									yamlParamInfo.setRequired(true);
								}

								if (nameSchema.getDefault() != null) {
									yamlParamInfo.setDefaultValue(nameSchema.getDefault().toString());
								}

								if (nameSchema.getExtensions() != null) {
									Object paramSource = nameSchema.getExtensions().get(DEFINED_EXTENSION);
									if (paramSource instanceof String
											&& String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
										yamlParamInfo.setUserInput(true);
									}
								}

								String nameSchemaType = nameSchema.getType();
								if ("array".equals(nameSchemaType)) {
									Schema schemItem = nameSchema.getItems();
									String itemType = schemItem.getType();
									if ("object".equals(itemType)) {
										// 增加父类
										yamlParamInfo.setType("Array<Object>");
										if (schemItem.getProperties() != null) {
											processSubProperties(schemItem.getProperties(), schemItem.getRequired(),
													yamlParamInfo);
										}
									}
									else {
										yamlParamInfo.setType("Array<" + firstLetterToUpperCase(itemType) + ">");
									}
								}
								else if ("object".equals(nameSchemaType)) {
									yamlParamInfo.setType("Object");
									Map<String, Schema> nameSchemaProperties = nameSchema.getProperties();
									if (nameSchemaProperties != null && !nameSchemaProperties.isEmpty()) {
										processSubProperties(nameSchemaProperties, nameSchema.getRequired(),
												yamlParamInfo);
									}
								}
								else {
									yamlParamInfo.setType(firstLetterToUpperCase(nameSchemaType));
								}
								inputYamlParamList.add(yamlParamInfo);
							});
						});
					}
				}
				else if (pathItem.getGet() != null) {
					operation = pathItem.getGet();
					toolConfig.setRequestMethod("Get");
					if (operation.getParameters() != null) {
						paramToYamlParam(operation.getParameters(), inputYamlParamList);
					}
				}
				else {
					throw new RuntimeException("NotSupportOperation Only Support post/get");
				}

				toolConfig.setInputParams(inputYamlParamList);

				// 添加输出
				ApiResponses apiResponses = operation.getResponses();
				ApiResponse apiResponse = apiResponses.get("200");
				if (apiResponse != null && apiResponse.getContent() != null) {
					MediaType mediaType = apiResponse.getContent()
						.get(org.springframework.http.MediaType.APPLICATION_JSON_VALUE);
					if (mediaType == null) {
						mediaType = apiResponse.getContent()
							.get(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE);
					}

					if (mediaType == null) {
						mediaType = apiResponse.getContent().get("*/*");
					}

					if (mediaType != null) {
						Schema responseSchema = mediaType.getSchema();
						String type = responseSchema.getType();
						if (!"object".equals(type) && !"array".equals(type)) {
							throw new RuntimeException("ResponseBody Only Support object or array Type");
						}

						List<ApiParameter> outputParams = new ArrayList<>();
						if ("object".equals(type)) {
							Map<String, Schema> properties = responseSchema.getProperties();
							if (!CollectionUtils.isEmpty(properties)) {

								properties.forEach((name, nameSchema) -> {
									ApiParameter yamlParamInfo = new ApiParameter();
									yamlParamInfo.setKey(name);
									yamlParamInfo.setDescription(nameSchema.getDescription());
									String nameSchemaType = nameSchema.getType();
									if ("array".equals(nameSchemaType)) {
										Schema schemItem = nameSchema.getItems();
										String itemType = schemItem.getType();
										if ("object".equals(itemType)) {
											// 增加父类
											yamlParamInfo.setType("Array<Object>");
											if (schemItem.getProperties() != null) {
												processSubProperties(schemItem.getProperties(), schemItem.getRequired(),
														yamlParamInfo);
											}
										}
										else {
											yamlParamInfo.setType("Array<" + firstLetterToUpperCase(itemType) + ">");
										}
									}
									else if ("object".equals(nameSchemaType)) {
										yamlParamInfo.setType("Object");
										Map<String, Schema> nameSchemaProperties = nameSchema.getProperties();
										if (nameSchemaProperties != null && !nameSchemaProperties.isEmpty()) {
											processSubProperties(nameSchemaProperties, nameSchema.getRequired(),
													yamlParamInfo);
										}
									}
									else {
										yamlParamInfo.setType(firstLetterToUpperCase(nameSchemaType));
									}
									outputParams.add(yamlParamInfo);
								});
							}
						}
						else {
							// not support array
						}
						toolConfig.setOutputParams(outputParams);
					}
				}
				pathMap.put(path, tool);
			});
		}
		catch (Exception ex) {
			LogUtils.error("parseOpenAPISchemaToForm exception", yamlString, ex);
			if (formatCheck) {
				throw ex;
			}
		}

		return pathMap;
	}

	/**
	 * Builds tool call schema from list of tools
	 * @param tools List of tools
	 * @return List of tool call schemas
	 */
	public static List<ToolCallSchema> buildToolCallSchema(List<Tool> tools) {
		if (CollectionUtils.isEmpty(tools)) {
			return new ArrayList<>();
		}

		List<ToolCallSchema> toolCallSchemas = new ArrayList<>();
		for (Tool tool : tools) {
			ToolCallSchema toolCallSchema = buildToolCallSchema(tool);
			toolCallSchemas.add(toolCallSchema);
		}

		return toolCallSchemas;
	}

	/**
	 * Builds tool call schema from single tool
	 * @param tool Tool object
	 * @return Tool call schema
	 */
	public static ToolCallSchema buildToolCallSchema(Tool tool) {
		ToolCallSchema toolSchema = new ToolCallSchema();
		toolSchema.setName(tool.getName());
		toolSchema.setDescription(tool.getDescription());

		List<ApiParameter> apiParameters = tool.getConfig().getInputParams();
		if (CollectionUtils.isEmpty(apiParameters)) {
			return toolSchema;
		}

		InputSchema inputSchema = new InputSchema();
		inputSchema.setType("object");
		Map<String, Object> properties = new HashMap<>();
		List<String> requiredList = new ArrayList<>();

		buildToolFunctionParameters(apiParameters, null, properties, requiredList);

		inputSchema.setProperties(properties);
		inputSchema.setRequired(requiredList);

		toolSchema.setExamples(toolSchema.getExamples());
		toolSchema.setInputSchema(inputSchema);

		return toolSchema;
	}

	/**
	 * Builds tool function parameters
	 * @param apiParameters List of API parameters
	 * @param parent Parent parameter
	 * @param properties Properties map
	 * @param requiredList List of required parameters
	 */
	private static void buildToolFunctionParameters(List<ApiParameter> apiParameters, ApiParameter parent,
			Map<String, Object> properties, List<String> requiredList) {
		if (CollectionUtils.isEmpty(apiParameters)) {
			return;
		}

		apiParameters.forEach(item -> {
			if (item.isUserInput()) {
				return;
			}

			Map<String, Object> param = new HashMap<>();
			param.put("type", item.getType());
			param.put("description", item.getDescription());
			param.put("enum", item.getEnums());

			String key = item.getKey();
			if (parent != null) {
				key = parent.getKey() + TOOL_FIELDS_CONNECTOR + key;
			}

			properties.put(key, param);
			if (item.isRequired()) {
				requiredList.add(item.getKey());
			}

			buildToolFunctionParameters(item.getProperties(), item, properties, requiredList);
		});
	}

	/**
	 * Processes sub-properties of a parameter
	 * @param properties Map of properties
	 * @param requiredKeys List of required keys
	 * @param yamlParamInfo Parameter info object
	 */
	private static void processSubProperties(Map<String, Schema> properties, List<String> requiredKeys,
			ApiParameter yamlParamInfo) {
		properties.forEach((key, value) -> {
			ApiParameter param = new ApiParameter();
			param.setKey(key);
			param.setDescription(value.getDescription());
			param.setLocation("Body");
			if (requiredKeys != null && requiredKeys.contains(key)) {
				param.setRequired(true);
			}

			// userInput
			if (value.getExtensions() != null) {
				Object paramSource = value.getExtensions().get(DEFINED_EXTENSION);
				if (paramSource instanceof String && String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
					param.setUserInput(true);
				}
			}

			if (value.getDefault() != null) {
				param.setDefaultValue(value.getDefault().toString());
			}

			String type = value.getType();
			if ("array".equals(type)) {
				Schema schemaItem = value.getItems();
				String itemType = schemaItem.getType();
				if ("object".equals(itemType)) {
					// add parent
					param.setType("Array<Object>");
					if (schemaItem.getProperties() != null) {
						processSubProperties(schemaItem.getProperties(), new ArrayList<>(), param);
					}
					yamlParamInfo.addSubParam(param);
				}
				else {
					param.setType("Array<" + firstLetterToUpperCase(itemType) + ">");
					yamlParamInfo.addSubParam(param);
				}
			}
			else if ("object".equals(type)) {
				param.setType("Object");
				if (value.getProperties() != null) {
					processSubProperties(value.getProperties(), new ArrayList<>(), param);
				}
				yamlParamInfo.addSubParam(param);
			}
			else {
				param.setType(firstLetterToUpperCase(type));
				yamlParamInfo.addSubParam(param);
			}
		});
	}

	/**
	 * Parses OpenAPI schema
	 * @param yamlString OpenAPI YAML string
	 * @return List of API parameters
	 */
	public static List<ApiParameter> parseOpenAPISchema(String yamlString) {
		long start = System.currentTimeMillis();
		try {
			ParseOptions parseOptions = new ParseOptions();
			parseOptions.setResolve(true);
			// parse reference
			parseOptions.setResolveFully(true);
			SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlString, null, parseOptions);
			if (!CollectionUtils.isEmpty(parseResult.getMessages())) {
				List<String> errMessages = parseResult.getMessages();
				errMessages.add(yamlString);
				throw new YAMLException(StringUtils.join(errMessages, "\n"));
			}

			List<ApiParameter> result = new ArrayList<>();
			OpenAPI openAPI = parseResult.getOpenAPI();
			Paths paths = openAPI.getPaths();
			if (!CollectionUtils.isEmpty(paths)) {
				// 遍历每个 url
				paths.forEach((path, pathItem) -> pathItem.readOperationsMap().values().forEach(operation -> {
					// 遍历 get post put 等方法
					if (operation != null) {
						List<ApiParameter> subResult = parseRestfulMethod(operation);
						if (!CollectionUtils.isEmpty(subResult)) {
							result.addAll(subResult);
						}
					}
				}));
			}

			return result;
		}
		catch (YAMLException e) {
			LogUtils.monitor("OpenAPIUtils", "parseOpenAPISchema", start, "YAMLException", yamlString, e);
			return Collections.emptyList();
		}
	}

	/**
	 * Parses RESTful method parameters
	 * @param operation Operation object
	 * @return List of API parameters
	 */
	private static List<ApiParameter> parseRestfulMethod(Operation operation) {
		List<ApiParameter> result = new ArrayList<>();

		// Check parameters
		if (operation.getParameters() != null) {
			operation.getParameters().forEach(parameter -> checkParameterExtension(parameter, result));
		}

		// Check request body
		if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
			operation.getRequestBody().getContent().forEach((contentType, mediaType) -> {
				Schema<?> mediaTypeSchema = mediaType.getSchema();
				if (mediaTypeSchema != null && !CollectionUtils.isEmpty(mediaTypeSchema.getProperties())) {
					mediaTypeSchema.getProperties()
						.forEach((propName, propSchema) -> checkSchemaExtension(propName, (Schema<?>) propSchema,
								result, 0));
				}
			});
		}
		return result;
	}

	/**
	 * Checks schema extensions
	 * @param schemaField Schema field name
	 * @param schema Schema object
	 * @param resultList List to store results
	 * @param currentDepth Current recursion depth
	 * @throws YAMLException If recursion depth limit is reached
	 */
	private static void checkSchemaExtension(String schemaField, Schema<?> schema, List<ApiParameter> resultList,
			int currentDepth) throws YAMLException {
		if (currentDepth >= MAX_DEPTH) {
			throw new YAMLException(String.format("Schema extensions recursion depth limit(%d) reached.", MAX_DEPTH));
		}

		if (!CollectionUtils.isEmpty(schema.getExtensions())) {
			Object paramSource = schema.getExtensions().get(DEFINED_EXTENSION);
			if (paramSource instanceof String && String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
				String type = schema.getType();
				// 防止用户使用 "token" 类型的参数
				if (StringUtils.isNotBlank(type) && TOKEN_TYPE.equals(type)) {
					throw new YAMLException("Type \"token\" is not allowed.");
				}

				ApiParameter param = new ApiParameter();
				param.setKey(schemaField);
				param.setType(type);
				param.setDescription(schema.getDescription());
				resultList.add(param);
			}
		}

		if (StringUtils.isNotBlank(schema.getType()) && "object".equals(schema.getType())
				&& !CollectionUtils.isEmpty(schema.getProperties())) {
			schema.getProperties()
				.forEach((propName, propSchema) -> checkSchemaExtension(propName, (Schema<?>) propSchema, resultList,
						currentDepth + 1));
		}
	}

	/**
	 * Checks parameter extensions
	 * @param parameter Parameter object
	 * @param resultList List to store results
	 */
	private static void checkParameterExtension(Parameter parameter, List<ApiParameter> resultList) {
		Map<String, Object> extensions = parameter.getExtensions();
		if (!CollectionUtils.isEmpty(extensions) && parameter.getName() != null) {
			Object paramSource = parameter.getExtensions().get(DEFINED_EXTENSION);
			if (paramSource instanceof String && String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
				String type = null;
				if (parameter.getSchema() != null) {
					type = parameter.getSchema().getType();
					// token is not allowed
					if (StringUtils.isNotBlank(type) && TOKEN_TYPE.equals(type)) {
						throw new YAMLException("Type \"token\" is not allowed.");
					}
				}

				ApiParameter param = new ApiParameter();
				param.setKey(parameter.getName());
				param.setType(type);
				param.setDescription(parameter.getDescription());
				resultList.add(param);
			}
		}
	}

	/**
	 * Converts OpenAPI YAML to JSON
	 * @param yaml YAML string
	 * @return JSON string
	 */
	public static String parseOpenAPIToJson(String yaml) {
		OpenAPI openAPI = new OpenAPIV3Parser().readContents(yaml).getOpenAPI();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		try {
			String json = mapper.writeValueAsString(openAPI);
			return json;
		}
		catch (Exception ex) {
			LogUtils.error("parseOpenAPIToJson error", yaml, ex);
			return null;
		}
	}

	/**
	 * Parses OpenAPI object
	 * @param yamlString YAML string
	 * @return List of error messages
	 */
	public static List<String> parseOpenAPIObject(String yamlString) {
		ParseOptions parseOptions = new ParseOptions();
		parseOptions.setResolve(true);
		parseOptions.setResolveFully(true);
		SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlString, null, parseOptions);
		if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty()) {
			return parseResult.getMessages();
		}
		return null;
	}

	/**
	 * Parses OpenAPI schema without user source
	 * @param yamlString YAML string
	 * @return List of API parameters
	 */
	public static List<ApiParameter> parseOpenAPISchemaWithOutUserSource(String yamlString) {
		ParseOptions parseOptions = new ParseOptions();
		parseOptions.setResolve(true);
		// 关联所有引用
		parseOptions.setResolveFully(true);
		SwaggerParseResult parseResult = new OpenAPIV3Parser().readContents(yamlString, null, parseOptions);
		if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty()) {
			List<String> errMessages = parseResult.getMessages();
			errMessages.add(yamlString);
			throw new YAMLException(StringUtils.join(errMessages, "\n"));
		}

		List<ApiParameter> result = new ArrayList<>();
		OpenAPI openAPI = parseResult.getOpenAPI();
		Paths paths = openAPI.getPaths();

		if (!paths.isEmpty()) {
			// iterate all urls
			paths.forEach((path, pathItem) -> pathItem.readOperationsMap().values().forEach(operation -> {
				// operations for get/post/put methods
				if (operation != null) {
					List<ApiParameter> subResult = parseOpenAPIRestfulMethodWithOutUserSource(operation);
					if (!subResult.isEmpty()) {
						result.addAll(subResult);
					}
				}
			}));
		}

		return result;
	}

	/**
	 * Builds OpenAPI YAML from plugin and tool
	 * @param plugin Plugin object
	 * @param tool Tool object
	 * @return YAML string
	 */
	public static String buildOpenAPIYaml(Plugin plugin, Tool tool) {
		OpenAPI openAPI = new OpenAPI(SpecVersion.V31);
		// set header info
		Info info = new Info().description(plugin.getDescription()).title(plugin.getName()).version("v1");
		openAPI.info(info);

		// set server
		Server apiServer = new Server();
		apiServer.setUrl(plugin.getConfig().getServer());
		List<Server> servers = List.of(apiServer);
		openAPI.setServers(servers);

		// set apis like path, parameters
		Paths paths = new Paths();
		PathItem pathItem = new PathItem();
		String requestMethod = tool.getConfig().getRequestMethod().toLowerCase();
		List<ApiParameter> inputParams = tool.getConfig().getInputParams();
		List<ApiParameter> yamlParamInfoListBody = new ArrayList<>();
		List<ApiParameter> yamlParamInfoNoBody = new ArrayList<>();
		if (inputParams != null && !inputParams.isEmpty()) {
			inputParams.forEach(item -> {
				String description = item.getDescription();
				if (StringUtils.isNotBlank(item.getDefaultValue()) && !description.contains(DEFAULT_VALUE_WORD)) {
					item.setDescription(description + DEFAULT_VALUE_WORD + "，请使用" + item.getDefaultValue());
				}

				if (NO_BODY_PARAMS_LOCATION.contains(item.getLocation())) {
					yamlParamInfoNoBody.add(item);
				}
				else {
					yamlParamInfoListBody.add(item);
				}
			});
		}

		List<Parameter> apiParameterList = new ArrayList<>();
		if (!yamlParamInfoNoBody.isEmpty()) {
			for (ApiParameter param : yamlParamInfoNoBody) {
				Parameter parameter = new Parameter();
				parameter.setIn(param.getLocation().toLowerCase());
				parameter.setName(param.getKey());
				parameter.setRequired(param.isRequired());
				parameter.setDescription(param.getDescription());

				// this is for user input
				if (param.isUserInput()) {
					Map<String, Object> extensions = new HashMap<>();
					extensions.put("x-source", "user");
					parameter.setExtensions(extensions);
				}

				Schema schema = new Schema<>();
				schema.setType(param.getType().toLowerCase());
				schema.setDefault(param.getDefaultValue());
				parameter.setSchema(schema);
				apiParameterList.add(parameter);
			}
		}

		List<ApiParameter> outputParams = tool.getConfig().getOutputParams();
		Operation post = new Operation();
		post.setSummary(tool.getDescription());
		post.setDescription(tool.getDescription());

		if ("post".equals(requestMethod)) {
			pathItem.setPost(post);
			String operationId;

			String path = tool.getConfig().getPath();
			if (path.length() > 1) {
				operationId = path.substring(1);
			}
			else {
				operationId = IdGenerator.uuid();
			}

			post.setOperationId(operationId);
			if (!apiParameterList.isEmpty()) {
				post.setParameters(apiParameterList);
			}

			// check if need to pass params for body
			if (!yamlParamInfoListBody.isEmpty()) {
				RequestBody requestBody = new RequestBody();
				requestBody.setRequired(true);
				Content content = new Content();
				Schema postBodySchema = new Schema<>();
				postBodySchema.setType("object");
				MediaType postParam = new MediaType();
				postParam.setSchema(postBodySchema);

				content.put(tool.getConfig().getContentType(), postParam);
				Map<String, Schema> propertiesSchemaMap = new HashMap<>();
				List<String> requiredList = new ArrayList<>();
				exchangeSchema(yamlParamInfoListBody, propertiesSchemaMap, requiredList);
				postBodySchema.setProperties(propertiesSchemaMap);

				if (!requiredList.isEmpty()) {
					postBodySchema.setRequired(requiredList);
				}

				requestBody.setContent(content);
				post.setRequestBody(requestBody);
			}
		}
		else if ("get".equals(requestMethod)) {
			post = new Operation();
			pathItem.setGet(post);
			if (!apiParameterList.isEmpty()) {
				post.setParameters(apiParameterList);
			}
		}

		// add response
		if (outputParams != null && !outputParams.isEmpty()) {
			List<ApiParameter> outPutYamlParam = new ArrayList<>(outputParams);

			ApiResponse apiResponse = new ApiResponse();
			Content contentRes = new Content();
			MediaType responseMedia = new MediaType();
			Schema responseSchema = new Schema();
			responseSchema.setType("object");
			Map<String, Schema> responseSchemaMap = new HashMap<>();
			List<String> responseRequiredList = new ArrayList<>();
			exchangeSchema(outPutYamlParam, responseSchemaMap, responseRequiredList);
			responseSchema.setProperties(responseSchemaMap);

			if (!responseRequiredList.isEmpty()) {
				responseSchema.setRequired(responseRequiredList);
			}
			responseMedia.setSchema(responseSchema);
			contentRes.put("application/json", responseMedia);
			apiResponse.setContent(contentRes);
			apiResponse.setDescription("查询成功");
			ApiResponses apiResponses = new ApiResponses();
			apiResponses.put("200", apiResponse);
			post.setResponses(apiResponses);
		}

		paths.addPathItem(tool.getConfig().getPath(), pathItem);
		openAPI.setPaths(paths);
		return Yaml.pretty(openAPI);
	}

	/**
	 * Parses OpenAPI RESTful method without user source
	 * @param operation Operation object
	 * @return List of API parameters
	 */
	private static List<ApiParameter> parseOpenAPIRestfulMethodWithOutUserSource(Operation operation) {
		List<ApiParameter> result = new ArrayList<>();

		// Check parameters
		if (operation.getParameters() != null) {
			operation.getParameters().forEach(parameter -> checkParameterExtensionWithOutUserSource(parameter, result));
		}

		// Check request body
		if (operation.getRequestBody() != null && operation.getRequestBody().getContent() != null) {
			operation.getRequestBody().getContent().forEach((contentType, mediaType) -> {
				Schema<?> mediaTypeSchema = mediaType.getSchema();
				if (mediaTypeSchema != null && mediaTypeSchema.getProperties() != null
						&& !mediaTypeSchema.getProperties().isEmpty()) {
					List<String> requiredKeys = mediaTypeSchema.getRequired();
					mediaTypeSchema.getProperties().forEach((propName, propSchema) -> {
						checkSchemaExtensionWithOutUserSource(propName, (Schema<?>) propSchema, result, 0, null,
								requiredKeys);
					});
				}
			});
		}

		return result;
	}

	/**
	 * Checks schema extension without user source
	 * @param schemaField Schema field name
	 * @param schema Schema object
	 * @param resultList List to store results
	 * @param currentDepth Current recursion depth
	 * @param parentName Parent name
	 * @param requiredKeys List of required keys
	 * @throws YAMLException If recursion depth limit is reached
	 */
	private static void checkSchemaExtensionWithOutUserSource(String schemaField, Schema<?> schema,
			List<ApiParameter> resultList, int currentDepth, String parentName, List<String> requiredKeys)
			throws YAMLException {
		if (currentDepth >= MAX_DEPTH) {
			throw new YAMLException(String.format("Schema extensions recursion depth limit(%d) reached.", MAX_DEPTH));
		}

		boolean needAdd = true;
		if (schema.getExtensions() != null && !schema.getExtensions().isEmpty()) {
			Object paramSource = schema.getExtensions().get(DEFINED_EXTENSION);
			if (String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
				needAdd = false;
			}
		}

		if (!needAdd) {
			return;
		}

		String type = schema.getType();
		// token is not allowed
		if (StringUtils.isNotBlank(type) && TOKEN_TYPE.equals(type)) {
			throw new YAMLException("Type \"token\" is not allowed.");
		}

		if (StringUtils.isNotBlank(schema.getType()) && "object".equals(schema.getType())
				&& schema.getProperties() != null && schema.getProperties().size() > 0) {
			schema.getProperties()
				.forEach((propName, propSchema) -> checkSchemaExtensionWithOutUserSource(propName,
						(Schema<?>) propSchema, resultList, currentDepth + 1,
						parentName == null ? schemaField : parentName + TOOL_FIELDS_CONNECTOR + schemaField, null));
		}
		else {
			ApiParameter param = new ApiParameter();
			param.setKey(parentName == null ? schemaField : parentName + TOOL_FIELDS_CONNECTOR + schemaField);
			param.setType(type);
			param.setEnums(schema.getEnum());
			param.setDescription(schema.getDescription());
			if (requiredKeys != null && requiredKeys.contains(schemaField)) {
				param.setRequired(true);
			}
			resultList.add(param);
		}
	}

	/**
	 * Checks parameter extension without user source
	 * @param parameter Parameter object
	 * @param resultList List to store results
	 */
	private static void checkParameterExtensionWithOutUserSource(Parameter parameter, List<ApiParameter> resultList) {
		Map<String, Object> extensions = parameter.getExtensions();
		boolean needAdd = true;
		if (extensions != null && !extensions.isEmpty()) {
			Object paramSource = parameter.getExtensions().get(DEFINED_EXTENSION);
			if (String.valueOf(paramSource).equals(EXTENSION_USER_SOURCE)) {
				needAdd = false;
			}
		}

		if (!needAdd) {
			return;
		}

		String type = parameter.getSchema().getType();
		// token is not allowed
		if (StringUtils.isNotBlank(type) && TOKEN_TYPE.equals(type)) {
			throw new YAMLException("Type \"token\" is not allowed.");
		}
		if (StringUtils.isNotBlank(type) && "object".equals(type)) {
			throw new YAMLException("Parameter Not Allow Object Type");
		}

		ApiParameter param = new ApiParameter();
		param.setKey(parameter.getName());
		param.setType(type);
		param.setRequired(parameter.getRequired() != null && parameter.getRequired());
		param.setDescription(parameter.getDescription());
		resultList.add(param);
	}

	/**
	 * Exchanges schema between API parameters and OpenAPI schema
	 * @param allParams List of API parameters
	 * @param propertiesMap Properties map
	 * @param requiredList List of required parameters
	 */
	private static void exchangeSchema(List<ApiParameter> allParams, Map<String, Schema> propertiesMap,
			List<String> requiredList) {
		for (ApiParameter param : allParams) {
			Schema paramSchema = new Schema<>();
			paramSchema.setType(param.getType().toLowerCase());
			paramSchema.setDescription(param.getDescription());
			paramSchema.setDefault(param.getDefaultValue());
			if (param.isUserInput()) {
				Map<String, Object> extensions = new HashMap<>();
				extensions.put("x-source", "user");
				paramSchema.setExtensions(extensions);
			}

			if (param.isRequired()) {
				requiredList.add(param.getKey());
			}

			if (param.getType().startsWith("Array")) {
				Schema schemaItem = new Schema<>();
				schemaItem.setDescription(param.getDescription());
				paramSchema.setType("array");
				if (param.getType().equals("Array<Object>")) {
					List<ApiParameter> properties = param.getProperties();
					if (properties != null && !properties.isEmpty()) {
						Map<String, Schema> nexMap = new HashMap<>();
						List<String> subRequiredList = new ArrayList<>();
						exchangeSchema(properties, nexMap, subRequiredList);
						schemaItem.setProperties(nexMap);
						if (!subRequiredList.isEmpty()) {
							schemaItem.setRequired(subRequiredList);
						}
					}
				}
				else {
					schemaItem.setType(arrayTypeChange(param.getType()));
				}
				paramSchema.setItems(schemaItem);
			}
			else {
				List<ApiParameter> properties = param.getProperties();
				if (properties != null && !properties.isEmpty()) {
					Map<String, Schema> nexMap = new HashMap<>();
					List<String> subRequiredList = new ArrayList<>();
					exchangeSchema(properties, nexMap, subRequiredList);
					if (!subRequiredList.isEmpty()) {
						paramSchema.setRequired(subRequiredList);
					}
					paramSchema.setProperties(nexMap);
				}
			}
			propertiesMap.put(param.getKey(), paramSchema);
		}
	}

	/**
	 * Converts array type string to OpenAPI type
	 * @param type Array type string
	 * @return OpenAPI type string
	 */
	private static String arrayTypeChange(String type) {
		return switch (type) {
			case "Array<String>" -> "string";
			case "Array<Object>" -> "object";
			case "Array<Number>" -> "number";
			case "Array<Boolean>" -> "boolean";
			default -> throw new RuntimeException("NotSupportType");
		};
	}

}
