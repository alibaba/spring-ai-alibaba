package com.alibaba.cloud.ai.example.manus.controller.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 表示后端等待用户输入的状态。
 */
public class UserInputWaitState implements Serializable {
    private String planId;
    private String message;
    private boolean waiting;
    private String formDescription; // 新增字段：表单描述
    private List<Map<String, String>> formInputs; // 新增字段：表单输入项

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

    public String getFormDescription() {
        return formDescription;
    }

    public void setFormDescription(String formDescription) {
        this.formDescription = formDescription;
    }

    public List<Map<String, String>> getFormInputs() {
        return formInputs;
    }

    public void setFormInputs(List<Map<String, String>> formInputs) {
        this.formInputs = formInputs;
    }
}
