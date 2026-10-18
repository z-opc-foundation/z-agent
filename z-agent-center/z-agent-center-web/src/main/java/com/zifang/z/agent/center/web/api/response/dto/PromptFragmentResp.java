package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;

/**
 * Prompt 片段响应 DTO.
 */
public class PromptFragmentResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private String fragment;
    private int length;

    public String getFragment() {
        return fragment;
    }

    public void setFragment(String fragment) {
        this.fragment = fragment;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }
}
