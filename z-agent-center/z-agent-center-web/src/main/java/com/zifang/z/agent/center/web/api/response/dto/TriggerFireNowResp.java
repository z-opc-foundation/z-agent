package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * 触发器手动触发响应 DTO.
 */
public class TriggerFireNowResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean ok;
    private String answer;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
