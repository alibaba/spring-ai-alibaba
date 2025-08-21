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

package com.alibaba.cloud.ai.studio.runtime.enums;

import com.alibaba.cloud.ai.studio.runtime.domain.Error;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Optional;

import static com.alibaba.cloud.ai.studio.runtime.constants.ApiConstants.*;

/**
 * Title error code enum.<br>
 * Description error code enum.<br>
 *
 * @since 1.0.0.3
 */

@Getter
public enum ErrorCode {

	/**
	 * system error code
	 */
	SYSTEM_ERROR(500, RESPONSE_ERROR, "InternalError",
			"An internal error has occurred, please try again later or contact service support."),

	MISSING_PARAMS(400, INVALID_REQUEST_ERROR, "MissingParameter",
			"Required parameters [%s] missing, please check the request parameters."),

	INVALID_PARAMS(400, INVALID_REQUEST_ERROR, "InvalidParameter", "Parameters %s invalid, %s."),

	INVALID_REQUEST(400, INVALID_REQUEST_ERROR, "InvalidRequest", "Request invalid, %s."),

	UNAUTHORIZED(401, INVALID_REQUEST_ERROR, "Unauthorized", "Required to authorize, please login."),

	INVALID_TOKEN(401, INVALID_REQUEST_ERROR, "InvalidAccessToken", "Access token is invalid."),

	FORBIDDEN(403, INVALID_REQUEST_ERROR, "Forbidden", "Access denied, please contact service support for permission."),

	PERMISSION_DENIED(403, INVALID_REQUEST_ERROR, "PermissionDenied", "You do not have permission for this operation."),

	INVALID_REFRESH_TOKEN(401, INVALID_REQUEST_ERROR, "InvalidRefreshToken", "Refresh token is invalid."),

	/**
	 * plugin and tool error code
	 */
	PLUGIN_NAME_EXISTS(400, INVALID_REQUEST_ERROR, "PluginNameExists", "Plugin name already exists."),

	CREATE_PLUGIN_ERROR(500, RESPONSE_ERROR, "CreatePluginError", "Failed to create plugin."),

	UPDATE_PLUGIN_ERROR(500, RESPONSE_ERROR, "UpdatePluginError", "Failed to create plugin."),

	PLUGIN_NOT_FOUND(404, RESPONSE_ERROR, "PluginNotFound", "Plugin can not be found."),

	BUILD_TOOL_SCHEMA_ERROR(400, INVALID_REQUEST_ERROR, "BuildToolSchemaError", "Failed to build tool schema."),

	CREATE_TOOL_ERROR(500, RESPONSE_ERROR, "CreateToolError", "Failed to create tool."),

	UPDATE_TOOL_ERROR(500, RESPONSE_ERROR, "UpdateToolError", "Failed to create tool."),

	TOOL_NOT_FOUND(404, RESPONSE_ERROR, "ToolNotFound", "Tool can not be found."),

	TOOL_NAME_EXISTS(400, INVALID_REQUEST_ERROR, "ToolNameExists", "Tool name already exists."),

	TEST_TOOL_EXECUTION_TIMEOUT(503, RESPONSE_ERROR, "TestToolExecutionTimeout",
			"Test tool execution timeout more than 10s."),

	TOOL_PARAMS_MISSING(400, INVALID_REQUEST_ERROR, "ToolParameterMissing", "Tool parameter [%s] is required."),

	TOOL_PARAMS_INVALID(400, INVALID_REQUEST_ERROR, "ToolParameterInvalid", "Tool parameter [%s] is invalid, %s"),

	TOOL_EXECUTION_ERROR(500, RESPONSE_ERROR, "ToolExecution", "Failed to execute tool."),

	TOOL_NOT_TESTED(400, INVALID_REQUEST_ERROR, "ToolNotTested", "Tool test has not passed yet."),

	BUILD_TOOL_RESULT_ERROR(500, RESPONSE_ERROR, "BuildToolResultError", "Failed to build tool result."),

	/**
	 * agent app error code
	 */
	CREATE_APP_ERROR(500, RESPONSE_ERROR, "CreateAppError", "Failed to create app."),

