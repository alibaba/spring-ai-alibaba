package com.alibaba.cloud.ai.dashscope.image.observation;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.fastjson.JSON;
import io.micrometer.common.KeyValue;
import io.micrometer.common.KeyValues;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.ai.image.observation.ImageModelObservationDocumentation;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

/**
 * @author 北极星
 */
public class DashScopeImageModelObservationConvention extends DefaultImageModelObservationConvention {

    public static final String DEFAULT_NAME = "gen_ai.client.operation";

    private static final String ILLEGAL_STOP_CONTENT = "<illegal_stop_content>";

    @Override
    public String getName () {
        return DEFAULT_NAME;
    }

    protected KeyValues requestStopSequences (KeyValues keyValues, ImageModelObservationContext context) {
        if (context.getRequestOptions() instanceof DashScopeChatOptions) {
            List<Object> stop = ((DashScopeChatOptions) context.getRequestOptions()).getStop();

            if (CollectionUtils.isEmpty(stop)) {
                return keyValues;
            }

            KeyValue.of(ImageModelObservationDocumentation.HighCardinalityKeyNames.RESPONSE_ID, stop, Objects::nonNull);

            String stopSequences;
            try {
                stopSequences = JSON.toJSONString(stop);
            }
            catch (Exception e) {
                stopSequences = ILLEGAL_STOP_CONTENT;
            }
            return keyValues.and(ImageModelObservationDocumentation.HighCardinalityKeyNames.REQUEST_IMAGE_SIZE.asString(), stopSequences);
        }

        return super.requestImageSize(keyValues, context);
    }

}
