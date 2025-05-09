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
package com.alibaba.cloud.ai.toolcalling.larksuite;

import com.alibaba.cloud.ai.toolcalling.larksuite.param.req.ValueRange;
import com.alibaba.cloud.ai.toolcalling.larksuite.param.req.ValuesAppendReq;
import com.alibaba.cloud.ai.toolcalling.larksuite.param.req.ValuesAppendReqBody;
import com.alibaba.cloud.ai.toolcalling.larksuite.param.resp.ValuesAppendResp;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.response.RawResponse;
import com.lark.oapi.core.token.AccessTokenType;
import com.lark.oapi.core.utils.UnmarshalRespUtil;
import com.lark.oapi.service.drive.v1.enums.BaseMemberMemberTypeEnum;
import com.lark.oapi.service.drive.v1.enums.BaseMemberPermEnum;
import com.lark.oapi.service.drive.v1.model.BaseMember;
import com.lark.oapi.service.drive.v1.model.CreatePermissionMemberReq;
import com.lark.oapi.service.drive.v1.model.CreatePermissionMemberResp;
import com.lark.oapi.service.sheets.v3.model.CreateSpreadsheetReq;
import com.lark.oapi.service.sheets.v3.model.CreateSpreadsheetResp;
import com.lark.oapi.service.sheets.v3.model.QuerySpreadsheetSheetReq;
import com.lark.oapi.service.sheets.v3.model.QuerySpreadsheetSheetResp;
import com.lark.oapi.service.sheets.v3.model.Spreadsheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.List;
import java.util.function.Function;

/**
 * @author kone_net
 */
public class LarkSuiteCreateSheetService implements Function<LarkSuiteCreateSheetService.SheetRequest, Object> {

	private static final String RANGE_FORMAT = "%s!A1:%s%d";

	private static final String SHEET = "sheet";

	private static final String LARK_SHEET_URL = "/open-apis/sheets/v2/spreadsheets/%s/values_append?insertDataOption=OVERWRITE";

	private static final Logger logger = LoggerFactory.getLogger(LarkSuiteCreateSheetService.class);

	LarkSuiteProperties larkSuiteProperties;

	public LarkSuiteCreateSheetService(LarkSuiteProperties properties) {
		this.larkSuiteProperties = properties;
	}

	/**
	 * 创建飞书doc
	 * @param request the function argument
	 * @return CreateDocumentResp
	 */
	@Override
	public Object apply(SheetRequest request) {
		if (ObjectUtils.isEmpty(larkSuiteProperties.getAppId())
				|| ObjectUtils.isEmpty(larkSuiteProperties.getAppSecret())) {
			logger.error("current larksuite appId or appSecret must not be null.");
			throw new IllegalArgumentException("current larksuite appId or appSecret must not be null.");
		}

		logger.debug("current larksuite.appId is {},appSecret is {}", larkSuiteProperties.getAppId(),
				larkSuiteProperties.getAppSecret());

		Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();

		Spreadsheet spreadsheet = null;

		try {
			// 创建工作表
			spreadsheet = this.createSpreadsheet(request.title());
		}
		catch (Exception e) {
			logger.error("failed to invoke larksuite sheet create, caused by:{}", e.getMessage());
		}

		if (spreadsheet == null) {
			throw new RuntimeException("服务端创建飞书表格出错");
		}

		Spreadsheet finalSpreadsheet = spreadsheet;
		try {
			// 获取 sheetId
			String sheetId = getSheetId(client, finalSpreadsheet.getSpreadsheetToken());
			// 追加数据
			ValuesAppendReq valuesAppendReq = buildWriteRequest(finalSpreadsheet.getSpreadsheetToken(), sheetId,
					request.data());
			valuesAppend(client, valuesAppendReq);
			// 分配权限
			addPermission(client, finalSpreadsheet.getSpreadsheetToken(), request.email());
		}
		catch (Exception e) {
			logger.error("failed to create larksuite sheet, caused by: ", e);
			throw new RuntimeException(e);
		}

		return spreadsheet.getUrl();
	}

	public record SheetRequest(
			@JsonProperty(required = true,
					value = "title") @JsonPropertyDescription("the larksuite sheet title") String title,
			@JsonProperty(required = true,
					value = "email") @JsonPropertyDescription("email that needs to be authorized for the user") String email,
			@JsonProperty(required = true,
					value = "data") @JsonPropertyDescription("the larksuite sheet data") List<List<String>> data) {
	}

