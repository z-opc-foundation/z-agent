-- z-agent-team 建表 (oc 库, 幂等)
-- 跳过 ALTER 语句 (新库直接 CREATE)

CREATE TABLE IF NOT EXISTS `z_agent_team`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `team_code`
    VARCHAR
(
    64
) NOT NULL,
    `team_name` VARCHAR
(
    128
) NOT NULL,
    `description` VARCHAR
(
    512
),
    `supervisor_app_code` VARCHAR
(
    64
) NOT NULL,
    `supervisor_app_name` VARCHAR
(
    128
),
    `status` VARCHAR
(
    16
) DEFAULT 'ENABLE',
    `creator` VARCHAR
(
    64
),
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `z_agent_team_member`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `member_code`
    VARCHAR
(
    64
) NOT NULL,
    `team_code` VARCHAR
(
    64
) NOT NULL,
    `app_code` VARCHAR
(
    64
) NOT NULL,
    `app_name` VARCHAR
(
    128
),
    `app_icon` VARCHAR
(
    256
),
    `role` VARCHAR
(
    16
) NOT NULL DEFAULT 'WORKER',
    `capabilities` TEXT,
    `priority` INT DEFAULT 0,
    `enabled` TINYINT DEFAULT 1,
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `z_agent_team_conversation`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `conv_code`
    VARCHAR
(
    64
) NOT NULL,
    `team_code` VARCHAR
(
    64
) NOT NULL,
    `user_id` VARCHAR
(
    64
) NOT NULL,
    `user_name` VARCHAR
(
    128
),
    `title` VARCHAR
(
    256
) NOT NULL,
    `summary` VARCHAR
(
    1024
),
    `status` VARCHAR
(
    16
) DEFAULT 'OPEN',
    `last_msg_preview` VARCHAR
(
    256
),
    `last_msg_time` DATETIME,
    `message_count` INT DEFAULT 0,
    `unread_count` INT DEFAULT 0,
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `z_team_participant`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `conv_code`
    VARCHAR
(
    64
) NOT NULL,
    `participant_code` VARCHAR
(
    64
) NOT NULL,
    `participant_type` VARCHAR
(
    16
) NOT NULL,
    `display_name` VARCHAR
(
    128
) NOT NULL,
    `avatar_url` VARCHAR
(
    512
),
    `role` VARCHAR
(
    16
) NOT NULL DEFAULT 'MEMBER',
    `joined_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `last_seen_at` DATETIME,
    `is_muted` TINYINT DEFAULT 0,
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `z_team_message_reaction`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `msg_code`
    VARCHAR
(
    64
) NOT NULL,
    `conv_code` VARCHAR
(
    64
) NOT NULL,
    `participant_code` VARCHAR
(
    64
) NOT NULL,
    `reaction_type` VARCHAR
(
    16
) NOT NULL,
    `emoji` VARCHAR
(
    16
),
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `z_team_presence`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `participant_code`
    VARCHAR
(
    64
) NOT NULL,
    `participant_type` VARCHAR
(
    16
) NOT NULL,
    `display_name` VARCHAR
(
    128
),
    `status` VARCHAR
(
    16
) NOT NULL DEFAULT 'OFFLINE',
    `last_heartbeat_at` DATETIME,
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `z_agent_team_message`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT,
    `msg_code`
    VARCHAR
(
    64
) NOT NULL,
    `conv_code` VARCHAR
(
    64
) NOT NULL,
    `parent_msg_code` VARCHAR
(
    64
),
    `reply_to_msg_code` VARCHAR
(
    64
),
    `sender_type` VARCHAR
(
    16
) NOT NULL,
    `sender_code` VARCHAR
(
    64
) NOT NULL,
    `sender_name` VARCHAR
(
    128
),
    `message_type` VARCHAR
(
    32
) NOT NULL DEFAULT 'TEXT',
    `content` TEXT,
    `mention_codes` VARCHAR
(
    2048
),
    `metadata` JSON,
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
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
