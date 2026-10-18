package com.zifang.z.agent.engine.agent.plugin;

import com.zifang.util.json.JsonUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * 从 JSON 配置文件加载插件元信息。
 * 支持从 classpath 或本地文件加载 plugin.json。
 * <p>
 * plugin.json 格式：
 * {
 * "plugins": [
 * { "name": "my-plugin", "version": "1.0.0", "className": "com.example.MyAgentPlugin" }
 * ]
 * }
 */
public class JsonPluginLoader {
    @SuppressWarnings("unchecked")
    public List<PluginDescriptor> loadFromFile(File pluginConfigFile) throws Exception {
        Map<String, Object> config = JsonUtil.parseToMap(
                new String(java.nio.file.Files.readAllBytes(pluginConfigFile.toPath()), java.nio.charset.StandardCharsets.UTF_8));
        List<Map<String, Object>> pluginsConf = (List<Map<String, Object>>) config.get("plugins");
        List<PluginDescriptor> descriptors = new ArrayList<>();
        if (pluginsConf != null) {
            for (Map<String, Object> p : pluginsConf) {
                descriptors.add(new PluginDescriptor(
                        String.valueOf(p.get("name")),
                        String.valueOf(p.get("version")),
                        String.valueOf(p.get("className"))
                ));
            }
        }
        return descriptors;
    }

    /**
     * 通过反射实例化插件类。
     * 注意：平台层负责将 jar 放入 classpath 并调用此方法。
     * Engine 层只处理接口和配置。
     */
    @SuppressWarnings("unchecked")
    public AgentPlugin instantiate(PluginDescriptor descriptor) {
        try {
            Class<?> clazz = Class.forName(descriptor.className);
            return (AgentPlugin) clazz.newInstance();
        } catch (Exception e) {
            System.err.println("Failed to instantiate plugin: " + descriptor.name);
            return null;
        }
    }

    public static class PluginDescriptor {
        public final String name;
        public final String version;
        public final String className;

        public PluginDescriptor(String name, String version, String className) {
            this.name = name;
            this.version = version;
            this.className = className;
        }
    }
}