	UPDATE_AGENT_ERROR(500, RESPONSE_ERROR, "UpdateAppError", "Failed to update app."),

	APP_NOT_FOUND(404, RESPONSE_ERROR, "AppNotFound", "App can not be found."),

	APP_NAME_EXISTS(400, INVALID_REQUEST_ERROR, "AppNameExists", "App name already exists."),

	APP_VERSION_NOT_FOUND(404, RESPONSE_ERROR, "AppVersionNotFound", "App version can not be found."),

	APP_CONFIG_NOT_FOUND(404, RESPONSE_ERROR, "AppConfigNotFound", "App config can not be found."),

	APP_TYPE_NOT_SUPPORT(400, INVALID_REQUEST_ERROR, "AppTypeNotSupport", "App type [%s] is not supported."),

	REQUEST_TIMEOUT(408, RESPONSE_ERROR, "RequestTimeOut", "Request timed out, please try again later."),

	AGENT_CALL_ERROR(500, RESPONSE_ERROR, "AgentCallError",
			"An internal error has occurred during agent call, please try again later."),

	AGENT_LOOP_EXCEEDED(500, RESPONSE_ERROR, "AgentLoopExceeded", "Agent loop has exceeded more than %s loops."),

	APP_NOT_PUBLISHED(400, INVALID_REQUEST_ERROR, "AppNotPublished", "App has not published yet."),

	/**
	 * model error code
	 */
	MODEL_NOT_FOUND(404, RESPONSE_ERROR, "ModelNotFound", "Model can not be found."),

	MODEL_CALL_ERROR(500, RESPONSE_ERROR, "ModelCallError",
			"An internal error has occurred during model call, please try again later."),

	MODEL_ADD_ERROR(500, RESPONSE_ERROR, "ModelAddError", "Model add error."),

	/**
	 * account error code
	 */
	ACCOUNT_NAME_EXISTS(400, INVALID_REQUEST_ERROR, "AccountNameExists", "Account name already exists."),

	ACCOUNT_EMAIL_EXISTS(400, INVALID_REQUEST_ERROR, "AccountEmailExists", "Account email already exists."),

	ACCOUNT_NOT_FOUND(404, RESPONSE_ERROR, "AccountNotFound", "Account can not be found."),

	ACCOUNT_PASSWORD_NOT_MATCH(400, INVALID_REQUEST_ERROR, "AccountPasswordNotMatch",
			"Account password does not match."),

	ACCOUNT_LOGIN_ERROR(401, INVALID_REQUEST_ERROR, "AccountLoginError",
			"Login error, please check username and password."),

	OAUTH2_USER_NOT_FOUND(404, RESPONSE_ERROR, "Oauth2UserNotFound", "Oauth2 user can not be found."),

	OAUTH2_CALL_ERROR(500, RESPONSE_ERROR, "Oauth2CallError",
			"An internal error has occurred during oauth2 call, please try again later."),

	DEFAULT_WORKSPACE_NOT_FOUND(404, RESPONSE_ERROR, "DefaultWorkspaceNotFound", "Default workspace can not be found."),

	/**
	 * workspace error code
	 */
	WORKSPACE_NAME_EXISTS(400, INVALID_REQUEST_ERROR, "WorkspaceNameExists", "Workspace name already exists."),

	WORKSPACE_COUNT_EXCEEDED(400, INVALID_REQUEST_ERROR, "WorkspacesLimit", "Workspace count limit."),

	WORKSPACE_NOT_FOUND(404, RESPONSE_ERROR, "WorkspaceNotFound", "Workspace can not be found."),

	WORKSPACE_ACCESS_DENIED(403, RESPONSE_ERROR, "WorkspaceAccessDenied", "Workspace access denied."),

	WORKSPACE_DELETION_FORBIDDEN(403, RESPONSE_ERROR, "WorkspaceDeletionForbidden",
			"Workspace deletion for default is forbidden."),

	/**
	 * Api Key error code
	 */
	API_KEY_NOT_FOUND(404, RESPONSE_ERROR, "ApiKeyNotFound", "Api key can not be found."),

	INVALID_API_KEY(401, RESPONSE_ERROR, "InvalidApiKey", "Api key is invalid."),

