package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * Widget CORS + 限流配置响应 DTO.
 */
public class WidgetConfigResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String widgetCors;
    private Integer widgetRateLimit;
    private String instanceCode;

    public String getWidgetCors() {
        return widgetCors;
    }

    public void setWidgetCors(String widgetCors) {
        this.widgetCors = widgetCors;
    }

    public Integer getWidgetRateLimit() {
        return widgetRateLimit;
    }

    public void setWidgetRateLimit(Integer widgetRateLimit) {
        this.widgetRateLimit = widgetRateLimit;
    }

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }
}
