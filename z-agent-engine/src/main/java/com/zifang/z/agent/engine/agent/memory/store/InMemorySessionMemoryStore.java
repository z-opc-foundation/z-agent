package com.zifang.z.agent.engine.agent.memory.store;

import com.zifang.z.agent.engine.agent.memory.model.MemoryEntry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基于堆内存的 {@link SessionMemoryStore} 默认实现。
 *
 * <ul>
 *   <li>SHORT 条目：存放在 {@link LinkedHashMap} 中，实现 LRU 淘汰（超过 maxShortEntries 则移除最老的）</li>
 *   <li>LONG 条目：存放在独立 {@link List} 中，不过期</li>
 * </ul>
 * <p>
 * 注意：本实现仅用于开发测试 / 轻量场景，生产环境请接入 z-agent-center-core提供的持久化实现。
 */
public class InMemorySessionMemoryStore implements SessionMemoryStore {

    private final int maxShortEntries;
    private final Map<String, LinkedHashMap<String, MemoryEntry>> shortTermMap;
    private final Map<String, List<MemoryEntry>> longTermMap;
    private final Map<String, AtomicInteger> sizeCache;

    public InMemorySessionMemoryStore() {
        this(100);
    }

    public InMemorySessionMemoryStore(int maxShortEntries) {
        this.maxShortEntries = maxShortEntries;
        this.shortTermMap = new ConcurrentHashMap<>();
        this.longTermMap = new ConcurrentHashMap<>();
        this.sizeCache = new ConcurrentHashMap<>();
    }

    @Override
    public void save(MemoryEntry entry) {
        if (entry == null || entry.getSessionId() == null) {
            return;
        }
        if (entry.getType() == MemoryEntry.MemoryType.SHORT) {
            saveShort(entry);
        } else {
            saveLong(entry);
        }
        // invalidate size cache
        sizeCache.remove(entry.getSessionId());
    }

    @Override
    public List<MemoryEntry> getHistory(String sessionId, int limit) {
        List<MemoryEntry> shortList = getShortTermEntries(sessionId);
        List<MemoryEntry> longList = getLongTermEntries(sessionId);

        // 合并两个列表
        List<MemoryEntry> merged = new ArrayList<>(shortList.size() + longList.size());
        merged.addAll(shortList);
        merged.addAll(longList);

        // 按 timestamp 升序（最老的在前），取最近 limit 条
        Collections.sort(merged, Comparator.comparingLong(MemoryEntry::getTimestamp));

        if (limit > 0 && merged.size() > limit) {
            return new ArrayList<>(merged.subList(merged.size() - limit, merged.size()));
        }
        return merged;
    }

    @Override
    public void clear(String sessionId) {
        shortTermMap.remove(sessionId);
        longTermMap.remove(sessionId);
        sizeCache.remove(sessionId);
    }

    @Override
    public int size(String sessionId) {
        AtomicInteger cached = sizeCache.get(sessionId);
        if (cached != null) {
            return cached.get();
        }
        int s = shortTermMap.get(sessionId) != null ? shortTermMap.get(sessionId).size() : 0;
        int l = longTermMap.get(sessionId) != null ? longTermMap.get(sessionId).size() : 0;
        int total = s + l;
        sizeCache.put(sessionId, new AtomicInteger(total));
        return total;
    }

    // ---------- private ----------

    private void saveShort(MemoryEntry entry) {
        String sid = entry.getSessionId();
        LinkedHashMap<String, MemoryEntry> map = shortTermMap.get(sid);
        if (map == null) {
            map = createLruMap();
            shortTermMap.put(sid, map);
        }
        // 2026-10-04: key 从 "sessionId + timestamp" 换成 "sessionId + entry.getId()"。
        // timestamp 是 currentTimeMillis()，毫秒级不唯一 —— 同一毫秒追加的两条会互相覆盖
        // （实测 7 条只剩 1 条，见 MemoryEntry#id 的注释）。
        // 原来那两行"先 remove 旧 key 再 put 同一个 key"是空操作（remove 的就是马上要
        // 覆盖的同一个 key），put 本身已覆盖，删掉不改任何语义。
        map.put(sid + "_" + entry.getId(), entry);
    }

    private void saveLong(MemoryEntry entry) {
        String sid = entry.getSessionId();
        List<MemoryEntry> list = longTermMap.get(sid);
        if (list == null) {
            list = new ArrayList<>();
            longTermMap.put(sid, list);
        }
        list.add(entry);
    }

    private List<MemoryEntry> getShortTermEntries(String sessionId) {
        LinkedHashMap<String, MemoryEntry> map = shortTermMap.get(sessionId);
        if (map == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(map.values());
    }

    private List<MemoryEntry> getLongTermEntries(String sessionId) {
        List<MemoryEntry> list = longTermMap.get(sessionId);
        if (list == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(list);
    }

    /**
     * 创建支持 LRU 淘汰的 LinkedHashMap。
     * 每次插入时判断 size 是否超过 maxShortEntries，若超过则移除最老的条目（eldest）。
     */
    private LinkedHashMap<String, MemoryEntry> createLruMap() {
        return new LinkedHashMap<String, MemoryEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, MemoryEntry> eldest) {
                return size() > InMemorySessionMemoryStore.this.maxShortEntries;
            }
        };
    }
}