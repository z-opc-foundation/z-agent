-- ============================================================================
-- FEATURE014 Wishlist 3-6 — z-bot 自我迭代基础设施
-- ============================================================================
-- 4 张新表：
--   z_bot_memory         — 长期记忆条目（跨会话持久化）
--   z_bot_feedback       — 用户反馈（thumbs up/down + comment）
--   z_bot_hook_event     — Hook 事件审计（每次 hook fire 一行）
--   z_bot_skill_promotion — 自动晋升记录（中心把 hot skill 写入 z_agent_app.config）
-- ============================================================================

-- 1. 长期记忆
CREATE TABLE IF NOT EXISTS `z_bot_memory`
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
) NOT NULL COMMENT '所属 Agent 实例',
    `user_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '用户 ID（多用户隔离）',
    `role` VARCHAR
(
    16
) NOT NULL COMMENT 'user / assistant / system / summary',
    `content` TEXT NOT NULL COMMENT '记忆内容（短文本 / JSON 摘要）',
    `importance` DECIMAL
(
    3,
    2
) DEFAULT 0.50 COMMENT '重要度 0-1（summary 一般 1.0）',
    `source` VARCHAR
(
    32
) DEFAULT 'chat' COMMENT '来源: chat / feedback / consolidation / manual',
    `tags` VARCHAR
(
    256
) DEFAULT NULL COMMENT '逗号分隔标签（便于检索）',
    `tenant_code` VARCHAR
(
    32
) DEFAULT 'default' COMMENT '所属租户',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_expire` DATETIME DEFAULT NULL COMMENT '过期时间（NULL = 永不过期）',
    `is_deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_instance_user`
(
    `instance_code`,
    `user_id`
),
    KEY `idx_gmt_create`
(
    `gmt_create`
),
    KEY `idx_importance`
(
    `importance`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = 'z-bot 长期记忆（FEATURE014 L2）';

-- 2. 用户反馈
CREATE TABLE IF NOT EXISTS `z_bot_feedback`
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
) NOT NULL COMMENT '所属 Agent 实例',
    `trace_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '关联的对话 trace_id',
    `user_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '反馈用户 ID',
    `rating` TINYINT NOT NULL COMMENT '1=点赞, -1=点踩, 0=中性',
    `comment` VARCHAR
(
    512
) DEFAULT NULL COMMENT '反馈备注',
    `app_code` VARCHAR
(
    64
) DEFAULT NULL COMMENT '触发反馈的应用',
    `skill_codes` VARCHAR
(
    512
) DEFAULT NULL COMMENT '当时使用的 skill（逗号分隔）',
    `tenant_code` VARCHAR
(
    32
) DEFAULT 'default' COMMENT '所属租户',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_instance`
(
    `instance_code`
),
    KEY `idx_trace`
(
    `trace_id`
),
    KEY `idx_rating`
(
    `rating`
),
    KEY `idx_gmt`
(
    `gmt_create`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = 'z-bot 用户反馈（FEATURE014 L2 — 评分机制）';

-- 3. Hook 事件审计
CREATE TABLE IF NOT EXISTS `z_bot_hook_event`
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
) NOT NULL COMMENT '所属 Agent 实例',
    `hook_name` VARCHAR
(
    64
) NOT NULL COMMENT 'Hook 实现类名（短名）',
    `event_type` VARCHAR
(
    32
) NOT NULL COMMENT 'preChat / postChat / preTool / postTool / onError',
    `payload` TEXT DEFAULT NULL COMMENT '事件 payload（JSON）',
    `result` VARCHAR
(
    16
) DEFAULT NULL COMMENT 'SUCCESS / FAIL / SKIPPED',
    `error_msg` VARCHAR
(
    512
) DEFAULT NULL,
    `duration_ms` BIGINT DEFAULT NULL,
    `tenant_code` VARCHAR
(
    32
) DEFAULT 'default',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_instance_event`
(
    `instance_code`,
    `event_type`
),
    KEY `idx_hook`
(
    `hook_name`
),
    KEY `idx_gmt`
(
    `gmt_create`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = 'z-bot Hook 事件审计（FEATURE014 L3）';

-- 4. Skill 自动晋升记录
CREATE TABLE IF NOT EXISTS `z_bot_skill_promotion`
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
) NOT NULL COMMENT '所属 Agent 实例',
    `skill_code` VARCHAR
(
    64
) NOT NULL COMMENT '被晋升的 skill',
    `app_code` VARCHAR
(
    64
) NOT NULL COMMENT '被写入的应用',
    `promotion_kind` VARCHAR
(
    16
) NOT NULL COMMENT 'add / remove',
    `reason` VARCHAR
(
    256
) DEFAULT NULL COMMENT '晋升理由（高赞数 / 反馈触发）',
    `feedback_score` INT DEFAULT NULL COMMENT '触发晋升时的累计反馈分数',
    `tenant_code` VARCHAR
(
    32
) DEFAULT 'default',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    KEY `idx_instance_skill`
(
    `instance_code`,
    `skill_code`
),
    KEY `idx_app`
(
    `app_code`
),
    KEY `idx_gmt`
(
    `gmt_create`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COMMENT = 'z-bot 自动晋升 skill 写入 z_agent_app.config 的记录';
