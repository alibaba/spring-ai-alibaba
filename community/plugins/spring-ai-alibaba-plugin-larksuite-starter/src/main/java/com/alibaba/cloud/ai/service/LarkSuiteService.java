package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.properties.LarkSuiteProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.lark.oapi.Client;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReq;
import com.lark.oapi.service.docx.v1.model.CreateDocumentReqBody;
import com.lark.oapi.service.docx.v1.model.CreateDocumentResp;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.function.Function;

/**
 * @author 北极星
 */
public class LarkSuiteService implements Function<LarkSuiteService.Request, Object> {

    private static final Logger logger = LoggerFactory.getLogger(LarkSuiteService.class);

    @Resource
    LarkSuiteProperties larkSuiteProperties;

    @Override
    public Object apply(Request request) {
        if (ObjectUtils.isEmpty(larkSuiteProperties.getAppId()) || ObjectUtils.isEmpty(larkSuiteProperties.getAppSecret())) {
            logger.error("current spring.ai.community.plugin.tool.larksuite must not be null.");
            throw new IllegalArgumentException("current spring.ai.community.plugin.tool.larksuite must not be null.");
        }

        logger.debug("current spring.ai.community.plugin.tool.larksuite.appId is {},appSecret is {}", larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret());

        Client client = Client.newBuilder(larkSuiteProperties.getAppId(), larkSuiteProperties.getAppSecret()).build();

        CreateDocumentResp resp = null;

        try {
            resp = client.docx().document().create(CreateDocumentReq.newBuilder().createDocumentReqBody(CreateDocumentReqBody.newBuilder().title("title").folderToken("fldcniHf40Vcv1DoEc8SXeuA0Zd").build()).build());
        } catch (Exception e) {
            logger.error("failed to invoke baidu search caused by:{}", e.getMessage());
            throw new RuntimeException(e);
        }

        if (!resp.success()) {
            logger.error("code:{},msg:{},reqId:{}", resp.getCode(), resp.getMsg(), resp.getRequestId());
            return resp.getError();
        }
        return resp.getData();
    }

    public record Request(
            @JsonProperty(required = true, value = "title") @JsonPropertyDescription("the larksuite title") String title,
            @JsonProperty(required = true, value = "folderToken") @JsonPropertyDescription("the larksuite folderToken") String folderToken) {
    }
}
