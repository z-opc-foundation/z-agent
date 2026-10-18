package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 心跳响应 DTO.
 */
public class HeartbeatResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean ok;
    private long serverTime;
    private List<String> pendingSkills;
    private int reconciledSkills;
    private List<Object> commands;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public long getServerTime() {
        return serverTime;
    }

    public void setServerTime(long serverTime) {
        this.serverTime = serverTime;
    }

    public List<String> getPendingSkills() {
        return pendingSkills;
    }

    public void setPendingSkills(List<String> pendingSkills) {
        this.pendingSkills = pendingSkills;
    }

    public int getReconciledSkills() {
        return reconciledSkills;
    }

    public void setReconciledSkills(int reconciledSkills) {
        this.reconciledSkills = reconciledSkills;
    }

    public List<Object> getCommands() {
        return commands;
    }

    public void setCommands(List<Object> commands) {
        this.commands = commands;
    }
}
