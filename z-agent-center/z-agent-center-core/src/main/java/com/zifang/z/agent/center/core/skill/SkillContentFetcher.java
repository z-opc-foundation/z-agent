package com.zifang.z.agent.center.core.skill;

import com.zifang.util.json.JsonUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 从 SkillCenter 拉取 Skill 内容（FEATURE014 Wishlist 2 配套）。
 *
 * <p>SkillCenter 暴露 {@code GET /api/skill/content?skillCode=xxx} 返回
 * {@code {"code":0,"msg":"success","data":"<skill.md content>"}}。</p>
 *
 * <p>center 通过这个 client 在 reconcile / distribute 时拿到 SKILL.md 全文，
 * 传给 {@link SkillAdapter#writeSkill} 落盘到 Agent 目标目录。</p>
 */
@Component
public class SkillContentFetcher {

    private static final Logger log = LogManager.getLogger(SkillContentFetcher.class);


    /**
     * SkillCenter 的 base URL（默认 http://localhost:8888 — main-starter 同进程）
     */
    @Value("${z.skill-center.base-url:http://localhost:8888}")
    private String skillCenterBaseUrl;

    /**
     * 调用超时（毫秒）
     */
    @Value("${z.skill-center.timeout-ms:5000}")
    private int timeoutMs;

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    /**
     * Java 8 兼容的 readAllBytes（InputStream.readAllBytes 是 Java 9+）
     */
    private static byte[] readAllBytes(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = in.read(chunk)) != -1) {
            buf.write(chunk, 0, n);
        }
        return buf.toByteArray();
    }

    /**
     * 拉取 Skill 内容。失败返回 null（调用方应当跳过并记录到 sync_log）。
     *
     * @return SKILL.md 全文；null 表示拉取失败
     */
    public String fetch(String skillCode) {
        String urlStr = skillCenterBaseUrl.replaceAll("/$", "")
                + "/api/skill/content?skillCode=" + urlEncode(skillCode);
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(timeoutMs);
            conn.setReadTimeout(timeoutMs);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            if (code != 200) {
                log.warn("SkillCenter returned non-200 for skillCode={}: code={}", skillCode, code);
                return null;
            }
            byte[] body;
            try (java.io.InputStream in = conn.getInputStream()) {
                body = readAllBytes(in);
            }
            String json = new String(body, StandardCharsets.UTF_8);
            Map<String, Object> resp = toPlainMap(json);
            Object data = resp.get("data");
            if (data == null) {
                return null;
            }

            String content = data.toString();
            if (content.isEmpty()) {
                log.warn("SkillCenter returned empty content for skillCode={}", skillCode);
                return null;
            }
            return content;
        } catch (Exception e) {
            log.warn("Failed to fetch skill content: skillCode={} url={}", skillCode, urlStr, e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }

        }
    }

    /**
     * JSON 字符串 → 深度摊平的 {@code Map<String,Object>}。
     * <p>
     * 必须过 {@code JsonUtil.toPlainValue}：{@code parseToMap} 只摊平第一层，
     * 嵌套节点仍是 {@code JsonObject}/{@code JsonArray}，下游按 {@code List}/{@code Map}
     * 强转会 ClassCastException。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> toPlainMap(String json) {
        return (Map<String, Object>) JsonUtil.toPlainValue(JsonUtil.parse(json));
    }
}
