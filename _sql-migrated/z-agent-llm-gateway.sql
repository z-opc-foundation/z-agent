-- LLM凭证表 - 管理API Key
CREATE TABLE IF NOT EXISTS z_llm_credential
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY
    COMMENT
    '主键',
    credential_code
    VARCHAR
(
    64
) NOT NULL UNIQUE COMMENT '凭证编码',
    credential_name VARCHAR
(
    128
) NOT NULL COMMENT '凭证名称',
    user_id VARCHAR
(
    64
) NOT NULL COMMENT '所属用户',
    provider_code VARCHAR
(
    32
) NOT NULL COMMENT 'Provider编码',
    api_key VARCHAR
(
    512
) NOT NULL COMMENT 'API Key(加密存储)',
    api_secret VARCHAR
(
    256
) DEFAULT NULL COMMENT 'API Secret(可选)',
    model_codes VARCHAR
(
    1024
) DEFAULT NULL COMMENT '允许使用的模型,逗号分隔',
    daily_limit INT DEFAULT 0 COMMENT '每日限额(0=不限)',
    monthly_limit INT DEFAULT 0 COMMENT '每月限额(0=不限)',
    enabled TINYINT DEFAULT 1 COMMENT '启用状态:0=禁用,1=启用',
    priority INT DEFAULT 0 COMMENT '优先级(高=优先使用)',
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id
(
    user_id
),
    INDEX idx_provider_code
(
    provider_code
),
    INDEX idx_enabled
(
    enabled
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM凭证表';

-- LLM用量明细表(优化版)
CREATE TABLE IF NOT EXISTS z_llm_usage_record
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    trace_id
    VARCHAR
(
    64
) NOT NULL COMMENT '追踪ID',
    credential_id BIGINT DEFAULT NULL COMMENT '凭证ID',
    app_code VARCHAR
(
    64
) DEFAULT NULL COMMENT '应用编码',
    instance_code VARCHAR
(
    64
) DEFAULT NULL COMMENT '实例编码',
    user_id VARCHAR
(
    64
) DEFAULT NULL COMMENT '用户ID',
    user_name VARCHAR
(
    128
) DEFAULT NULL COMMENT '用户名',
    provider_code VARCHAR
(
    32
) NOT NULL COMMENT 'Provider',
    model_code VARCHAR
(
    64
) NOT NULL COMMENT '模型',
    input_tokens INT DEFAULT 0 COMMENT '输入Token数',
    output_tokens INT DEFAULT 0 COMMENT '输出Token数',
    total_tokens INT DEFAULT 0 COMMENT '总Token数',
    latency_ms INT DEFAULT 0 COMMENT '延迟毫秒',
    status VARCHAR
(
    16
) DEFAULT 'SUCCESS' COMMENT '状态:SUCCESS/FAILED',
    error_msg VARCHAR
(
    512
) DEFAULT NULL COMMENT '错误信息',
    request_id VARCHAR
(
    128
) DEFAULT NULL COMMENT 'Provider返回的请求ID',
    input_price DECIMAL
(
    10,
    6
) DEFAULT 0 COMMENT '输入单价(元/千token)',
    output_price DECIMAL
(
    10,
    6
) DEFAULT 0 COMMENT '输出单价(元/千token)',
    total_cost DECIMAL
(
    10,
    4
) DEFAULT 0 COMMENT '总费用',
    conversation_code VARCHAR
(
    64
) DEFAULT NULL COMMENT '会话编码',
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id
(
    trace_id
),
    INDEX idx_credential_id
(
    credential_id
),
    INDEX idx_user_id
(
    user_id
),
    INDEX idx_provider_model
(
    provider_code,
    model_code
),
    INDEX idx_gmt_create
(
    gmt_create
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM用量明细表';

-- LLM用量日汇总表
CREATE TABLE IF NOT EXISTS z_llm_usage_daily
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    stat_date
    DATE
    NOT
    NULL
    COMMENT
    '统计日期',
    credential_id
    BIGINT
    DEFAULT
    NULL
    COMMENT
    '凭证ID',
    app_code
    VARCHAR
(
    64
) DEFAULT NULL,
    user_id VARCHAR
(
    64
) DEFAULT NULL,
    provider_code VARCHAR
(
    32
) NOT NULL,
    model_code VARCHAR
(
    64
) NOT NULL,
    total_calls INT DEFAULT 0 COMMENT '总调用次数',
    success_calls INT DEFAULT 0 COMMENT '成功次数',
    failed_calls INT DEFAULT 0 COMMENT '失败次数',
    total_input_tokens BIGINT DEFAULT 0,
    total_output_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL
(
    10,
    4
) DEFAULT 0,
    avg_latency_ms INT DEFAULT 0,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stat_credential
(
    stat_date,
    credential_id,
    provider_code,
    model_code
),
    UNIQUE KEY uk_stat_user
(
    stat_date,
    user_id,
    provider_code,
    model_code
),
    INDEX idx_stat_date
(
    stat_date
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM用量日汇总表';

-- LLM用量会话汇总表
CREATE TABLE IF NOT EXISTS z_llm_usage_conversation
(
    id
    BIGINT
    AUTO_INCREMENT
    PRIMARY
    KEY,
    conversation_code
    VARCHAR
(
    64
) NOT NULL UNIQUE COMMENT '会话编码',
    credential_id BIGINT DEFAULT NULL,
    app_code VARCHAR
(
    64
) DEFAULT NULL,
    instance_code VARCHAR
(
    64
) DEFAULT NULL,
    user_id VARCHAR
(
    64
) DEFAULT NULL,
    user_name VARCHAR
(
    128
) DEFAULT NULL,
    provider_code VARCHAR
(
    32
) NOT NULL,
    model_code VARCHAR
(
    64
) NOT NULL,
    total_calls INT DEFAULT 0,
    total_input_tokens BIGINT DEFAULT 0,
    total_output_tokens BIGINT DEFAULT 0,
    total_tokens BIGINT DEFAULT 0,
    total_cost DECIMAL
(
    10,
    4
) DEFAULT 0,
    first_message TEXT,
    last_message TEXT,
    gmt_create DATETIME DEFAULT CURRENT_TIMESTAMP,
    gmt_modified DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id
(
    user_id
),
    INDEX idx_gmt_create
(
    gmt_create
)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM用量会话汇总表';