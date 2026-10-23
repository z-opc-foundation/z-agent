-- =====================================================================
-- 结构对齐回写 (缺表) — 生成日 2026-10-24
-- 来源: 生产 oc 只读 mysqldump --no-data
-- 口径: CREATE TABLE IF NOT EXISTS, 结构与生产一致;
--       外键已剥离 (被引用的表未必在本仓, 直接搬会建不出表);
--       归属按代码引用取证 (@TableName 实体声明 > 源码出现)。
-- 位置: z-agent/_sql-migrated
-- =====================================================================

CREATE TABLE IF NOT EXISTS `z_agent_workflow_instance`
(

  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `process_id` varchar(64) NOT NULL COMMENT '流程实例ID',
  `app_code` varchar(64) DEFAULT '' COMMENT '所属应用编码',
  `status` varchar(32) NOT NULL DEFAULT 'RUNNING' COMMENT '状态: RUNNING/COMPLETED/FAILED/PAUSED',
  `workflow_config` longtext COMMENT '工作流配置JSON',
  `snapshot_data` longtext COMMENT '执行快照JSON',
  `current_node_id` varchar(64) DEFAULT NULL COMMENT '当前执行节点',
  `error_message` text COMMENT '错误信息',
  `created_time` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_time` datetime DEFAULT NULL COMMENT '更新时间',
  `is_deleted` int DEFAULT '0' COMMENT '逻辑删除',
  `tenant_code` varchar(64) DEFAULT NULL,
  `domain_code` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `process_id` (`process_id`),
  KEY `idx_tenant_domain` (`tenant_code`,`domain_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
