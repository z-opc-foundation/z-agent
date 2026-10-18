package com.zifang.z.agent.center.core.agent.plugin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * FEATURE013 T2.1: Agent Plugin 可执行插件实体
 *
 * <p>Plugin 与 Skill 平行：
 * <ul>
 *     <li>Skill = Markdown 文档（人类阅读）</li>
 *     <li>Plugin = 真正可执行的工具（HTTP/BEAN/MCP 三种模式）</li>
 * </ul>
 */
@TableName("z_agent_plugin")
public class AgentPlugin {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 插件编码（唯一标识，Agent 引用时用）
     */
    private String pluginCode;

    /**
     * 插件名称
     */
    private String pluginName;

    /**
     * 描述
     */
    private String description;

    /**
     * 分类: TOOL/SEARCH/IMAGE/CODE/UTILITY
     */
    private String categoryCode;

    /**
     * 逗号分隔标签
     */
    private String tags;

    /**
     * 图标 URL
     */
    private String iconUrl;

    /**
     * 执行方式: HTTP / BEAN / MCP
     */
    private String executeType;

    /**
     * 执行配置 JSON（按 mode 字段不同）
     */
    private String executeConfig;

    /**
     * 入参 JSON Schema
     */
    private String inputSchema;

    /**
     * 出参 JSON Schema
     */
    private String outputSchema;

    /**
     * 鉴权方式: NONE / API_KEY / OAUTH / BEARER
     */
    private String authType;

    /**
     * 鉴权配置 JSON
     */
    private String authConfig;

    /**
     * 测试用例 JSON 数组
     */
    private String testCases;

    /**
     * 单次执行超时（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 是否内置插件（0=用户创建 / 1=系统预置）
     */
    private Integer builtin;

    /**
     * 状态: ACTIVE / DISABLED / DEPRECATED
     */
    private String status;

    /**
     * 作者
     */
    private String author;

    /**
     * 版本
     */
    private String version;

    /**
     * 被 Agent 引用次数
     */
    private Long downloadCount;

    /**
     * 租户
     */
    private String tenantCode;

    private LocalDateTime gmtCreate;
    private LocalDateTime gmtModified;
    private Integer isDeleted;

    // ===== getters/setters =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPluginCode() {
        return pluginCode;
    }

    public void setPluginCode(String pluginCode) {
        this.pluginCode = pluginCode;
    }

    public String getPluginName() {
        return pluginName;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getExecuteType() {
        return executeType;
    }

    public void setExecuteType(String executeType) {
        this.executeType = executeType;
    }

    public String getExecuteConfig() {
        return executeConfig;
    }

    public void setExecuteConfig(String executeConfig) {
        this.executeConfig = executeConfig;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getAuthConfig() {
        return authConfig;
    }

    public void setAuthConfig(String authConfig) {
        this.authConfig = authConfig;
    }

    public String getTestCases() {
        return testCases;
    }

    public void setTestCases(String testCases) {
        this.testCases = testCases;
    }

    public Integer getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Integer timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getBuiltin() {
        return builtin;
    }

    public void setBuiltin(Integer builtin) {
        this.builtin = builtin;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public String getTenantCode() {
        return tenantCode;
    }

    public void setTenantCode(String tenantCode) {
        this.tenantCode = tenantCode;
    }

    public LocalDateTime getGmtCreate() {
        return gmtCreate;
    }

    public void setGmtCreate(LocalDateTime gmtCreate) {
        this.gmtCreate = gmtCreate;
    }

    public LocalDateTime getGmtModified() {
        return gmtModified;
    }

    public void setGmtModified(LocalDateTime gmtModified) {
        this.gmtModified = gmtModified;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
