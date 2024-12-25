import com.alibaba.cloud.ai.functioncalling.googletranslate.GoogleTranslateService;
import com.sun.tools.javac.Main;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author 北极星
 */
@SpringBootTest
@ContextConfiguration(classes = Main.class)
public class GoogleTranslateTest {

    private static final Logger log = LoggerFactory.getLogger(GoogleTranslateTest.class);

    private final ChatClient chatClient;

    public GoogleTranslateTest(@NotNull ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 微软翻译
     *
     * @link <a href="https://api.cognitive.microsofttranslator.com"/a>
     * 版本号 version 3.0
     * ApplicationYml spring.ai.alibaba.functioncalling.microsofttranslate 传入 api-key
     */
    @Test
    protected void GoogleTranslate () {
        String text = "你好，spring-ai-alibaba!";

        String ans = chatClient.prompt().functions("googleTranslateFunction").user(text).call().content();
        log.info("translated text -> : ${}", ans);
    }
}