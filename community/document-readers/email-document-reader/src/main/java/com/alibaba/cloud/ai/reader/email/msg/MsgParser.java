package com.alibaba.cloud.ai.reader.email.msg;

import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.poifs.filesystem.DocumentEntry;
import org.apache.poi.poifs.filesystem.DocumentInputStream;
import org.apache.poi.poifs.filesystem.Entry;
import org.apache.poi.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * MSG文件解析器
 * 负责解析MSG文件的具体实现
 * 使用Apache POI库来处理复合文件二进制格式
 */
public class MsgParser {
    private static final Logger logger = LoggerFactory.getLogger(MsgParser.class);
    
    private final POIFSFileSystem fs;
    private final DirectoryNode root;
    
    public MsgParser(InputStream input) throws IOException {
        this.fs = new POIFSFileSystem(input);
        this.root = fs.getRoot();
    }
    
    /**
     * 解析MSG文件，提取邮件内容
     * @return MsgEmailElement 包含邮件所有信息的对象
     * @throws IOException 如果读取文件出错
     */
    public MsgEmailElement parse() throws IOException {
        try {
            MsgEmailElement email = new MsgEmailElement();
            
            // 读取邮件属性
            readProperties(email);
            
            // 读取邮件正文
            readBody(email);
            
            // 读取附件
            readAttachments(email);
            
            return email;
        }
        finally {
            fs.close();
        }
    }
    
    /**
     * 读取邮件属性（主题、发件人、收件人等）
     */
    private void readProperties(MsgEmailElement email) throws IOException {
        // 读取主题
        String subject = readStringProperty(MsgPropertyTags.PR_SUBJECT);
        email.setSubject(subject);
        
        // 读取发件人信息
        String senderName = readStringProperty(MsgPropertyTags.PR_SENDER_NAME);
        String senderEmail = readStringProperty(MsgPropertyTags.PR_SENDER_EMAIL_ADDRESS);
        email.setFrom(senderEmail != null ? senderEmail : senderName);
        
        // 读取收件人信息
        String to = readStringProperty(MsgPropertyTags.PR_DISPLAY_TO);
        String cc = readStringProperty(MsgPropertyTags.PR_DISPLAY_CC);
        String bcc = readStringProperty(MsgPropertyTags.PR_DISPLAY_BCC);
        
        if (to != null) email.setTo(to);
        if (cc != null) email.setCc(cc);
        if (bcc != null) email.setBcc(bcc);
    }
    
    /**
     * 读取邮件正文内容
     */
    private void readBody(MsgEmailElement email) throws IOException {
        // 优先尝试读取HTML内容
        String htmlBody = readStringProperty(MsgPropertyTags.PR_HTML);
        if (htmlBody != null) {
            email.setHtmlContent(htmlBody);
            return;
        }
        
        // 如果没有HTML内容，读取纯文本内容
        String textBody = readStringProperty(MsgPropertyTags.PR_BODY);
        if (textBody != null) {
            email.setTextContent(textBody);
        }
    }
    
    /**
     * 读取邮件附件
     */
    private void readAttachments(MsgEmailElement email) throws IOException {
        List<MsgEmailElement.Attachment> attachments = new ArrayList<>();
        
        for (Entry entry : root) {
            if (entry instanceof DirectoryNode) {
                DirectoryNode dir = (DirectoryNode) entry;
                String name = dir.getName();
                
                if (name.startsWith(MsgPropertyTags.ATTACHMENT_PREFIX)) {
                    MsgEmailElement.Attachment attachment = readAttachment(dir);
                    if (attachment != null) {
                        attachments.add(attachment);
                    }
                }
            }
        }
        
        if (!attachments.isEmpty()) {
            email.setAttachments(attachments);
        }
    }
    
    /**
     * 读取单个附件
     */
    private MsgEmailElement.Attachment readAttachment(DirectoryNode dir) throws IOException {
        String filename = readStringProperty(dir, MsgPropertyTags.PR_ATTACH_LONG_FILENAME);
        if (filename == null) {
            return null;
        }
        
        byte[] content = readBinaryProperty(dir, MsgPropertyTags.PR_ATTACH_DATA);
        if (content == null) {
            return null;
        }
        
        return new MsgEmailElement.Attachment(filename, content);
    }
    
    /**
     * 读取字符串类型的属性
     */
    private String readStringProperty(int tag) throws IOException {
        return readStringProperty(root, tag);
    }
    
    private String readStringProperty(DirectoryNode dir, int tag) throws IOException {
        byte[] data = readBinaryProperty(dir, tag);
        return data != null ? new String(data, "UTF-8").trim() : null;
    }
    
    /**
     * 读取二进制类型的属性
     */
    private byte[] readBinaryProperty(DirectoryNode dir, int tag) throws IOException {
        String name = String.format("__substg1.0_%04X001F", tag);
        
        if (!dir.hasEntry(name)) {
            name = String.format("__substg1.0_%04X001E", tag);
            if (!dir.hasEntry(name)) {
                return null;
            }
        }
        
        DocumentEntry entry = (DocumentEntry) dir.getEntry(name);
        DocumentInputStream stream = null;
        try {
            stream = new DocumentInputStream(entry);
            return IOUtils.toByteArray(stream);
        }
        finally {
            IOUtils.closeQuietly(stream);
        }
    }
} 