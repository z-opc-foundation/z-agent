-- =============================================
-- FEATURE016: 用户面板 · 工作项 (Workspace) + 工作空间文件树
-- 数据库: oc
-- 说明:
--   - z_workspace: 工作空间元数据 (一个工作空间 = 一个本地目录路径)
--   - 工作空间可以关联到一个会话 (z_agent_team_conversation.workspace_code)
--   - 工作项 (Conv) 之下可挂工作空间 (Workspace), 工作空间下挂本地文件树
--   - 文件树本身不持久化, 每次请求实时扫描 localPath
-- =============================================

-- ---------------------------------------------------
-- 1. 工作空间表 z_workspace
-- ---------------------------------------------------
CREATE TABLE IF NOT EXISTS `z_workspace`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `workspace_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '工作空间编码 (唯一标识)',
    `workspace_name` VARCHAR
(
    128
) NOT NULL COMMENT '工作空间名称',
    `description` VARCHAR
(
    512
) DEFAULT NULL COMMENT '描述',
    `local_path` VARCHAR
(
    1024
) NOT NULL COMMENT '本地路径 (服务器侧的绝对路径)',
    `owner_id` VARCHAR
(
    64
) DEFAULT NULL COMMENT '所有者用户ID',
    `owner_name` VARCHAR
(
    128
) DEFAULT NULL COMMENT '所有者用户名',
    `team_code` VARCHAR
(
    64
) DEFAULT NULL COMMENT '所属 Team 编码 (可空, 单兵工作空间)',
    `status` VARCHAR
(
    16
) DEFAULT 'ENABLE' COMMENT '状态: ENABLE/DISABLE',
    `creator` VARCHAR
(
    64
) DEFAULT NULL COMMENT '创建者',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_workspace_code`
(
    `workspace_code`
),
    KEY `idx_owner_id`
(
    `owner_id`
),
    KEY `idx_team_code`
(
    `team_code`
),
    KEY `idx_status`
(
    `status`
)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='用户面板 · 工作空间表 (映射本地目录)';

-- ---------------------------------------------------
-- 2. z_agent_team_conversation 加 workspace_code 列 (幂等 ALTER)
-- ---------------------------------------------------
SET
@col_exists = (SELECT COUNT(*)
                   FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'z_agent_team_conversation'
                     AND COLUMN_NAME = 'workspace_code');
SET
@sql = IF(@col_exists = 0,
              'ALTER TABLE `z_agent_team_conversation` ADD COLUMN `workspace_code` VARCHAR(64) DEFAULT NULL COMMENT ''关联的工作空间编码'' AFTER `team_code`',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 索引 (幂等)
SET
@idx_exists = (SELECT COUNT(*)
                   FROM INFORMATION_SCHEMA.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'z_agent_team_conversation'
                     AND INDEX_NAME = 'idx_workspace_code');
SET
@sql = IF(@idx_exists = 0,
              'ALTER TABLE `z_agent_team_conversation` ADD KEY `idx_workspace_code` (`workspace_code`)',
              'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;