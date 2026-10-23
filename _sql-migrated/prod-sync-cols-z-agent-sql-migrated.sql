-- =====================================================================
-- 结构对齐回写 (缺列) — 生成日 2026-10-24
-- 来源: 生产 oc 只读 mysqldump --no-data
-- 口径: 只加不改; 只收「两种独立解析实现都判缺」的列, 歧义区不动。
-- 用法: mysql <库名> < 本文件 (脚本用 DELIMITER, 走 mysql 客户端执行)。
-- 位置: 与本表 CREATE 语句同目录 (z-agent/_sql-migrated)
-- =====================================================================

-- 逐列补齐: 只在列不存在时才 ALTER ⇒ 可重复执行。
-- MySQL **不支持** ALTER TABLE ... ADD COLUMN IF NOT EXISTS (那是 MariaDB 语法),
-- 所以幂等靠 information_schema 判定 + 动态 SQL, 不靠语法糖。
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-agent-sql-migrated`;
DELIMITER $$
CREATE PROCEDURE `zopc_sync_prod_cols_z-agent-sql-migrated`()
BEGIN
  DECLARE v_t VARCHAR(64);
  DECLARE v_c VARCHAR(64);
  DECLARE v_d TEXT;
  DECLARE cur CURSOR FOR SELECT tbl, col, defn FROM `_zopc_prod_sync_cols_z-agent-sql-migrated`;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET v_t = NULL;
  OPEN cur;
  lp: LOOP
    FETCH cur INTO v_t, v_c, v_d;
    IF v_t IS NULL THEN LEAVE lp; END IF;
    IF EXISTS (
         SELECT 1 FROM information_schema.TABLES
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t)
       AND NOT EXISTS (
         SELECT 1 FROM information_schema.COLUMNS
          WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = v_t AND COLUMN_NAME = v_c) THEN
      SET @ddl = CONCAT('ALTER TABLE `', v_t, '` ADD COLUMN `', v_c, '` ', v_d);
      PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
    END IF;
  END LOOP;
  CLOSE cur;
END$$
DELIMITER ;

DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-agent-sql-migrated`;
CREATE TEMPORARY TABLE `_zopc_prod_sync_cols_z-agent-sql-migrated` (tbl VARCHAR(64), col VARCHAR(64), defn TEXT);
INSERT INTO `_zopc_prod_sync_cols_z-agent-sql-migrated` (tbl, col, defn) VALUES
  ('z_agent_app', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_app_group', 'created_time', 'datetime DEFAULT NULL COMMENT ''创建时间'''),
  ('z_agent_app_group', 'dept_code', 'varchar(64) DEFAULT '''' COMMENT ''部门编码'''),
  ('z_agent_app_group', 'description', 'varchar(512) DEFAULT '''' COMMENT ''描述'''),
  ('z_agent_app_group', 'domain_code', 'varchar(64) DEFAULT '''' COMMENT ''域编码'''),
  ('z_agent_app_group', 'ext_config', 'varchar(512) DEFAULT '''' COMMENT ''扩展配置'''),
  ('z_agent_app_group', 'is_deleted', 'int DEFAULT ''0'' COMMENT ''是否删除'''),
  ('z_agent_app_group', 'org_code', 'varchar(64) DEFAULT '''' COMMENT ''组织编码'''),
  ('z_agent_app_group', 'status', 'int DEFAULT ''1'' COMMENT ''状态'''),
  ('z_agent_app_group', 'updated_time', 'datetime DEFAULT NULL COMMENT ''更新时间'''),
  ('z_agent_conversation', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_conversation', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_instance', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_instance', 'tenant_code', 'varchar(64) DEFAULT ''default'''),
  ('z_agent_share', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_share', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_skill_binding', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_skill_sync_log', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team_conversation', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team_conversation', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team_member', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team_member', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team_message', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_team_message', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_tool_template', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_agent_tool_template', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_credential', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_credential', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_model', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_model', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_provider', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_provider', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_usage_conversation', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_usage_conversation', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_usage_daily', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_usage_daily', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_usage_record', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_llm_usage_record', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_team_message_reaction', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_team_message_reaction', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_team_participant', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_team_participant', 'tenant_code', 'varchar(64) DEFAULT NULL'),
  ('z_team_presence', 'domain_code', 'varchar(64) DEFAULT NULL'),
  ('z_team_presence', 'tenant_code', 'varchar(64) DEFAULT NULL');

CALL `zopc_sync_prod_cols_z-agent-sql-migrated`();
DROP PROCEDURE IF EXISTS `zopc_sync_prod_cols_z-agent-sql-migrated`;
DROP TEMPORARY TABLE IF EXISTS `_zopc_prod_sync_cols_z-agent-sql-migrated`;