	/**
	 * HTTP json error code
	 */
	INVALID_JSON(400, INVALID_REQUEST_ERROR, "InvalidJson", "Request message is not json format."),

	MEDIA_TYPE_NOT_ACCEPTABLE(400, INVALID_REQUEST_ERROR, "MediaTypeNotAcceptable",
			"Request media type is not acceptable."),

	MEDIA_TYPE_NOT_SUPPORTED(400, INVALID_REQUEST_ERROR, "MediaTypeNotSupported",
			"Request media type is not supported."),

	REQUEST_METHOD_NOT_SUPPORTED(405, INVALID_REQUEST_ERROR, "RequestMethodNotSupported",
			"Request method is not supported."),

	/**
	 * knowledge base error code
	 */
	KNOWLEDGE_BASE_NAME_EXISTS(400, INVALID_REQUEST_ERROR, "KnowledgeBaseNameExists",
			"Knowledge base name already exists."),

	KNOWLEDGE_BASE_NOT_FOUND(404, RESPONSE_ERROR, "KnowledgeBaseNotFound", "Knowledge base can not be found."),

	DOCUMENT_NOT_FOUND(404, RESPONSE_ERROR, "DocumentNotFound", "Document can not be found."),

	DOCUMENT_RETRIEVAL_ERROR(500, RESPONSE_ERROR, "DocumentRetrievalError", "Document retrieval error."),

	DOCUMENT_RETRIEVAL_TIMEOUT(408, RESPONSE_ERROR, "DocumentRetrievalTimeout", "Document retrieval timeout."),

	UPDATE_DOCUMENT_CHUNK_ERROR(500, RESPONSE_ERROR, "UpdateDocumentChunkError", "Update document chunk error."),

	/**
	 * workflow error code
	 */
	WORKFLOW_EXECUTE_ERROR(500, RESPONSE_ERROR, "WorkflowExecuteError", "Workflow execute error, root cause is %s"),

	WORKFLOW_NODE_DEBUG_FAIL(500, RESPONSE_ERROR, "WorkflowNodeDebugError", "Workflow node debug error."),

	WORKFLOW_DEBUG_FAIL(500, RESPONSE_ERROR, "WorkflowDebugError", "Workflow debug error."),

	WORKFLOW_DEBUG_GET_PROCESS_FAIL(500, RESPONSE_ERROR, "WorkflowGetDebugProcessError",
			"Workflow get debug process error."),

	WORKFLOW_DEBUG_INIT_FAIL(500, RESPONSE_ERROR, "WorkflowDebugInitError", "Workflow init debug params error."),

	WORKFLOW_CONFIG_INVALID(400, INVALID_REQUEST_ERROR, "WorkflowConfigInvalid", "Workflow config is invalid, %s"),

	WORKFLOW_RUN_CANCEL(202, OPERATION_CANCEL, "WorkflowRunCancel", "Workflow task is canceled, %s"),

	WORKFLOW_CONFIG_ILLEGAL(400, INVALID_REQUEST_ERROR, "WorkflowConfigInvalid",
			"Workflow config is Illegal,please check the config,%s"),

	WORKFLOW_EXECUTION_TIMEOUT(503, RESPONSE_ERROR, "WorkflowExecuteTimeout", "Workflow task is timeout"),

	/**
	 * MCP error code
	 */
	CREATE_MCP_ERROR(500, RESPONSE_ERROR, "CreateMCPServerError", "Failed to create MCPServer. %s"),

	UPDATE_MCP_ERROR(500, RESPONSE_ERROR, "UpdateMCPServerError", "Failed to update MCPServer. %s"),

	MCP_NOT_FOUND(404, RESPONSE_ERROR, "MCPServerNotFound", "MCPServer can not be found."),

	MCP_CONFIG_NOT_FOUND(404, RESPONSE_ERROR, "MCPServerConfigNotFound", "MCPServer config can not be found."),

	MCP_TYPE_NOT_SUPPORT(400, INVALID_REQUEST_ERROR, "MCPServerTypeNotSupport",
			"MCPServer type [%s] is not supported."),

