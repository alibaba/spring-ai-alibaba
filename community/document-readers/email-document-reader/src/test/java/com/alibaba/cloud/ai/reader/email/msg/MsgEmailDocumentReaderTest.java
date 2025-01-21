package com.alibaba.cloud.ai.reader.email.msg;

import com.alibaba.cloud.ai.document.Document;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * MSG文件解析器测试类
 */
public class MsgEmailDocumentReaderTest {
    
    private final MsgEmailDocumentReader reader = new MsgEmailDocumentReader();
    
    @Test
    void testSupports() {
        assertThat(reader.supports("test.msg")).isTrue();
        assertThat(reader.supports("test.MSG")).isTrue();
        assertThat(reader.supports("test.eml")).isFalse();
        assertThat(reader.supports(null)).isFalse();
    }
    
    @Test
    void testReadMsgFile() throws IOException {
        // 从测试资源目录加载测试MSG文件
        ClassPathResource resource = new ClassPathResource("test.msg");
        try (InputStream input = resource.getInputStream()) {
            Document document = reader.read(input);
            
            // 验证解析结果
            assertThat(document).isNotNull();
            assertThat(document.getText()).isNotEmpty();
            // TODO: 添加更多具体的断言来验证邮件内容
        }
    }
    
    @Test
    void testReadInvalidFile() {
        assertThrows(MsgFileException.class, () -> {
            try (InputStream input = new ClassPathResource("invalid.msg").getInputStream()) {
                reader.read(input);
            }
        });
    }
} 