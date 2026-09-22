package com.zifang.z.agent.core.registry;

import com.zifang.z.agent.api.dto.AgentDto;
import com.zifang.z.agent.api.exception.AgentException;
import com.zifang.z.agent.kernel.agent.Agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * z-agent Agent 注册中心. 内存版默认实现, 启动时把 @Bean Agent + 业务方 register(...) 全部纳入.
 *
 * <p>register(name, dto, agent): 业务方在 Spring 启动时注册.
 * <p>get(name): 取出 Agent SPI 实例.
 * <p>describe(name): 取出元信息 DTO.
 * <p>list(): 列出全部 agent 元信息.
 */
public class AgentRegistry {

    private final Map<String, Entry> entries = new ConcurrentHashMap<String, Entry>();

    public void register(String name, AgentDto dto, Agent agent) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name required");
        }
        if (dto == null) {
            throw new IllegalArgumentException("dto required");
        }
        if (agent == null) {
            throw new IllegalArgumentException("agent required");
        }
        entries.put(name, new Entry(dto, agent));
    }

    public void unregister(String name) {
        entries.remove(name);
    }

    public Agent get(String name) {
        Entry e = entries.get(name);
        if (e == null) {
            throw AgentException.agentNotFound(name);
        }
        return e.agent;
    }

    public AgentDto describe(String name) {
        Entry e = entries.get(name);
        if (e == null) {
            throw AgentException.agentNotFound(name);
        }
        return e.dto;
    }

    public List<AgentDto> list() {
        List<AgentDto> out = new ArrayList<AgentDto>(entries.size());
        for (Entry e : entries.values()) {
            out.add(e.dto);
        }
        return Collections.unmodifiableList(out);
    }

    public boolean has(String name) {
        return entries.containsKey(name);
    }

    public int size() {
        return entries.size();
    }

    public List<String> names() {
        return new ArrayList<String>(entries.keySet());
    }

    private static final class Entry {
        final AgentDto dto;
        final Agent agent;

        Entry(AgentDto dto, Agent agent) {
            this.dto = dto;
            this.agent = agent;
        }
    }
}