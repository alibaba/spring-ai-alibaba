package com.alibaba.cloud.ai.plugin.feishu;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.docx.v1.model.*;
import com.lark.oapi.service.drive.v1.model.ListFileReq;
import com.lark.oapi.service.drive.v1.model.ListFileResp;
import java.nio.charset.StandardCharsets;

/**
 * @author wudihaoke214
 * @author <a href="mailto:2897718178@qq.com">wudihaoke214</a>
 */
public class FeiShuService {

	private final Client client;

	public FeiShuService(Client client) {
		this.client = client;
	}

	/**
	 * 使用tenant_access_token访问[用户身份]
	 * @param documentId 文档的唯一ID
	 * @param userAccessToken 用户访问凭证
	 * @return String 文档纯文本内容
	 */
	public String getDocumentContentByUser(String documentId, String userAccessToken) throws Exception {
		// 创建请求对象
		RawContentDocumentReq req = RawContentDocumentReq.newBuilder().documentId(documentId).lang(0).build();

		// 发起请求
		RawContentDocumentResp resp = client.docx()
			.document()
			.rawContent(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
		// 处理服务端错误
		if (!resp.success()) {
			System.out.printf("code:%s,msg:%s,reqId:%s, resp:%s%n", resp.getCode(), resp.getMsg(), resp.getRequestId(),
					Jsons.createGSON(true, false)
						.toJson(JsonParser
							.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			throw new Exception(resp.getMsg());
		}

		// 业务数据处理
		return Jsons.DEFAULT.toJson(resp.getData());
	}

	/**
	 * 使用tenant_access_token访问[应用身份]
	 * @param documentId 文档的唯一ID
	 * @param tenantAccessToken 租户访问凭证
	 * @return String 文档纯文本内容
	 */
	public String getDocumentContentByTenant(String documentId, String tenantAccessToken) throws Exception {
		// 创建请求对象
		RawContentDocumentReq req = RawContentDocumentReq.newBuilder().documentId(documentId).lang(0).build();

		// 发起请求
		RawContentDocumentResp resp = client.docx()
			.document()
			.rawContent(req, RequestOptions.newBuilder().tenantAccessToken(tenantAccessToken).build());
		// 处理服务端错误
		if (!resp.success()) {
			System.out.printf("code:%s,msg:%s,reqId:%s, resp:%s%n", resp.getCode(), resp.getMsg(), resp.getRequestId(),
					Jsons.createGSON(true, false)
						.toJson(JsonParser
							.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			throw new Exception(resp.getMsg());
		}

		// 业务数据处理
		return Jsons.DEFAULT.toJson(resp.getData());
	}

	/**
	 * 获取指定用户的文档列表
	 * @param userAccessToken 用户访问凭证
	 * @return String 文档列表JSON[token为文档唯一标识]
	 */
	public String getDocumentListByUser(String userAccessToken) throws Exception {
		// 创建请求对象
		ListFileReq req = ListFileReq.newBuilder().orderBy("EditedTime").direction("DESC").build();
		// 发起请求
		ListFileResp resp = client.drive()
			.file()
			.list(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
		// 处理服务端错误
		if (!resp.success()) {
			System.out.printf("code:%s,msg:%s,reqId:%s, resp:%s%n", resp.getCode(), resp.getMsg(), resp.getRequestId(),
					Jsons.createGSON(true, false)
						.toJson(JsonParser
							.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			throw new Exception(resp.getMsg());
		}
		return Jsons.DEFAULT.toJson(resp.getData());
	}

}
