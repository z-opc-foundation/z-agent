package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * Widget 启用/禁用响应 DTO.
 */
public class WidgetToggleResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer widgetEnabled;
    private String instanceCode;

    public Integer getWidgetEnabled() {
        return widgetEnabled;
    }

    public void setWidgetEnabled(Integer widgetEnabled) {
        this.widgetEnabled = widgetEnabled;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }
}