	/**
	 * 创建 飞书表格
	 * @param title 飞书表格标题
	 * @return 飞书表格链接
	 * @throws Exception 抛出异常
	 */
	private Spreadsheet createSpreadsheet(String title) throws Exception {
		CreateSpreadsheetReq req = CreateSpreadsheetReq.newBuilder()
			.spreadsheet(Spreadsheet.newBuilder().title(title).build())
			.build();

		Client larkClient = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret())
			.build();

		CreateSpreadsheetResp resp = larkClient.sheets().v3().spreadsheet().create(req);
		return resp.getData().getSpreadsheet();
	}

	/**
	 * 获取 Sheet1 的 sheet ID
	 * @param larkClient 飞书客户端
	 * @param spreadsheetToken 飞书表格 token
	 * @return 飞书表格 ID
	 * @throws Exception 抛出异常
	 */
	private String getSheetId(Client larkClient, String spreadsheetToken) throws Exception {
		// 查询 电子表格 的 sheet 信息
		QuerySpreadsheetSheetReq req = QuerySpreadsheetSheetReq.newBuilder().spreadsheetToken(spreadsheetToken).build();

		QuerySpreadsheetSheetResp resp = larkClient.sheets().v3().spreadsheetSheet().query(req);
		if (null == resp || null == resp.getData() || null == resp.getData().getSheets()
				|| resp.getData().getSheets().length == 0) {
			throw new RuntimeException("get sheet id is empty");
		}

		return resp.getData().getSheets()[0].getSheetId();
	}

	/**
	 * 构造 追加数据 请求
	 * @param spreadsheetToken 飞书表格 token
	 * @param sheetId sheet ID
	 * @param sheetData 追加数据
	 * @return 追加数据请求
	 */
	private ValuesAppendReq buildWriteRequest(String spreadsheetToken, String sheetId, List<List<String>> sheetData) {
		int rowCount = sheetData.size();
		int colCount = rowCount > 0 ? sheetData.get(0).size() : 0;

		return ValuesAppendReq.newBuilder()
			.spreadsheetToken(spreadsheetToken)
			.body(ValuesAppendReqBody.newBuilder()
				.valueRange(
						ValueRange.newBuilder().range(getRange(sheetId, rowCount, colCount)).values(sheetData).build())
				.build())
			.build();
	}

	private String getRange(String sheetId, int rowCount, int colCount) {
		return String.format(RANGE_FORMAT, sheetId, toColumnName(colCount), rowCount);
	}

	private String toColumnName(int colNum) {
		StringBuilder stringBuilder = new StringBuilder();
		while (colNum > 0) {
			colNum--;
			stringBuilder.insert(0, (char) ('A' + colNum % 26));
			colNum /= 26;
		}
		return stringBuilder.toString();
	}

	/**
	 * 向飞书表格追加数据
	 * @param larkClient 飞书客户端
	 * @param req 追加数据请求
	 * @throws Exception 抛出异常
	 */
	private void valuesAppend(Client larkClient, ValuesAppendReq req) throws Exception {
		// 请求参数选项
		RequestOptions reqOptions = new RequestOptions();

		// 发起请求
		RawResponse httpResponse = larkClient.post(String.format(LARK_SHEET_URL, req.getSpreadsheetToken()),
				req.getBody(), AccessTokenType.Tenant, reqOptions);

		ValuesAppendResp resp = UnmarshalRespUtil.unmarshalResp(httpResponse, ValuesAppendResp.class);
		if (resp == null) {
			throw new IllegalArgumentException("The result returned by the server is illegal");
		}

		resp.setRawResponse(httpResponse);
		resp.setRequest(req);

		if (!resp.success()) {
			throw new RuntimeException("设置权限失败: " + resp.getMsg());
		}
	}

	/**
	 * 分配飞书表格的授权
	 * @param larkClient 飞书客户端
	 * @param spreadsheetToken 飞书表格 token
	 * @param email 分配权限的邮箱
	 * @throws Exception 抛出异常
	 */
	private void addPermission(Client larkClient, String spreadsheetToken, String email) throws Exception {
		CreatePermissionMemberReq req = CreatePermissionMemberReq.newBuilder()
			.token(spreadsheetToken)
			.type(SHEET)
			.baseMember(BaseMember.newBuilder()
				.memberId(email)
				.memberType(BaseMemberMemberTypeEnum.EMAIL)
				.perm(BaseMemberPermEnum.FULL_ACCESS)
				.build())
			.needNotification(true)
			.build();

		CreatePermissionMemberResp resp = larkClient.drive().v1().permissionMember().create(req);

		if (!resp.success()) {
			throw new RuntimeException("设置权限失败: " + resp.getMsg());
		}

	}

}
