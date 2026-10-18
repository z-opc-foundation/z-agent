-- =============================================
-- z-agent-team 数据库初始化脚本
-- 数据库: biz_service
-- 说明: 群组 = 主 Agent (z-one-company 首席助手) + 多个子 Agent (各业务模块)
--       一次会话 = 一个「事项」, 由 CEO 创建, 主 Agent 接待, 必要时调度子 Agent
-- v3.0 IM-first: 加 z_team_participant / z_team_message_reaction / z_team_presence
--                z_agent_team_message 加 reply_to_msg_code / mention_codes
--                z_agent_team_conversation 加 unread_count
-- =============================================

-- ---------------------------------------------------
-- 幂等 ALTER (用于已部署的数据库升级, 所有语句都做存在性判断)
-- ---------------------------------------------------
-- 会话表加 unread_count
SET
@col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_agent_team_conversation' AND COLUMN_NAME = 'unread_count');
SET
@sql = IF(@col_exists = 0, 'ALTER TABLE `z_agent_team_conversation` ADD COLUMN `unread_count` INT DEFAULT 0 COMMENT ''CEO 未读消息数'' AFTER `message_count`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 消息表加 reply_to_msg_code
SET
@col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_agent_team_message' AND COLUMN_NAME = 'reply_to_msg_code');
SET
@sql = IF(@col_exists = 0, 'ALTER TABLE `z_agent_team_message` ADD COLUMN `reply_to_msg_code` VARCHAR(64) COMMENT ''回复引用消息编码'' AFTER `parent_msg_code`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 消息表加 mention_codes
SET
@col_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_agent_team_message' AND COLUMN_NAME = 'mention_codes');
SET
@sql = IF(@col_exists = 0, 'ALTER TABLE `z_agent_team_message` ADD COLUMN `mention_codes` VARCHAR(2048) COMMENT ''@-mention 参与者编码 JSON'' AFTER `content`', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 消息表加 reply_to 索引
SET
@idx_exists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'z_agent_team_message' AND INDEX_NAME = 'idx_reply_to');
SET
@sql = IF(@idx_exists = 0, 'ALTER TABLE `z_agent_team_message` ADD KEY `idx_reply_to` (`reply_to_msg_code`)', 'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------
-- Team 表
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_agent_team`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `team_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '群组编码(唯一标识)',
    `team_name` VARCHAR
(
    128
) NOT NULL COMMENT '群组名称',
    `description` VARCHAR
(
    512
) COMMENT '群组描述',
    `supervisor_app_code` VARCHAR
(
    64
) NOT NULL COMMENT '主 Agent appCode (z_agent_app.app_code)',
    `supervisor_app_name` VARCHAR
(
    128
) COMMENT '主 Agent 名称(冗余, 方便展示)',
    `status` VARCHAR
(
    16
) DEFAULT 'ENABLE' COMMENT '状态: ENABLE/DISABLE',
    `creator` VARCHAR
(
    64
) COMMENT '创建者',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_team_code`
(
    `team_code`
),
    KEY `idx_status`
(
    `status`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 群组表';


-- ---------------------------------------------------
-- Team 成员表 (主 Agent + 子 Agent 们)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_agent_team_member`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `member_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '成员编码(唯一)',
    `team_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属群组编码',
    `app_code` VARCHAR
(
    64
) NOT NULL COMMENT 'Agent appCode (z_agent_app.app_code)',
    `app_name` VARCHAR
(
    128
) COMMENT 'Agent 名称(冗余)',
    `app_icon` VARCHAR
(
    256
) COMMENT 'Agent 图标URL(冗余)',
    `role` VARCHAR
(
    16
) NOT NULL DEFAULT 'WORKER' COMMENT '角色: SUPERVISOR(主Agent)/WORKER(子Agent)',
    `capabilities` TEXT COMMENT '能力描述 JSON: [{"name":"...", "desc":"..."}, ...]',
    `priority` INT DEFAULT 0 COMMENT '调度优先级, 大者优',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_member_code`
(
    `member_code`
),
    KEY `idx_team_code`
(
    `team_code`
),
    KEY `idx_app_code`
(
    `app_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 群组成员表';


-- ---------------------------------------------------
-- Team 会话表 (= CEO 的「事项」)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_agent_team_conversation`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `conv_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '会话编码(唯一)',
    `team_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属群组编码',
    `user_id` VARCHAR
(
    64
) NOT NULL COMMENT '所属用户ID (z-ctc)',
    `user_name` VARCHAR
(
    128
) COMMENT '用户名称(冗余)',
    `title` VARCHAR
(
    256
) NOT NULL COMMENT '事项标题 (= 会话标题)',
    `summary` VARCHAR
(
    1024
) COMMENT '摘要(首条消息前200字)',
    `status` VARCHAR
(
    16
) DEFAULT 'OPEN' COMMENT '状态: OPEN/DONE/ARCHIVED',
    `last_msg_preview` VARCHAR
(
    256
) COMMENT '最后一条消息预览',
    `last_msg_time` DATETIME COMMENT '最后一条消息时间',
    `message_count` INT DEFAULT 0 COMMENT '消息总数',
    `unread_count` INT DEFAULT 0 COMMENT 'CEO 未读消息数 (重启/分布式恢复用)',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_conv_code`
(
    `conv_code`
),
    KEY `idx_team_user`
(
    `team_code`,
    `user_id`
),
    KEY `idx_last_msg_time`
(
    `last_msg_time`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Team 会话表(事项)';


-- ---------------------------------------------------
-- Team 参与者表 (IM 的群成员: 多个 user + 多个 agent)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_team_participant`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `conv_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '所属会话编码',
    `participant_code` VARCHAR
(
    64
) NOT NULL COMMENT '参与者编码 (userId 或 appCode)',
    `participant_type` VARCHAR
(
    16
) NOT NULL COMMENT '类型: USER / AGENT',
    `display_name` VARCHAR
(
    128
) NOT NULL COMMENT '显示名 (冗余, 改名时刷新)',
    `avatar_url` VARCHAR
(
    512
) COMMENT '头像 URL (冗余)',
    `role` VARCHAR
(
    16
) NOT NULL DEFAULT 'MEMBER' COMMENT '角色: OWNER / SUPERVISOR / MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_seen_at` DATETIME COMMENT '最后心跳时间 (presence 用)',
    `is_muted` TINYINT DEFAULT 0 COMMENT '是否免打扰',
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_conv_participant`
(
    `conv_code`,
    `participant_code`
),
    KEY `idx_participant_code`
(
    `participant_code`
),
    KEY `idx_last_seen`
(
    `last_seen_at`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Team 参与者表 (IM 群成员)';


-- ---------------------------------------------------
-- Team 消息反应表 (emoji 反应 / 已读回执 / @-mention)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_team_message_reaction`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `msg_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '目标消息编码',
    `conv_code` VARCHAR
(
    64
) NOT NULL COMMENT '会话编码 (冗余, 加速查询)',
    `participant_code` VARCHAR
(
    64
) NOT NULL COMMENT '反应者编码 (userId 或 appCode)',
    `reaction_type` VARCHAR
(
    16
) NOT NULL COMMENT '类型: EMOJI / READ',
    `emoji` VARCHAR
(
    16
) COMMENT 'emoji 字符 (类型=EMOJI 时使用)',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_msg_user_emoji`
(
    `msg_code`,
    `participant_code`,
    `reaction_type`,
    `emoji`
),
    KEY `idx_msg`
(
    `msg_code`
),
    KEY `idx_conv_user`
(
    `conv_code`,
    `participant_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息反应 (emoji + 已读)';


-- ---------------------------------------------------
-- Team 在线状态表 (agent / user 心跳)
-- 心跳频率: 30s 一次, 超过 90s 没心跳视为离线
-- 用于 IM 头像旁边的"绿点" / "灰点"
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_team_presence`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `participant_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '参与者编码',
    `participant_type` VARCHAR
(
    16
) NOT NULL COMMENT '类型: USER / AGENT',
    `display_name` VARCHAR
(
    128
) COMMENT '显示名 (冗余)',
    `status` VARCHAR
(
    16
) NOT NULL DEFAULT 'OFFLINE' COMMENT '状态: ONLINE / AWAY / OFFLINE',
    `last_heartbeat_at` DATETIME COMMENT '最后心跳时间',
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_participant`
(
    `participant_code`
),
    KEY `idx_status`
(
    `status`
),
    KEY `idx_heartbeat`
(
    `last_heartbeat_at`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='在线状态 (心跳跟踪)';


-- ---------------------------------------------------
-- Team 消息表 (IM 消息持久化)
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_agent_team_message`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `msg_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '消息编码(唯一)',
    `conv_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属会话编码',
    `parent_msg_code` VARCHAR
(
    64
) COMMENT '父消息编码(用于消息分支/引用)',
    `reply_to_msg_code` VARCHAR
(
    64
) COMMENT '回复引用消息编码 (IM 引用回复)',
    `sender_type` VARCHAR
(
    16
) NOT NULL COMMENT '发送者类型: USER/AGENT/SYSTEM',
    `sender_code` VARCHAR
(
    64
) NOT NULL COMMENT '发送者编码: userId 或 appCode',
    `sender_name` VARCHAR
(
    128
) COMMENT '发送者名称(冗余)',
    `message_type` VARCHAR
(
    32
) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型: TEXT/THINKING/DELEGATION/DELEGATION_RESULT/TOOL_CALL/TOOL_RESULT/SYSTEM/ERROR',
    `content` TEXT COMMENT '消息正文',
    `mention_codes` VARCHAR
(
    2048
) COMMENT '@-mention 的参与者编码列表 JSON: ["ceo", "z-config", ...]',
    `metadata` JSON COMMENT '附加元数据 JSON (如 toolName/toolArgs/toolResult, delegationMembers...)',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_msg_code`
(
    `msg_code`
),
    KEY `idx_conv_gmt`
(
    `conv_code`,
    `gmt_create`
),
    KEY `idx_reply_to`
(
    `reply_to_msg_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Team 消息表';


-- ---------------------------------------------------
-- 初始化默认主 Agent (z-one-company 首席助手)
-- 启动时 z-agent-team 的 InitService 会幂等检查,
-- 这里提供 SQL 兜底, 让数据库初始化后即可使用
-- ---------------------------------------------------
INSERT
IGNORE INTO `z_agent_app` (
    `app_code`, `app_name`, `description`, `prompt`, `status`,
    `agent_mode`, `temperature`, `top_p`, `max_tokens`, `history_window_size`,
    `enable_long_memory`, `creator`
) VALUES (
    'z-one-company-supervisor',
    'Z-One Company 首席助手',
    'z-one-company 集团首席 AI 助手, 负责接待 CEO 并协调全公司 Agent 资源',
    '你是 z-one-company 集团的首席 AI 助手, 名字叫 "首席小 Z"。你的职责是:\n1. 接待公司 CEO, 理解 CEO 提出的事项或需求\n2. 尝试直接回答或解决问题\n3. 当遇到超出你能力范围的任务时, 调用 dispatch_to_agent 工具, 把任务分派给合适的子 Agent\n4. 汇总子 Agent 的结果, 用简洁清晰的方式回复 CEO\n\n你可以调度的子 Agent 名单 (动态变化, 以工具参数为准):\n{AGENT_REGISTRY}\n\n回复风格: 简洁、清晰、有温度, 像一位专业的高级助理。',
    'ENABLE',
    'agent',
    0.7,
    0.9,
    4096,
    30,
    1,
    'system'
);
