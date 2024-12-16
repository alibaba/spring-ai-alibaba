package com.alibaba.cloud.ai.reader.feishu;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.docx.v1.model.*;
import com.lark.oapi.service.drive.v1.model.ListFileReq;
import com.lark.oapi.service.drive.v1.model.ListFileResp;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * @author wblu214
 * @author <a href="mailto:2897718178@qq.com">wblu214</a>
 */
public class FeiShuDocumentReader implements DocumentReader {

	private final Client client;

	private ExtractedTextFormatter textFormatter;

	public FeiShuDocumentReader(Client client) {
		this.client = client;
	}

	public FeiShuDocumentReader(Client client, ExtractedTextFormatter textFormatter) {
		this.client = client;
		this.textFormatter = textFormatter;
	}

	/**
	 * use tenant_access_token access [tenant identity]
	 * @param documentId documentId
	 * @param userAccessToken userAccessToken
	 * @return String
	 */
	public String getDocumentContentByUser(String documentId, String userAccessToken) throws Exception {
		RawContentDocumentReq req = RawContentDocumentReq.newBuilder().documentId(documentId).lang(0).build();

		RawContentDocumentResp resp = client.docx()
			.document()
			.rawContent(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
		if (!resp.success()) {
			System.out.printf("code:%s,msg:%s,reqId:%s, resp:%s%n", resp.getCode(), resp.getMsg(), resp.getRequestId(),
					Jsons.createGSON(true, false)
						.toJson(JsonParser
							.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			throw new Exception(resp.getMsg());
		}

		return Jsons.DEFAULT.toJson(resp.getData());
	}

	/**
	 * use tenant_access_token [tenant identity]
	 * @param documentId documentId
	 * @param tenantAccessToken tenantAccessToken
	 * @return String
	 */
	public String getDocumentContentByTenant(String documentId, String tenantAccessToken) throws Exception {
		RawContentDocumentReq req = RawContentDocumentReq.newBuilder().documentId(documentId).lang(0).build();

		RawContentDocumentResp resp = client.docx()
			.document()
			.rawContent(req, RequestOptions.newBuilder().tenantAccessToken(tenantAccessToken).build());
		if (!resp.success()) {
			System.out.printf("code:%s,msg:%s,reqId:%s, resp:%s%n", resp.getCode(), resp.getMsg(), resp.getRequestId(),
					Jsons.createGSON(true, false)
						.toJson(JsonParser
							.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			throw new Exception(resp.getMsg());
		}
		return Jsons.DEFAULT.toJson(resp.getData());
	}

	/**
	 * get document list
	 * @param userAccessToken userAccessToken
	 * @return String
	 */
	public String getDocumentListByUser(String userAccessToken) throws Exception {
		ListFileReq req = ListFileReq.newBuilder().orderBy("EditedTime").direction("DESC").build();
		ListFileResp resp = client.drive()
			.file()
			.list(req, RequestOptions.newBuilder().userAccessToken(userAccessToken).build());
		if (!resp.success()) {
			System.out.printf("code:%s,msg:%s,reqId:%s, resp:%s%n", resp.getCode(), resp.getMsg(), resp.getRequestId(),
					Jsons.createGSON(true, false)
						.toJson(JsonParser
							.parseString(new String(resp.getRawResponse().getBody(), StandardCharsets.UTF_8))));
			throw new Exception(resp.getMsg());
		}
		return Jsons.DEFAULT.toJson(resp.getData());
	}

	private Document toDocument(String docText) {
		docText = Objects.requireNonNullElse(docText, "");
		docText = this.textFormatter.format(docText);
		return new Document(docText);
	}

	@Override
	public List<Document> get() {
		return null;
	}

	@Override
	public List<Document> read() {
		return DocumentReader.super.read();
	}

}
