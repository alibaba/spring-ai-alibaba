package com.alibaba.cloud.ai.reader.email.msg;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.DocumentReader;

import java.io.IOException;
import java.io.InputStream;

/**
 * Microsoft Outlook MSG文件解析器
 * MSG文件是一个复合文件二进制格式(Compound File Binary Format)
 * 用于解析Microsoft Outlook的邮件存储格式
 */
public class MsgEmailDocumentReader implements DocumentReader {
    
    private static final Logger logger = LoggerFactory.getLogger(MsgEmailDocumentReader.class);
    
    @Override
    public boolean supports(String filename) {
        return filename != null && filename.toLowerCase().endsWith(".msg");
    }
    
    @Override
    public Document read(InputStream inputStream) throws IOException {
        try {
            // 解析MSG文件结构
            MsgParser msgParser = new MsgParser(inputStream);
            MsgEmailElement emailElement = msgParser.parse();

            // 转换为统一的Document格式
            return MsgEmailParser.convertToDocument(emailElement);
        }
        catch (Exception e) {
            logger.error("Failed to parse MSG file", e);
            throw new MsgFileException("Failed to parse MSG file", e);
        }
        finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}