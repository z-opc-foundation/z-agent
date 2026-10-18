package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.Map;

/**
 * 插件执行响应 DTO (动态结构，保留 Map).
 */
public class PluginExecuteResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, Object> data;

    public PluginExecuteResp() {
    }

    public PluginExecuteResp(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
