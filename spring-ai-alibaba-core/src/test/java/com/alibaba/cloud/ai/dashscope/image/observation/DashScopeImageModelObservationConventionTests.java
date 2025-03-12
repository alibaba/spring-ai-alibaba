package com.alibaba.cloud.ai.dashscope.image.observation;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.dashscope.observation.conventions.AiProvider;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.ai.observation.conventions.AiOperationType;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

/**
 * @author 北极星
 */
public class DashScopeImageModelObservationConventionTests {

    private final DashScopeImageModel imageModel;

    private final TestObservationRegistry observationRegistry;

    public DashScopeImageModelObservationConventionTests () {
        this.observationRegistry = TestObservationRegistry.create();
        this.imageModel = new DashScopeImageModel(new DashScopeImageApi("sk" +
                "-7a74bd9492b24f6f835a03e01affe294"),observationRegistry);
        DefaultImageModelObservationConvention defaultImageModelObservationConvention =
                new DefaultImageModelObservationConvention();
        this.imageModel.setObservationConvention(defaultImageModelObservationConvention);
    }

    @Test
    void imageModelObservationTest () {

        DashScopeImageOptions options = DashScopeImageOptions.builder()
                .withModel("wanx-v1")
                .withN(1)
                .withWidth(1024)
                .withHeight(1024)
                .withSeed(42)
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

        TestObservationRegistryAssert.assertThat(this.observationRegistry)
                .doesNotHaveAnyRemainingCurrentObservation()
                .hasObservationWithNameEqualTo(DefaultImageModelObservationConvention.DEFAULT_NAME)
                .that()
                .hasContextualNameEqualTo("image " + "wanx-v1")
                .hasHighCardinalityKeyValue(ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_SIZE.asString(), "1024x1024")
                .hasLowCardinalityKeyValue(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_PROVIDER.asString(), AiProvider.DASHSCOPE.value())
                .hasLowCardinalityKeyValue(ImageModelObservationDocumentation.LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(), AiOperationType.IMAGE.value());
    }

}
