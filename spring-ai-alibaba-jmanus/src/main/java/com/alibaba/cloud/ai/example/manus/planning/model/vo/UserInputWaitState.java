package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import java.io.Serializable;

public class UserInputWaitState implements Serializable {
    private String planId;
    private String message;
    private boolean waiting;

    public UserInputWaitState() {
    }

    public UserInputWaitState(String planId, String message, boolean waiting) {
        this.planId = planId;
        this.message = message;
        this.waiting = waiting;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isWaiting() {
        return waiting;
    }

    public void setWaiting(boolean waiting) {
        this.waiting = waiting;
    }
}
