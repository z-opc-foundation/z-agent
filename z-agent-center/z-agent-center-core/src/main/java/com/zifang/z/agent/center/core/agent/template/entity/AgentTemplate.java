package com.zifang.z.agent.center.core.agent.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zifang.z.meta.api.annotation.BizField;
import com.zifang.z.meta.api.annotation.BizModel;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * FEATURE013 T3.3: Agent 模板市场实体
 */
@BizModel(name = "FEATURE013 T3.3: Agent 模板市场实体", module = "z-agent", description = "FEATURE013 T3.3: Agent 模板市场实体")
@TableName("z_agent_template")
public class AgentTemplate {

    @TableId(type = IdType.AUTO)
    @BizField(name = "id", type = BizField.FieldType.INTEGER)
    private Long id;
    @BizField(name = "templateCode", type = BizField.FieldType.STRING)
    private String templateCode;
    @BizField(name = "name", type = BizField.FieldType.STRING)
    private String name;
    @BizField(name = "description", type = BizField.FieldType.STRING)
    private String description;
    @BizField(name = "category", type = BizField.FieldType.STRING)
    private String category;
    @BizField(name = "tags", type = BizField.FieldType.STRING)
    private String tags;
    @BizField(name = "icon", type = BizField.FieldType.STRING)
    private String icon;
    @BizField(name = "coverImage", type = BizField.FieldType.STRING)
    private String coverImage;
    @BizField(name = "configSnapshot", type = BizField.FieldType.STRING)
    private String configSnapshot;
    @BizField(name = "requiredPlugins", type = BizField.FieldType.STRING)
    private String requiredPlugins;
    @BizField(name = "requiredSkills", type = BizField.FieldType.STRING)
    private String requiredSkills;
    @BizField(name = "requiredKnowledge", type = BizField.FieldType.STRING)
    private String requiredKnowledge;
    /**
     * FEATURE013 T3.3: 2026-09-04 DB 迁移补齐 author/author_name 等列后,
     * 移除多余的 @TableField(exist=false). 仅保留 download_count / status
     * (DB 中不存在这两列).
     */
    @BizField(name = "author", type = BizField.FieldType.STRING)
    private String author;
    @BizField(name = "authorName", type = BizField.FieldType.STRING)
    private String authorName;
    @BizField(name = "homepage", type = BizField.FieldType.STRING)
    private String homepage;
    @TableField(exist = false)
    @BizField(name = "downloadCount", type = BizField.FieldType.INTEGER)
    private Integer downloadCount;
    @BizField(name = "viewCount", type = BizField.FieldType.INTEGER)
    private Integer viewCount;
    @BizField(name = "rating", type = BizField.FieldType.DECIMAL)
    private BigDecimal rating;
    @BizField(name = "ratingCount", type = BizField.FieldType.INTEGER)
    private Integer ratingCount;
    @BizField(name = "isOfficial", type = BizField.FieldType.INTEGER)
    private Integer isOfficial;
    @TableField(exist = false)
    @BizField(name = "status", type = BizField.FieldType.STRING)
    private String status;
    @BizField(name = "tenantCode", type = BizField.FieldType.STRING)
    private String tenantCode;
    @BizField(name = "gmtCreate", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtCreate;
    @BizField(name = "gmtModified", type = BizField.FieldType.DATETIME)
    private LocalDateTime gmtModified;
    @BizField(name = "isDeleted", type = BizField.FieldType.INTEGER)
    private Integer isDeleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCoverImage() {
        return coverImage;
    }

    public void setCoverImage(String coverImage) {
        this.coverImage = coverImage;
    }

    public String getConfigSnapshot() {
        return configSnapshot;
    }

    public void setConfigSnapshot(String configSnapshot) {
        this.configSnapshot = configSnapshot;
    }

    public String getRequiredPlugins() {
        return requiredPlugins;
    }

    public void setRequiredPlugins(String requiredPlugins) {
        this.requiredPlugins = requiredPlugins;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public void setRequiredSkills(String requiredSkills) {
        this.requiredSkills = requiredSkills;
    }

    public String getRequiredKnowledge() {
        return requiredKnowledge;
    }

    public void setRequiredKnowledge(String requiredKnowledge) {
        this.requiredKnowledge = requiredKnowledge;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getHomepage() {
        return homepage;
    }

    public void setHomepage(String homepage) {
        this.homepage = homepage;
    }

    public Integer getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Integer downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Integer getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Integer isOfficial) {
        this.isOfficial = isOfficial;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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



