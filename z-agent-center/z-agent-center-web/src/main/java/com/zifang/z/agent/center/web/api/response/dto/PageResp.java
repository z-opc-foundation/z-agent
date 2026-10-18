package com.zifang.z.agent.center.web.api.response.dto;

import java.io.Serializable;
import java.util.List;

/**
 * 通用分页响应 DTO.
 *
 * @param <T> 记录类型
 */
public class PageResp<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> records;
    private long total;
    private int page;
    private int size;

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
