package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * Widget API Key 生成响应 DTO.
 */
public class WidgetKeyGenerateResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String widgetApiKey;
    private Integer widgetEnabled;
    private String instanceCode;

    public String getWidgetApiKey() {
        return widgetApiKey;
    }

    public void setWidgetApiKey(String widgetApiKey) {
        this.widgetApiKey = widgetApiKey;
    }

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
