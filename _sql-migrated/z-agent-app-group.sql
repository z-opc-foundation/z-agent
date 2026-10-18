-- Agent应用分组表（树形结构，使用code关联）
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

-- 在 z_agent_app 表添加 group_code 字段
ALTER TABLE `z_agent_app`
    ADD COLUMN `group_code` VARCHAR(64) DEFAULT '' COMMENT '所属分组编码' AFTER `status`;

-- 初始化一条默认分组
INSERT INTO `z_agent_app_group` (`group_code`, `parent_code`, `group_name`, `sort_order`, `tenant_code`)
VALUES ('default', '', '默认分组', 0, 'default');