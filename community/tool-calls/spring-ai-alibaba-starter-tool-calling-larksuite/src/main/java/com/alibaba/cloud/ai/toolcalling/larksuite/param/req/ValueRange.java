package com.alibaba.cloud.ai.toolcalling.larksuite.param.req;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * @author NewGK
 */
public class ValueRange {

    @SerializedName("range")
    private String range;

    @SerializedName("values")
    private List<List<String>> values;

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public List<List<String>> getValues() {
        return values;
    }

    public void setValues(List<List<String>> values) {
        this.values = values;
    }

    public ValueRange(Builder builder) {
        this.range = builder.range;
        this.values = builder.values;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {

        @SerializedName("range")
        private String range;

        @SerializedName("values")
        private List<List<String>> values;

        public Builder range(String range) {
            this.range = range;
            return this;
        }

        public Builder values(List<List<String>> values) {
            this.values = values;
            return this;
        }

        public ValueRange build() {
            return new ValueRange(this);
        }

    }
}
