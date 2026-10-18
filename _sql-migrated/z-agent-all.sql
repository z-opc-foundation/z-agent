-- =============================================
-- z-agent 完整数据库初始化脚本
-- 数据库: biz_service
-- =============================================

-- ---------------------------------------------------
-- Part 1: z-agent-center (Agent 应用核心)
-- ---------------------------------------------------

-- Agent应用表
CREATE TABLE IF NOT EXISTS `z_agent_app`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `app_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '应用编码(唯一标识)',
    `app_name` VARCHAR
(
    128
) NOT NULL COMMENT '应用名称',
    `description` VARCHAR
(
    512
) COMMENT '应用描述',
    `model_code` VARCHAR
(
    64
) COMMENT '默认模型编码',
    `system_prompt` TEXT COMMENT '系统提示词',
    `status` VARCHAR
(
    16
) DEFAULT 'ENABLE' COMMENT '状态: ENABLE/DISABLE',
    `user_id` VARCHAR
(
    64
) COMMENT '创建者ID',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_app_code`
(
    `app_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent应用表';

-- Agent实例表
CREATE TABLE IF NOT EXISTS `z_agent_instance`
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
) NOT NULL COMMENT '实例编码',
    `app_code` VARCHAR
(
    64
) NOT NULL COMMENT '所属应用编码',
    `instance_name` VARCHAR
(
    128
) COMMENT '实例名称',
    `user_id` VARCHAR
(
    64
) COMMENT '用户ID',
    `owner_id` VARCHAR
(
    64
) COMMENT '所有者ID',
    `owner_name` VARCHAR
(
    128
) COMMENT '所有者名称',
    `status` VARCHAR
(
    16
) DEFAULT 'ACTIVE' COMMENT '状态',
    `config` JSON COMMENT '实例配置',
    `app_version` VARCHAR
(
    32
) COMMENT '应用版本',
    `visit_count` INT DEFAULT 0 COMMENT '访问次数',
    `last_visit_time` DATETIME COMMENT '最后访问时间',
    `run_mode` VARCHAR
(
    16
) DEFAULT 'IN_PROCESS' COMMENT '运行模式: IN_PROCESS/CONTAINER',
    `container_id` VARCHAR
(
    128
) COMMENT 'Docker容器ID(CONTAINER模式)',
    `container_port` INT COMMENT '容器端口(CONTAINER模式)',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0 COMMENT '是否删除',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_instance_code`
(
    `instance_code`
),
    KEY `idx_app_code`
(
    `app_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent实例表';

-- Agent会话表
CREATE TABLE IF NOT EXISTS `z_agent_conversation`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `conversation_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '会话编码',
    `instance_code` VARCHAR
(
    64
) NOT NULL COMMENT '实例编码',
    `user_id` VARCHAR
(
    64
) COMMENT '用户ID',
    `user_name` VARCHAR
(
    128
) COMMENT '用户名称',
    `title` VARCHAR
(
    256
) COMMENT '会话标题',
    `status` VARCHAR
(
    16
) DEFAULT 'ACTIVE' COMMENT '状态',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_conversation_code`
(
    `conversation_code`
),
    KEY `idx_instance_code`
(
    `instance_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

-- Agent分享表
CREATE TABLE IF NOT EXISTS `z_agent_share`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `share_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '分享码',
    `app_code` VARCHAR
(
    64
) NOT NULL COMMENT '应用编码',
    `conversation_code` VARCHAR
(
    64
) COMMENT '会话编码(可选)',
    `share_type` VARCHAR
(
    16
) DEFAULT 'APP' COMMENT '分享类型: APP/CONVERSATION',
    `password` VARCHAR
(
    128
) COMMENT '访问密码(可选)',
    `expire_time` DATETIME COMMENT '过期时间',
    `view_count` INT DEFAULT 0 COMMENT '浏览次数',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_share_code`
(
    `share_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent分享表';

-- ---------------------------------------------------
-- Part 2: z-agent-llm-center (LLM模型管理)
-- 表前缀: z_llm_
-- ---------------------------------------------------

-- LLM供应商表
CREATE TABLE IF NOT EXISTS `z_llm_provider`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `provider_code`
    VARCHAR
(
    32
) NOT NULL COMMENT '供应商编码: ollama/openai/azure/claude/gemini',
    `provider_name` VARCHAR
(
    128
) NOT NULL COMMENT '供应商名称',
    `base_url` VARCHAR
(
    256
) COMMENT 'API地址',
    `api_key` VARCHAR
(
    512
) COMMENT 'API密钥(加密存储)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `priority` INT DEFAULT 100 COMMENT '优先级(越小越优先)',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_provider_code`
(
    `provider_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM供应商配置';

-- LLM模型表
CREATE TABLE IF NOT EXISTS `z_llm_model`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `model_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '模型编码',
    `model_name` VARCHAR
(
    128
) NOT NULL COMMENT '模型名称(展示用)',
    `provider_code` VARCHAR
(
    32
) NOT NULL COMMENT '所属供应商编码',
    `model_type` VARCHAR
(
    32
) DEFAULT 'chat' COMMENT '模型类型: chat/completion/embedding',
    `context_window` INT COMMENT '上下文窗口(token)',
    `supports_function_call` TINYINT DEFAULT 0 COMMENT '是否支持函数调用',
    `supports_vision` TINYINT DEFAULT 0 COMMENT '是否支持视觉',
    `max_output_tokens` INT COMMENT '最大输出token',
    `input_price` DECIMAL
(
    10,
    6
) COMMENT '输入价格(元/千token)',
    `output_price` DECIMAL
(
    10,
    6
) COMMENT '输出价格(元/千token)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `description` VARCHAR
(
    512
) COMMENT '模型描述',
    `default_params` JSON COMMENT '默认参数配置',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_model_code`
(
    `model_code`
),
    KEY `idx_provider`
(
    `provider_code`
),
    KEY `idx_enabled`
(
    `enabled`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM模型配置';

-- Agent工具模板表
CREATE TABLE IF NOT EXISTS `z_agent_tool_template`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `tool_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '工具编码',
    `tool_name` VARCHAR
(
    128
) NOT NULL COMMENT '工具名称',
    `tool_type` VARCHAR
(
    32
) DEFAULT 'function' COMMENT '工具类型: function/http/search',
    `description` VARCHAR
(
    512
) COMMENT '工具描述',
    `icon` VARCHAR
(
    64
) COMMENT '图标',
    `definition` JSON NOT NULL COMMENT '工具定义(JSON Schema)',
    `endpoint` VARCHAR
(
    256
) COMMENT 'HTTP工具调用地址',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_tool_code`
(
    `tool_code`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具模板';

-- ---------------------------------------------------
-- Part 3: z-agent-llm-gateway (LLM用量记录)
-- 表前缀: z_llm_usage_
-- ---------------------------------------------------

-- LLM调用明细表
CREATE TABLE IF NOT EXISTS `z_llm_usage_record`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `trace_id`
    VARCHAR
(
    64
) NOT NULL COMMENT '调用追踪ID',
    `app_code` VARCHAR
(
    64
) COMMENT '应用编码',
    `instance_code` VARCHAR
(
    64
) COMMENT '实例编码',
    `user_id` VARCHAR
(
    64
) COMMENT '用户ID',
    `user_name` VARCHAR
(
    128
) COMMENT '用户名称',
    `provider_code` VARCHAR
(
    32
) NOT NULL COMMENT '供应商编码: ollama/openai/azure',
    `model_code` VARCHAR
(
    64
) NOT NULL COMMENT '模型编码',
    `input_tokens` INT DEFAULT 0 COMMENT '输入token数',
    `output_tokens` INT DEFAULT 0 COMMENT '输出token数',
    `total_tokens` INT DEFAULT 0 COMMENT '总token数',
    `latency_ms` INT DEFAULT 0 COMMENT '响应延迟(毫秒)',
    `status` VARCHAR
(
    16
) DEFAULT 'SUCCESS' COMMENT '状态: SUCCESS/FAILED',
    `error_msg` VARCHAR
(
    512
) COMMENT '错误信息',
    `conversation_code` VARCHAR
(
    64
) COMMENT '会话编码',
    `request_id` VARCHAR
(
    128
) COMMENT 'LLM返回的请求ID',
    `input_price` DECIMAL
(
    10,
    6
) DEFAULT 0 COMMENT '输入价格(元/千token)',
    `output_price` DECIMAL
(
    10,
    6
) DEFAULT 0 COMMENT '输出价格(元/千token)',
    `total_cost` DECIMAL
(
    10,
    4
) DEFAULT 0 COMMENT '本次调用费用(元)',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY
(
    `id`
),
    KEY `idx_app_code`
(
    `app_code`
),
    KEY `idx_instance_code`
(
    `instance_code`
),
    KEY `idx_user_id`
(
    `user_id`
),
    KEY `idx_conversation`
(
    `conversation_code`
),
    KEY `idx_provider_model`
(
    `provider_code`,
    `model_code`
),
    KEY `idx_gmt_create`
(
    `gmt_create`
),
    KEY `idx_trace_id`
(
    `trace_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM调用明细表';

-- LLM日汇总表
CREATE TABLE IF NOT EXISTS `z_llm_usage_daily`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `stat_date`
    DATE
    NOT
    NULL
    COMMENT
    '统计日期',
    `app_code`
    VARCHAR
(
    64
) COMMENT '应用编码',
    `user_id` VARCHAR
(
    64
) COMMENT '用户ID',
    `provider_code` VARCHAR
(
    32
) NOT NULL COMMENT '供应商编码',
    `model_code` VARCHAR
(
    64
) NOT NULL COMMENT '模型编码',
    `total_calls` INT DEFAULT 0 COMMENT '总调用次数',
    `total_input_tokens` BIGINT DEFAULT 0 COMMENT '总输入token',
    `total_output_tokens` BIGINT DEFAULT 0 COMMENT '总输出token',
    `total_tokens` BIGINT DEFAULT 0 COMMENT '总token',
    `total_cost` DECIMAL
(
    10,
    4
) DEFAULT 0 COMMENT '总费用(元)',
    `avg_latency_ms` INT DEFAULT 0 COMMENT '平均延迟',
    `success_calls` INT DEFAULT 0 COMMENT '成功次数',
    `failed_calls` INT DEFAULT 0 COMMENT '失败次数',
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_date_app_user_provider_model`
(
    `stat_date`,
    `app_code`,
    `user_id`,
    `provider_code`,
    `model_code`
),
    KEY `idx_stat_date`
(
    `stat_date`
),
    KEY `idx_app_code`
(
    `app_code`
),
    KEY `idx_user_id`
(
    `user_id`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM日汇总表';

-- LLM会话汇总表
CREATE TABLE IF NOT EXISTS `z_llm_usage_conversation`
(
    `id`
    BIGINT
    NOT
    NULL
    AUTO_INCREMENT
    COMMENT
    '主键',
    `conversation_code`
    VARCHAR
(
    64
) NOT NULL COMMENT '会话编码',
    `app_code` VARCHAR
(
    64
) COMMENT '应用编码',
    `instance_code` VARCHAR
(
    64
) COMMENT '实例编码',
    `user_id` VARCHAR
(
    64
) COMMENT '用户ID',
    `user_name` VARCHAR
(
    128
) COMMENT '用户名称',
    `provider_code` VARCHAR
(
    32
) COMMENT '供应商编码',
    `model_code` VARCHAR
(
    64
) COMMENT '模型编码',
    `total_calls` INT DEFAULT 0 COMMENT '总调用次数',
    `total_input_tokens` BIGINT DEFAULT 0 COMMENT '总输入token',
    `total_output_tokens` BIGINT DEFAULT 0 COMMENT '总输出token',
    `total_tokens` BIGINT DEFAULT 0 COMMENT '总token',
    `total_cost` DECIMAL
(
    10,
    4
) DEFAULT 0 COMMENT '总费用',
    `first_message` TEXT COMMENT '首条用户消息',
    `last_message` TEXT COMMENT '末条用户消息',
    `gmt_create` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `gmt_modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY
(
    `id`
),
    UNIQUE KEY `uk_conversation_code`
(
    `conversation_code`
),
    KEY `idx_app_code`
(
    `app_code`
),
    KEY `idx_instance_code`
(
    `instance_code`
),
    KEY `idx_user_id`
(
    `user_id`
),
    KEY `idx_gmt_create`
(
    `gmt_create`
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM会话汇总表';

-- ---------------------------------------------------
-- 初始化默认数据
-- ---------------------------------------------------

-- 初始化默认Provider
INSERT INTO `z_llm_provider` (`provider_code`, `provider_name`, `base_url`, `enabled`, `priority`)
VALUES ('ollama', 'Ollama (本地)', 'http://localhost:11434', 1, 10),
       ('openai', 'OpenAI', 'https://api.openai.com/v1', 1, 20),
       ('azure', 'Azure OpenAI', '', 0, 30);

-- 初始化默认模型
INSERT INTO `z_llm_model` (`model_code`, `model_name`, `provider_code`, `model_type`, `context_window`,
                           `supports_function_call`, `enabled`, `description`)
VALUES ('qwen2.5:7b', 'Qwen 2.5 7B', 'ollama', 'chat', 8192, 0, 1, '通义千问2.5 7B参数版本'),
       ('qwen2.5:14b', 'Qwen 2.5 14B', 'ollama', 'chat', 8192, 0, 1, '通义千问2.5 14B参数版本'),
       ('llama3:8b', 'Llama 3 8B', 'ollama', 'chat', 8192, 0, 1, 'Meta Llama 3 8B'),
       ('gpt-4o', 'GPT-4o', 'openai', 'chat', 128000, 1, 1, 'OpenAI最新多模态模型'),
       ('gpt-4o-mini', 'GPT-4o Mini', 'openai', 'chat', 128000, 1, 1, '轻量版GPT-4o'),
       ('gpt-4-turbo', 'GPT-4 Turbo', 'openai', 'chat', 128000, 1, 1, 'GPT-4 turbo版本');

-- 初始化默认工具模板
INSERT INTO `z_agent_tool_template` (`tool_code`, `tool_name`, `tool_type`, `description`, `definition`)
VALUES ('weather', '天气查询', 'function', '查询指定城市的天气信息',
        '{"type":"object","properties":{"city":{"type":"string","description":"城市名称"}},"required":["city"]}'),
       ('calculator', '计算器', 'function', '执行数学计算',
        '{"type":"object","properties":{"expression":{"type":"string","description":"数学表达式"}},"required":["expression"]}'),
       ('web_search', '网页搜索', 'http', '搜索互联网信息',
        '{"type":"object","properties":{"query":{"type":"string","description":"搜索关键词"},"count":{"type":"integer","description":"返回数量"}},"required":["query"]}'),
       ('code_executor', '代码执行', 'function', '执行代码片段',
        '{"type":"object","properties":{"language":{"type":"string","description":"编程语言"},"code":{"type":"string","description":"代码内容"}},"required":["language","code"]}');
