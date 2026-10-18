package com.zifang.z.agent.center.core.agent.share.dto;

import java.io.Serializable;

public class AgentShareReq implements Serializable {
    private String instanceCode;
    private String appCode;

    public String getInstanceCode() {
        return instanceCode;
    }

    public void setInstanceCode(String instanceCode) {
        this.instanceCode = instanceCode;
    }

    public String getAppCode() {
        return appCode;
    }

    public void setAppCode(String appCode) {
        this.appCode = appCode;
    }
}
