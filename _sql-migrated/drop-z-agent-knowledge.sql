-- 2026-10-18: z-agent-knowledge 整 module 已删 (RAG 归 z-kb 独立仓 io.github.yuku123:z-kb-*)
-- 本脚本清理由该 module 独占的五张表。z_knowledge_base / _document / _embedding 曾被
-- z-agent-center 的 HybridRetrievalServiceImpl 引用，该 impl 的 @Service 早已禁用且随包删除；
-- z_knowledge_yuque_config / _access_log 无任何 Java 侧引用。
--
-- 执行前请先自查，确认无其他系统在读这些表：
--   SELECT table_name, table_rows FROM information_schema.tables
--    WHERE table_schema = DATABASE() AND table_name LIKE 'z_knowledge%';
--
-- 本脚本不含数据备份。若库中有需要保留的数据，请先 mysqldump。

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS `z_knowledge_access_log`;
DROP TABLE IF EXISTS `z_knowledge_yuque_config`;
DROP TABLE IF EXISTS `z_knowledge_embedding`;
DROP TABLE IF EXISTS `z_knowledge_document`;
DROP TABLE IF EXISTS `z_knowledge_base`;

SET FOREIGN_KEY_CHECKS = 1;
