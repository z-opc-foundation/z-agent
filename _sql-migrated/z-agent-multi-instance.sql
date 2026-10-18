-- ============================================================================
-- FEATURE014 · 多 Agent 统一管理 — AgentInstance 扩展 + SkillBinding 表
-- ============================================================================
-- 兼容旧数据：所有 ALTER 字段都有 DEFAULT，旧实例 agent_type 默认 'ZBOT'，
-- 其他 callback_url / skill_root_path / auth_token / agent_version / capabilities
-- 允许为 NULL（不破坏现有 row）。
-- ============================================================================

-- 1. AgentInstance 扩展字段（FEATURE014 §5.1）
ALTER TABLE `z_agent_instance`
    ADD COLUMN `agent_type` VARCHAR(32) DEFAULT 'ZBOT' COMMENT 'Agent 类型: ZBOT / HERMES_CONTAINER / HERMES_LOCAL / CLAUDE_CODE / OPENCODE / CUSTOM'
        AFTER `run_mode`,
    ADD COLUMN `callback_url` VARCHAR(255) DEFAULT NULL
        COMMENT 'Agent 回调地址（外部 Agent 必填，ZBOT 留空）'
        AFTER `agent_type`,
    ADD COLUMN `skill_root_path` VARCHAR(255) DEFAULT NULL
        COMMENT 'Skill 落盘根路径（按 agent_type 自动推导，可覆盖）'
        AFTER `callback_url`,
    ADD COLUMN `auth_token` VARCHAR(128) DEFAULT NULL
        COMMENT 'Agent 访问 center 的认证 token'
        AFTER `skill_root_path`,
    ADD COLUMN `last_heartbeat` DATETIME DEFAULT NULL
        COMMENT 'Agent 最后心跳时间'
        AFTER `auth_token`,
    ADD COLUMN `agent_version` VARCHAR(32) DEFAULT NULL
        COMMENT 'Agent 版本号'
        AFTER `last_heartbeat`,
    ADD COLUMN `capabilities` TEXT DEFAULT NULL
        COMMENT 'Agent 能力描述（JSON 字符串）'
        AFTER `agent_version`;

-- 为 agent_type 加索引（按类型查询实例列表）
ALTER TABLE `z_agent_instance`
    ADD INDEX `idx_agent_type` (`agent_type`);

-- 2. Skill 绑定关系表（FEATURE014 §5.1）
CREATE TABLE IF NOT EXISTS `z_agent_skill_binding`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `instance_code`
    VARCHAR
(
    64
) NOT NULL COMMENT 'Agent 实例编码',
    `skill_code` VARCHAR
(
    64
) NOT NULL COMMENT '技能编码',
    `binding_type` VARCHAR
(
    16
) DEFAULT 'PUSH' COMMENT '绑定类型: PUSH 推送 / PULL 拉取',
    `status` VARCHAR
(
    16
) DEFAULT 'BOUND' COMMENT '状态: BOUND / UNBOUND / SYNCING / ERROR',
    `gmt_bind` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    `gmt_sync` DATETIME DEFAULT NULL COMMENT '最后同步时间',
    `error_msg` VARCHAR
(
    512
) DEFAULT NULL COMMENT '最近一次同步失败的错误信息',
    `tenant_code` VARCHAR
(
    32
) DEFAULT 'default' COMMENT '所属租户',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_instance_skill`
(
    `instance_code`,
    `skill_code`
),
    KEY `idx_instance`
(
    `instance_code`
),
    KEY `idx_skill`
(
    `skill_code`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = 'Agent 实例 ↔ Skill 绑定关系表（FEATURE014）';

-- 3. Skill 同步日志表（FEATURE014 §5.2）
CREATE TABLE IF NOT EXISTS `z_agent_skill_sync_log`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `instance_code`
    VARCHAR
(
    64
) NOT NULL COMMENT 'Agent 实例编码',
    `skill_code` VARCHAR
(
    64
) NOT NULL COMMENT '技能编码',
    `agent_type` VARCHAR
(
    32
) DEFAULT NULL COMMENT 'Agent 类型（冗余便于查询）',
    `action` VARCHAR
(
    16
) NOT NULL COMMENT '操作类型: INSTALL / UPGRADE / REMOVE',
    `result` VARCHAR
(
    16
) NOT NULL COMMENT '执行结果: SUCCESS / FAIL',
    `error_msg` VARCHAR
(
    512
) DEFAULT NULL COMMENT '失败时的错误信息',
    `duration_ms` BIGINT DEFAULT NULL COMMENT '同步耗时（毫秒）',
    `tenant_code` VARCHAR
(
    32
) DEFAULT 'default' COMMENT '所属租户',
    `gmt_sync` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_instance`
(
    `instance_code`
),
    KEY `idx_skill`
(
    `skill_code`
),
    KEY `idx_gmt_sync`
(
    `gmt_sync`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = 'Skill 同步执行日志（FEATURE014）';

-- 4. 旧数据兼容：把现有 CONTAINER 模式的实例统一标记为 HERMES_CONTAINER
UPDATE `z_agent_instance`
SET `agent_type` = 'HERMES_CONTAINER'
WHERE `run_mode` = 'CONTAINER'
  AND (`agent_type` IS NULL OR `agent_type` = '');

UPDATE `z_agent_instance`
SET `agent_type` = 'ZBOT'
WHERE `run_mode` = 'IN_PROCESS'
  AND (`agent_type` IS NULL OR `agent_type` = '');
