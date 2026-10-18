package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 人类输入收件箱响应 DTO.
 */
public class HumanInboxResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<?> records;
    private int total;
    private String assignee;

    public List<?> getRecords() {
        return records;
    }

    public void setRecords(List<?> records) {
        this.records = records;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
}
