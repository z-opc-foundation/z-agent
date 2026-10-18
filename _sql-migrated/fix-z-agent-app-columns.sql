-- =============================================
-- 修复 z_agent_app 表：补充 Entity 中定义但数据库缺失的列
-- 执行方式: mysql -h <host> -u <user> -p oc < fix-z-agent-app-columns.sql
-- =============================================

-- 基础字段
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `icon_url` VARCHAR (512) DEFAULT NULL COMMENT '应用图标URL' AFTER `description`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `prompt` TEXT DEFAULT NULL COMMENT '系统提示词' AFTER `icon_url`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `model_name` VARCHAR (64) DEFAULT NULL COMMENT '默认模型名称' AFTER `prompt`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `model_provider` VARCHAR (32) DEFAULT NULL COMMENT '模型供应商编码' AFTER `model_name`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `tools` TEXT DEFAULT NULL COMMENT '工具配置(JSON)' AFTER `model_provider`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `knowledge_ids` TEXT DEFAULT NULL COMMENT '知识库ID列表' AFTER `tools`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `skill_codes` TEXT DEFAULT NULL COMMENT '技能编码列表' AFTER `knowledge_ids`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `variables` TEXT DEFAULT NULL COMMENT '变量配置(JSON)' AFTER `skill_codes`;

-- 分组与租户
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `group_code` VARCHAR (64) DEFAULT '' COMMENT '所属分组编码' AFTER `status`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `tenant_code` VARCHAR (64) DEFAULT 'default' COMMENT '租户编码' AFTER `group_code`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `creator` VARCHAR (64) DEFAULT NULL COMMENT '创建者' AFTER `tenant_code`;

-- 逻辑删除
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除' AFTER `gmt_modified`;

-- 应用类型与模型参数
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `agent_mode` VARCHAR (16) DEFAULT 'chat' COMMENT '应用类型: chat/agent/workflow' AFTER `is_deleted`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `temperature` FLOAT DEFAULT NULL COMMENT '模型温度' AFTER `agent_mode`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `top_p` FLOAT DEFAULT NULL COMMENT 'Top-P采样' AFTER `temperature`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `max_tokens` INT DEFAULT NULL COMMENT '最大输出token' AFTER `top_p`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `response_format` VARCHAR (32) DEFAULT NULL COMMENT '输出格式: text/json' AFTER `max_tokens`;

-- 记忆配置
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `history_window_size` INT DEFAULT NULL COMMENT '历史窗口大小' AFTER `response_format`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `enable_long_memory` TINYINT DEFAULT 0 COMMENT '是否启用长期记忆' AFTER `history_window_size`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `memory_prompt` TEXT DEFAULT NULL COMMENT '记忆提示词' AFTER `enable_long_memory`;

-- 分享配置
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `share_enabled` TINYINT DEFAULT 0 COMMENT '是否开启分享' AFTER `memory_prompt`;
ALTER TABLE `z_agent_app`
    ADD COLUMN IF NOT EXISTS `share_code` VARCHAR (64) DEFAULT NULL COMMENT '分享码' AFTER `share_enabled`;

-- 确保 z_agent_app_group 表存在
CREATE TABLE IF NOT EXISTS `z_agent_app_group`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `group_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '分组编码（唯一标识）',
    `parent_code` VARCHAR
(
    64
) DEFAULT '' COMMENT '父分组编码，空字符串表示顶级',
    `group_name` VARCHAR
(
    128
) NOT NULL COMMENT '分组名称',
    `sort_order` INT DEFAULT 0 COMMENT '排序号',
    `tenant_code` VARCHAR
(
    64
) DEFAULT 'default' COMMENT '租户编码',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_group_code`
(
    `group_code`
),
    KEY `idx_parent_code`
(
    `parent_code`
),
    KEY `idx_tenant`
(
    `tenant_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent应用分组表';

-- 初始化默认分组（忽略重复）
INSERT
IGNORE INTO `z_agent_app_group` (`group_code`, `parent_code`, `group_name`, `sort_order`, `tenant_code`)
VALUES ('default', '', '默认分组', 0, 'default');
