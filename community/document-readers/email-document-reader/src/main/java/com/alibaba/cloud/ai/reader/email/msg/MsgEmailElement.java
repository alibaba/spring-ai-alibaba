package com.alibaba.cloud.ai.reader.email.msg;

import java.util.List;

/**
 * MSG邮件元素类
 * 用于存储MSG邮件的各种属性，包括主题、发件人、收件人、正文和附件等
 */
public class MsgEmailElement {
    private String subject;
    private String from;
    private String to;
    private String cc;
    private String bcc;
    private String textContent;
    private String htmlContent;
    private List<Attachment> attachments;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getTo() {
        return to;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public String getCc() {
        return cc;
    }

    public void setCc(String cc) {
        this.cc = cc;
    }

    public String getBcc() {
        return bcc;
    }

    public void setBcc(String bcc) {
        this.bcc = bcc;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * MSG附件类
     * 用于存储MSG附件的文件名和内容
     */
    public static class Attachment {
        private final String filename;
        private final byte[] content;

        public Attachment(String filename, byte[] content) {
            this.filename = filename;
            this.content = content;
        }

        public String getFilename() {
            return filename;
        }

        public byte[] getContent() {
            return content;
        }
    }
} 