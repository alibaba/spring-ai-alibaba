package com.alibaba.cloud.ai.toolcalling.larksuite.param.req;

import com.google.gson.annotations.SerializedName;

/**
 * @author NewGK
 */
public class ValuesAppendReqBody {

    @SerializedName("valueRange")
    private ValueRange valueRange;

    public ValueRange getValueRange() {
        return valueRange;
    }

    public void setValueRange(ValueRange valueRange) {
        this.valueRange = valueRange;
    }

    public ValuesAppendReqBody(Builder builder) {
        this.valueRange = builder.valueRange;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        @SerializedName("valueRange")
        private ValueRange valueRange;

        public Builder valueRange(ValueRange valueRange) {
            this.valueRange = valueRange;
            return this;
        }

        public ValuesAppendReqBody build() {
            return new ValuesAppendReqBody(this);
        }

    }

}
