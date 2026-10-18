package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * 反馈评分响应 DTO.
 */
public class FeedbackScoreResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceCode;
    private int score;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