	MCP_REQUEST_TIMEOUT(408, RESPONSE_ERROR, "MCPRequestTimeOut",
			"MCPServer request timed out, please try again later."),

	MCP_CALL_ERROR(500, RESPONSE_ERROR, "MCPServerCallError",
			"An internal error has occurred during MCPServer call, please try again later."),

	MCP_PARSE_CONFIG_ERROR(500, RESPONSE_ERROR, "MCPParseConfigError",
			"mcpServers must be configured and only one is supported."),

	MCP_PARSE_URL_ERROR(500, RESPONSE_ERROR, "ParseUrlError",
			"ParseUrlError, please check whether the URL format is correct."),

	DELETE_MCP_ERROR(500, RESPONSE_ERROR, "DeleteMCPServerError", "Failed to delete MCPServer."),

	GET_TOOLS_ERROR(500, RESPONSE_ERROR, "DeleteMCPServerError", "Failed to delete MCPServer."),

	/**
	 * component error code
	 */
	APP_COMPONENT_LIST_ERROR(500, RESPONSE_ERROR, "ComponentListError", "Failed to obtain component list."),

	APP_COMPONENT_PUBLISH_ERROR(500, RESPONSE_ERROR, "CreateComponentError",
			"Failed to release application as component."),

	APP_COMPONENT_UPDATE_ERROR(500, RESPONSE_ERROR, "UpdateComponentError", "Failed to update component."),

	APP_COMPONENT_DETAIL_ERROR(500, RESPONSE_ERROR, "QueryComponentError", "Failed to query component detail."),

	APP_COMPONENT_DELETE_ERROR(500, RESPONSE_ERROR, "DeleteComponentError", "Failed to delete component."),

	APP_COMPONENT_REFER_ERROR(500, RESPONSE_ERROR, "ReferComponentError", "Failed to query reference of component."),

	APP_COMPONENT_QUERYCONFIG_ERROR(500, RESPONSE_ERROR, "QueryConfigError", "Failed to query config of component."),

	APP_COMPONENT_PUBLISHABLE_ERROR(500, RESPONSE_ERROR, "QueryApplicationError",
			"Failed to get information of component."),

	APP_COMPONENT_NOT_FOUND_ERROR(404, RESPONSE_ERROR, "ComponentNotFound", "Component can not be found."),

	APP_COMPONENT_SCHEMA_ERROR(500, RESPONSE_ERROR, "QuerySchemaError", "Failed to get schema of component."),

	APP_COMPONENT_ALREADY_EXISI_ERROR(500, RESPONSE_ERROR, "CreateComponentError",
			"Failed to delete component.application already release to component"),

	/**
	 * file error code
	 */
	FILE_NOT_FOUND(400, RESPONSE_ERROR, "FileNotFound", "File can not be found."),

	OSS_UPLOAD_ERROR(500, RESPONSE_ERROR, "OssUploadError", "Failed to upload file to OSS."),

	OSS_DOWNLOAD_ERROR(500, RESPONSE_ERROR, "OssDownloadError", "Failed to download file from OSS."),

	OSS_GEN_URL_ERROR(500, RESPONSE_ERROR, "OssGenUrlError", "Failed to generate url from OSS."),;

	/**
	 * http status code.
	 */
	private final int statusCode;

	private final String type;

	/**
	 * error code
	 */
	private final String code;

	/**
	 * error message.
	 */
	private final String message;

	ErrorCode(int statusCode, String type, String code, String message) {
		this.statusCode = statusCode;
		this.type = type;
		this.code = code;
		this.message = message;
	}

	public static ErrorCode of(String errorCode) {
		// success if error code is blank
		if (StringUtils.isBlank(errorCode)) {
			return null;
		}

		Optional<ErrorCode> any = Arrays.stream(values()).filter(error -> errorCode.equals(error.getCode())).findAny();

		return any.orElse(null);
	}

	public String getMessage(String... params) {
		return String.format(this.message, params);
	}

	public Error toError(String... params) {
		String message;
		if (params != null && params.length > 0) {
			message = getMessage(params);
		}
		else {
			message = this.message;
		}

		return Error.builder().statusCode(statusCode).code(code).message(message).type(type).build();
	}

}
