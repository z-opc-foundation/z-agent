package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * 反馈提交响应 DTO.
 */
public class FeedbackSubmitResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private int hooksFired;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getHooksFired() {
        return hooksFired;
    }

    public void setHooksFired(int hooksFired) {
        this.hooksFired = hooksFired;
    }
}
