package com.zifang.z.agent.engine.agent.multi;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Supervisor 和 Worker 之间共享的"黑板"状态。
 *
 * <p>典型用法:
 * <ul>
 *   <li>supervisor 写 "user_id" / "tenant_code" / "current_step"</li>
 *   <li>worker 读 "user_id" 来做权限过滤, 写自己的"中间结果"</li>
 *   <li>汇总阶段 supervisor 读所有 worker 写的中间结果</li>
 * </ul>
 *
 * <p>实现: {@link ConcurrentHashMap}, 读写线程安全, 不保证事务。
 * 想做严格一致性请升级到带版本号/锁的实现。
 */
public class Blackboard {

    private final ConcurrentMap<String, Object> store = new ConcurrentHashMap<>();

    public static Blackboard create() {
        return new Blackboard();
    }

    public void put(String key, Object value) {
        store.put(key, value);
    }

    public Object get(String key) {
        return store.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object v = store.get(key);
        if (v == null) {
            return null;
        }

        if (type.isInstance(v)) {
            return (T) v;
        }

        throw new ClassCastException("Blackboard[" + key + "] is " + v.getClass() + ", not " + type);
    }

    public Object remove(String key) {
        return store.remove(key);
    }

    public boolean contains(String key) {
        return store.containsKey(key);
    }

    public void clear() {
        store.clear();
    }

    public int size() {
        return store.size();
    }
}
