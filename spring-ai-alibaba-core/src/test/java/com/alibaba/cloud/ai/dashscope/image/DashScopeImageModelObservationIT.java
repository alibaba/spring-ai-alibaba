package com.alibaba.cloud.ai.dashscope.image;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import com.alibaba.cloud.ai.dashscope.observation.conventions.AiProvider;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.image.*;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashscopeAiTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".+")
/**
 * @author 北极星
 */ public class DashScopeImageModelObservationIT {

    @Autowired
    ImageModel imageModel;

    @Autowired
    TestObservationRegistry observationRegistry;

    @Test
    void imageModelObservationTest () {

        var options = ImageOptionsBuilder.builder().model("wanx2.1-t2i-turbo")
                .withHeight(1024)
                .withWidth(1024)
                .N(1)
                .build();

        var instructions = """
                A light cream colored mini golden doodle with a sign that contains the message "I'm on my way to BARCADE!".""";

        ImagePrompt imagePrompt = new ImagePrompt(instructions, options);

        ImageResponse imageResponse = imageModel.call(imagePrompt);

        assertThat(imageResponse.getResults()).hasSize(1);

        ImageResponseMetadata imageResponseMetadata = imageResponse.getMetadata();
        assertThat(imageResponseMetadata.getCreated()).isPositive();

        var generation = imageResponse.getResult();
        Image image = generation.getOutput();
        assertThat(image.getUrl()).isNotEmpty();

        TestObservationRegistryAssert.assertThat(this.observationRegistry).doesNotHaveAnyRemainingCurrentObservation().hasObservationWithNameEqualTo(DefaultImageModelObservationConvention.DEFAULT_NAME).that().hasContextualNameEqualTo("image " + "wanx2.1-t2i-turbo").hasHighCardinalityKeyValue(ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_SIZE.asString(), "1024x1024").hasLowCardinalityKeyValue(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER.asString(), AiProvider.DASHSCOPE.value()).hasLowCardinalityKeyValue(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(), AiOperationType.IMAGE.value());
    }
}
