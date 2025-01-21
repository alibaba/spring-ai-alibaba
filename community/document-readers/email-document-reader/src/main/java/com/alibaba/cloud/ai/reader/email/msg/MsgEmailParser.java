package com.alibaba.cloud.ai.reader.email.msg;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hmef.Attachment;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentMetadata;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MSG邮件解析工具类
 * 用于将MsgEmailElement转换为Document对象
 */
public class MsgEmailParser {
    
    private MsgEmailParser() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 将MsgEmailElement转换为Document对象
     * @param email MsgEmailElement对象
     * @return Document对象
     */
    public static Document convertToDocument(MsgEmailElement email) {
        if (email == null) {
            return null;
        }
        
        // 创建元数据
        Map<String, String> metadata = new HashMap<>();
        metadata.put("subject", email.getSubject());
        metadata.put("from", email.getFrom());
        metadata.put("to", email.getTo());
        if (email.getCc() != null) metadata.put("cc", email.getCc());
        if (email.getBcc() != null) metadata.put("bcc", email.getBcc());
        
        // 创建文档内容
        StringBuilder content = new StringBuilder();
        content.append("Subject: ").append(email.getSubject()).append("\n");
        content.append("From: ").append(email.getFrom()).append("\n");
        content.append("To: ").append(email.getTo()).append("\n");
        if (email.getCc() != null) content.append("Cc: ").append(email.getCc()).append("\n");
        if (email.getBcc() != null) content.append("Bcc: ").append(email.getBcc()).append("\n");
        content.append("\n");
        
        // 添加邮件正文
        if (email.getHtmlContent() != null) {
            content.append(email.getHtmlContent());
        } else if (email.getTextContent() != null) {
            content.append(email.getTextContent());
        }
        
        // 处理附件
        List<Attachment> attachments = new ArrayList<>();
        if (email.getAttachments() != null) {
            for (MsgEmailElement.Attachment attachment : email.getAttachments()) {
                attachments.add(new Attachment(
                    attachment.getFilename(),
                    new ByteArrayInputStream(attachment.getContent())
                ));
            }
        }
        
        // 创建Document对象
        return Document.builder()
                .text(content.toString())
                .metadata(new DocumentMetadata(metadata))
                .attachments(attachments)
                .build();
    }
} 