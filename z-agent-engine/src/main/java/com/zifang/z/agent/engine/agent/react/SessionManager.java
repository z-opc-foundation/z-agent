package com.zifang.z.agent.engine.agent.react;

import com.zifang.util.json.JsonUtil;
import com.zifang.util.json.define.TypeReference;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Session manager for bot conversations.
 * Persists sessions to local files under ~/.zbot/sessions/
 */
public class SessionManager {

    private static final File SESSION_DIR = new File(System.getProperty("user.home") + "/.zbot/sessions");
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, SessionData> sessions = new ConcurrentHashMap<>();
    private String currentSessionId;

    public SessionManager() {
        SESSION_DIR.mkdirs();
        loadIndex();
    }

    public String getCurrentSessionId() {
        if (currentSessionId == null) {
            currentSessionId = createSession();
        }
        return currentSessionId;
    }

    public String createSession() {
        String id = "session_" + System.currentTimeMillis();
        SessionData sd = new SessionData();
        sd.id = id;
        sd.createdAt = LocalDateTime.now().format(DTF);
        sd.title = "新会话";
        sessions.put(id, sd);
        currentSessionId = id;
        saveIndex();
        return id;
    }

    public void switchSession(String id) {
        if (sessions.containsKey(id)) {
            currentSessionId = id;
        }
    }

    public List<SessionSummary> listSessions() {
        List<SessionSummary> list = new ArrayList<>();
        for (SessionData sd : sessions.values()) {
            SessionSummary s = new SessionSummary();
            s.id = sd.id;
            s.title = sd.title;
            s.createdAt = sd.createdAt;
            s.messageCount = sd.messageCount;
            list.add(s);
        }
        list.sort((a, b) -> b.createdAt.compareTo(a.createdAt));
        return list;
    }

    public void deleteSession(String id) {
        sessions.remove(id);
        File f = new File(SESSION_DIR, id + ".json");
        if (f.exists()) {
            f.delete();
        }

        if (id.equals(currentSessionId)) {
            currentSessionId = null;
        }
        saveIndex();
    }

    public void saveMessages(String sessionId, List<Msg> messages) {
        SessionData sd = sessions.get(sessionId);
        if (sd == null) {
            return;
        }
        try {
            File f = new File(SESSION_DIR, sessionId + ".json");
            List<Map<String, Object>> msgs = new ArrayList<>();
            for (Msg m : messages) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("role", m.getRole());
                map.put("content", m.getContent());
                map.put("contentType", m.getContentType());
                map.put("toolName", m.getToolName());
                map.put("toolCallId", m.getToolCallId());
                map.put("isFinal", m.isFinal());
                map.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : LocalDateTime.now().toString());
                msgs.add(map);
            }
            java.nio.file.Files.write(f.toPath(), JsonUtil.toJsonExcludingNulls(msgs)
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            sd.messageCount = msgs.size();
            // Update title from first user message
            if (sd.title.equals("新会话") || sd.title == null) {
                for (Msg m : messages) {
                    if ("user".equals(m.getRole()) && m.getContent() != null && m.getContent().length() > 0) {
                        sd.title = m.getContent().length() > 30 ? m.getContent().substring(0, 30) + "..." : m.getContent();
                        break;
                    }
                }
            }
            saveIndex();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Msg> loadMessages(String sessionId) {
        try {
            File f = new File(SESSION_DIR, sessionId + ".json");
            if (!f.exists()) {
                return new ArrayList<>();
            }
            List<Map<String, Object>> raw = JsonUtil.fromJson(
                    new String(java.nio.file.Files.readAllBytes(f.toPath()), java.nio.charset.StandardCharsets.UTF_8),
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            List<Msg> msgs = new ArrayList<>();
            for (Map<String, Object> map : raw) {
                Msg m = new Msg(
                        (String) map.getOrDefault("role", "user"),
                        (String) map.getOrDefault("content", "")
                );
                m.setContentType((String) map.getOrDefault("contentType", "text"));
                m.setToolName((String) map.get("toolName"));
                m.setToolCallId((String) map.get("toolCallId"));
                Object isFinal = map.get("isFinal");
                m.setFinal(isFinal instanceof Boolean && (Boolean) isFinal);
                msgs.add(m);
            }
            return msgs;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    // ===== Internal =====

    private void loadIndex() {
        File idx = new File(SESSION_DIR, "_index.json");
        if (idx.exists()) {
            try {
                List<SessionData> list = JsonUtil.fromJson(
                        new String(java.nio.file.Files.readAllBytes(idx.toPath()), java.nio.charset.StandardCharsets.UTF_8),
                        new TypeReference<List<SessionData>>() {
                        });
                for (SessionData sd : list) {
                    sessions.put(sd.id, sd);
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void saveIndex() {
        try {
            File idx = new File(SESSION_DIR, "_index.json");
            java.nio.file.Files.write(idx.toPath(), JsonUtil.toJsonExcludingNulls(new ArrayList<>(sessions.values()))
                    .getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception ignored) {
        }
    }

    // ===== Data classes =====

    public static class SessionData {
        public String id;
        public String title;
        public String createdAt;
        public int messageCount;
    }

    public static class SessionSummary {
        public String id;
        public String title;
        public String createdAt;
        public int messageCount;
    }
}
