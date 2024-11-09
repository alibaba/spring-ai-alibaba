package com.alibaba.cloud.ai.observation.conventions;

/**
 * Extended collection of systems providing AI functionality. Based on the OpenTelemetry
 * Semantic Conventions for AI Systems.
 *
 * @author Lumian
 * @since 1.0.0
 * @see <a href=
 * "https://github.com/open-telemetry/semantic-conventions/tree/main/docs/gen-ai">OTel
 * Semantic Conventions</a>.
 * @see org.springframework.ai.observation.conventions.AiProvider
 */
public enum AiProvider {

	// @formatter:off

    // Please, keep the alphabetical sorting.
    DASHSCOPE("dashscope");

    private final String value;

    AiProvider(String value) {
        this.value = value;
    }

    public String value() {
        return this.value;
    }

    // @formatter:on

}
