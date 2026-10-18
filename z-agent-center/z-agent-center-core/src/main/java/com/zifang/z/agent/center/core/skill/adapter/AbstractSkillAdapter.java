package com.zifang.z.agent.center.core.skill.adapter;

import com.zifang.util.core.io.file.FileDirUtil;
import com.zifang.util.json.JsonUtil;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SkillAdapter 抽象基类 — 统一文件系统操作 + HTTP 通知协议。
 *
 * <p>各 AgentType 的子类只需指定 {@link #getAgentType()} 和
 * {@link #getDefaultSkillRootPath()}，文件写入 / 删除 / Agent 通知的通用逻辑都在这里。</p>
 */
public abstract class AbstractSkillAdapter implements SkillAdapter {

    protected static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    protected static final int TIMEOUT_SECONDS = 30;
    protected final Logger log = LogManager.getLogger(getClass());
    protected final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build();


    /**
     * 把 ~ 展开为用户主目录。
     */
    protected Path resolveRoot(String skillRootPath) {
        String root = skillRootPath;
        if (root.startsWith("~/")) {
            root = System.getProperty("user.home") + root.substring(1);
        } else if (root.equals("~")) {
            root = System.getProperty("user.home");
        }
        return Paths.get(root);
    }

    /**
     * 计算单 skill 的目标路径：{@code <root>/<instanceCode>/<skillCode>/SKILL.md}。
     *
     * <p>按 instanceCode 隔离，避免多实例共享同一目录造成写冲突。</p>
     */
    protected Path resolveSkillPath(String instanceCode, String skillCode) {
        Path root = resolveRoot(getDefaultSkillRootPath());
        return root.resolve(instanceCode).resolve(skillCode).resolve("SKILL.md");
    }

    @Override
    public String writeSkill(String instanceCode, String skillCode, String content) {
        Path target = resolveSkillPath(instanceCode, skillCode);
        try {
            Files.createDirectories(target.getParent());
            // Java 8 兼容: Files.writeString 是 Java 11+ API
            byte[] bytes = content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8);
            Files.write(target, bytes,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            log.info("Skill written: instanceCode={} skillCode={} path={}",
                    instanceCode, skillCode, target.toAbsolutePath());
            return target.toAbsolutePath().toString();
        } catch (IOException e) {
            log.error("Failed to write skill: instanceCode={} skillCode={}",
                    instanceCode, skillCode, e);
            throw new RuntimeException("Failed to write skill " + skillCode + ": " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteSkill(String instanceCode, String skillCode) {
        Path target = resolveSkillPath(instanceCode, skillCode);
        try {
            Path skillDir = target.getParent();
            FileDirUtil.deleteDir(skillDir.toFile());
            log.info("Skill deleted: instanceCode={} skillCode={}", instanceCode, skillCode);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete skill: instanceCode={} skillCode={}",
                    instanceCode, skillCode, e);
            return false;
        }
    }

    @Override
    public boolean notifyAgent(String callbackUrl, List<String> skillCodes) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            return false;
        }
        String url = callbackUrl.replaceAll("/$", "") + "/api/skill/push";
        try {
            // Java 8 兼容: Map.of 是 Java 9+ API
            java.util.Map<String, Object> bodyMap = new java.util.LinkedHashMap<>();
            bodyMap.put("skillCodes", skillCodes);
            bodyMap.put("timestamp", System.currentTimeMillis());
            String body = JsonUtil.toJsonExcludingNulls(bodyMap);
            RequestBody reqBody = RequestBody.create(body, JSON);
            Request request = new Request.Builder().url(url).post(reqBody).build();
            try (Response response = httpClient.newCall(request).execute()) {
                boolean ok = response.isSuccessful();
                if (!ok) {
                    log.warn("notifyAgent non-2xx: {} - {} skillCodes={}",
                            response.code(), url, skillCodes);
                }
                return ok;
            }
        } catch (IOException e) {
            log.warn("notifyAgent failed: url={} skillCodes={}", url, skillCodes, e);
            return false;
        }
    }

    @Override
    public boolean healthCheck(String callbackUrl) {
        if (callbackUrl == null || callbackUrl.isEmpty()) {
            return false;
        }
        try {
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection)
                    new java.net.URL(callbackUrl.replaceAll("/$", "") + "/health").openConnection();
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int code = conn.getResponseCode();
            return code > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
