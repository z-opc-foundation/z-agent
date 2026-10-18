package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * Widget 配置详情响应 DTO (含脱敏 API Key).
 */
public class WidgetConfigDetailResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceCode;
    private Integer widgetEnabled;
    private String widgetCors;
    private Integer widgetRateLimit;
    private Long widgetEmbedCount;
    private String widgetApiKeyMasked;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public Integer getWidgetEnabled() {
        return widgetEnabled;
    }

    public void setWidgetEnabled(Integer widgetEnabled) {
        this.widgetEnabled = widgetEnabled;
    }

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

    public Long getWidgetEmbedCount() {
        return widgetEmbedCount;
    }

    public void setWidgetEmbedCount(Long widgetEmbedCount) {
        this.widgetEmbedCount = widgetEmbedCount;
    }

    public String getWidgetApiKeyMasked() {
        return widgetApiKeyMasked;
    }

    public void setWidgetApiKeyMasked(String widgetApiKeyMasked) {
        this.widgetApiKeyMasked = widgetApiKeyMasked;
    }
}
