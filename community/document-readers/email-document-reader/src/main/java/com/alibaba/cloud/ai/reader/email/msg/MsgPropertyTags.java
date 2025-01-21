package com.alibaba.cloud.ai.reader.email.msg;

/**
 * MSG文件属性标签常量类
 * 定义了MSG文件中常用的属性标签
 */
public class MsgPropertyTags {
    // 邮件主题
    public static final int PR_SUBJECT = 0x0037;
    
    // 发件人
    public static final int PR_SENDER_NAME = 0x0C1A;
    public static final int PR_SENDER_EMAIL_ADDRESS = 0x0C1F;
    
    // 收件人
    public static final int PR_DISPLAY_TO = 0x0E04;
    public static final int PR_DISPLAY_CC = 0x0E03;
    public static final int PR_DISPLAY_BCC = 0x0E02;
    
    // 邮件正文
    public static final int PR_BODY = 0x1000;
    public static final int PR_HTML = 0x1013;
    public static final int PR_RTF_COMPRESSED = 0x1009;
    
    // 附件相关
    public static final String ATTACHMENT_PREFIX = "__attach_version1.0_#";
    public static final int PR_ATTACH_LONG_FILENAME = 0x3707;
    public static final int PR_ATTACH_DATA = 0x3701;
    
    private MsgPropertyTags() {
        // 私有构造函数，防止实例化
    }
} 