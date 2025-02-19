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
package com.alibaba.cloud.ai.reader.feishu;

import com.google.gson.JsonParser;
import com.lark.oapi.Client;
import com.lark.oapi.core.request.RequestOptions;
import com.lark.oapi.core.utils.Jsons;
import com.lark.oapi.service.docx.v1.model.*;
import com.lark.oapi.service.drive.v1.model.ListFileReq;
import com.lark.oapi.service.drive.v1.model.ListFileResp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * @author wblu214
 * @author <a href="mailto:2897718178@qq.com">wblu214</a>
 */
public class FeiShuDocumentReader implements DocumentReader {

	private static final Logger log = LoggerFactory.getLogger(FeiShuDocumentReader.class);

	private final FeiShuResource feiShuResource;

	private final Client client;

	private String documentId;

	private String userAccessToken;

	private String tenantAccessToken;

	public FeiShuDocumentReader(FeiShuResource feiShuResource) {
		this.feiShuResource = feiShuResource;
		this.client = feiShuResource.buildDefaultFeiShuClient();
	}

	public FeiShuDocumentReader(FeiShuResource feiShuResource, String documentId, String userAccessToken,
			String tenantAccessToken) {
		this(feiShuResource);
		this.documentId = documentId;
		this.userAccessToken = userAccessToken;
		this.tenantAccessToken = tenantAccessToken;
	}

	public FeiShuDocumentReader(FeiShuResource feiShuResource, String userAccessToken) {
		this(feiShuResource);
		this.userAccessToken = userAccessToken;
	}

	public FeiShuDocumentReader(FeiShuResource feiShuResource, String userAccessToken, String documentId) {
		this(feiShuResource);
		this.userAccessToken = userAccessToken;
		this.documentId = documentId;
	}

	/**
	 * use tenant_access_token access [tenant identity]
	 * @param documentId documentId
	 * @param userAccessToken userAccessToken
	 * @return String
	 */
	public Document getDocumentContentByUser(String documentId, String userAccessToken) throws Exception {
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

		return toDocument(Jsons.DEFAULT.toJson(resp.getData()));
	}

	/**
	 * use tenant_access_token [tenant identity]
	 * @param documentId documentId
	 * @param tenantAccessToken tenantAccessToken
	 * @return String
	 */
	public Document getDocumentContentByTenant(String documentId, String tenantAccessToken) throws Exception {
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
		return toDocument(Jsons.DEFAULT.toJson(resp.getData()));
	}

	/**
	 * get document list
	 * @param userAccessToken userAccessToken
	 * @return String
	 */
	public Document getDocumentListByUser(String userAccessToken) throws Exception {
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
		return toDocument(Jsons.DEFAULT.toJson(resp.getData()));
	}

	private Document toDocument(String docText) {
		return new Document(docText);
	}

	@Override
	public List<Document> get() {
		List<Document> documents = new ArrayList<>();
		if (this.feiShuResource != null) {
			loadDocuments(documents, this.feiShuResource);
		}
		return documents;
	}

	private void loadDocuments(List<Document> documents, FeiShuResource feiShuResource) {
		String appId = feiShuResource.getAppId();
		String appSecret = feiShuResource.getAppSecret();
		String source = format("feishu://%s/%s", appId, appSecret);
		try {
			documents.add(new Document(source));
			if (this.userAccessToken != null) {
				documents.add(getDocumentListByUser(userAccessToken));
			}
			else {
				log.info("userAccessToken is null");
			}
			if (this.tenantAccessToken != null && this.documentId != null) {
				documents.add(getDocumentContentByTenant(documentId, tenantAccessToken));
			}
			else {
				log.info("tenantAccessToken or documentId is null");
			}
			if (this.userAccessToken != null && this.documentId != null) {
				documents.add(getDocumentContentByUser(documentId, userAccessToken));
			}
			else {
				log.info("userAccessToken or documentId is null");
			}

		}
		catch (Exception e) {
			log.warn("Failed to load an object with appId: {}, appSecret: {},{}", appId, appSecret, e.getMessage(), e);
		}
	}

}
