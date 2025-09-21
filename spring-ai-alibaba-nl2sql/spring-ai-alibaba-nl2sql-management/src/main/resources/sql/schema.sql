-- 简化的数据库初始化脚本，兼容Spring Boot SQL初始化

-- 智能体表
CREATE TABLE IF NOT EXISTS agent (
    id INT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL COMMENT '智能体名称',
    description TEXT COMMENT '智能体描述',
    avatar TEXT COMMENT '头像URL',
    status VARCHAR(50) DEFAULT 'draft' COMMENT '状态：draft-待发布，published-已发布，offline-已下线',
    prompt TEXT COMMENT '自定义Prompt配置',
    category VARCHAR(100) COMMENT '分类',
    admin_id BIGINT COMMENT '管理员ID',
    tags TEXT COMMENT '标签，逗号分隔',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    human_review_enabled TINYINT DEFAULT 0 COMMENT '是否启用计划人工复核：0-否，1-是',
    PRIMARY KEY (id),
    INDEX idx_name (name),
    INDEX idx_status (status),
    INDEX idx_category (category),
    INDEX idx_admin_id (admin_id)
    ) ENGINE = InnoDB COMMENT = '智能体表';

-- 业务知识表
CREATE TABLE IF NOT EXISTS business_knowledge (
  id INT NOT NULL AUTO_INCREMENT,
  business_term VARCHAR(255) NOT NULL COMMENT '业务名词',
  description TEXT COMMENT '描述',
  synonyms TEXT COMMENT '同义词',
  is_recall INT DEFAULT 1 COMMENT '是否召回',
  data_set_id VARCHAR(255) COMMENT '数据集id',
  agent_id INT COMMENT '关联的智能体ID',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_business_term (business_term),
  INDEX idx_data_set_id (data_set_id),
  INDEX idx_agent_id (agent_id),
  INDEX idx_is_recall (is_recall),
  FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE SET NULL
) ENGINE = InnoDB COMMENT = '业务知识表';

-- 语义模型表
CREATE TABLE IF NOT EXISTS semantic_model (
  id INT NOT NULL AUTO_INCREMENT,
  agent_id INT COMMENT '关联的智能体ID',
  field_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '智能体字段名称',
  synonyms TEXT COMMENT '字段名称同义词',
  origin_name VARCHAR(255) DEFAULT '' COMMENT '原始字段名',
  description TEXT COMMENT '字段描述',
  origin_description VARCHAR(255) COMMENT '原始字段描述',
  type VARCHAR(255) DEFAULT '' COMMENT '字段类型 (integer, varchar....)',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_recall TINYINT DEFAULT 1 COMMENT '0 停用 1 启用',
  status TINYINT DEFAULT 1 COMMENT '0 停用 1 启用',
  PRIMARY KEY (id),
  INDEX idx_agent_id (agent_id),
  INDEX idx_field_name (field_name),
  INDEX idx_status (status),
  INDEX idx_is_recall (is_recall),
  FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE SET NULL
) ENGINE = InnoDB COMMENT = '语义模型表';


-- 智能体知识表
CREATE TABLE IF NOT EXISTS agent_knowledge (
  id INT NOT NULL AUTO_INCREMENT,
  agent_id INT NOT NULL COMMENT '智能体ID',
  title VARCHAR(255) NOT NULL COMMENT '知识标题',
  content TEXT COMMENT '知识内容',
  type VARCHAR(50) DEFAULT 'document' COMMENT '知识类型：document-文档，qa-问答，faq-常见问题',
  category VARCHAR(100) COMMENT '知识分类',
  tags TEXT COMMENT '标签，逗号分隔',
  status VARCHAR(50) DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
  source_url VARCHAR(500) COMMENT '来源URL',
  file_path VARCHAR(500) COMMENT '文件路径',
  file_size BIGINT COMMENT '文件大小（字节）',
  file_type VARCHAR(100) COMMENT '文件类型',
  embedding_status VARCHAR(50) DEFAULT 'pending' COMMENT '向量化状态：pending-待处理，processing-处理中，completed-已完成，failed-失败',
  creator_id BIGINT COMMENT '创建者ID',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_agent_id (agent_id),
  INDEX idx_title (title),
  INDEX idx_type (type),
  INDEX idx_status (status),
  INDEX idx_category (category),
  INDEX idx_embedding_status (embedding_status),
  FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '智能体知识表';

-- 数据源表
CREATE TABLE IF NOT EXISTS datasource (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL COMMENT '数据源名称',
  type VARCHAR(50) NOT NULL COMMENT '数据源类型：mysql, postgresql',
  host VARCHAR(255) NOT NULL COMMENT '主机地址',
  port INT NOT NULL COMMENT '端口号',
  database_name VARCHAR(255) NOT NULL COMMENT '数据库名称',
  username VARCHAR(255) NOT NULL COMMENT '用户名',
  password VARCHAR(255) NOT NULL COMMENT '密码（加密存储）',
  connection_url VARCHAR(1000) COMMENT '完整连接URL',
  status VARCHAR(50) DEFAULT 'active' COMMENT '状态：active-启用，inactive-禁用',
  test_status VARCHAR(50) DEFAULT 'unknown' COMMENT '连接测试状态：success-成功，failed-失败，unknown-未知',
  description TEXT COMMENT '描述',
  creator_id BIGINT COMMENT '创建者ID',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_name (name),
  INDEX idx_type (type),
  INDEX idx_status (status),
  INDEX idx_creator_id (creator_id)
) ENGINE = InnoDB COMMENT = '数据源表';

-- 智能体数据源关联表
CREATE TABLE IF NOT EXISTS agent_datasource (
  id INT NOT NULL AUTO_INCREMENT,
  agent_id INT NOT NULL COMMENT '智能体ID',
  datasource_id INT NOT NULL COMMENT '数据源ID',
  is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_agent_datasource (agent_id, datasource_id),
  INDEX idx_agent_id (agent_id),
  INDEX idx_datasource_id (datasource_id),
  INDEX idx_is_active (is_active),
  FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE,
  FOREIGN KEY (datasource_id) REFERENCES datasource(id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '智能体数据源关联表';

-- 智能体预设问题表
CREATE TABLE IF NOT EXISTS agent_preset_question (
  id INT NOT NULL AUTO_INCREMENT,
  agent_id INT NOT NULL COMMENT '智能体ID',
  question TEXT NOT NULL COMMENT '预设问题内容',
  sort_order INT DEFAULT 0 COMMENT '排序顺序',
  is_active TINYINT DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_agent_id (agent_id),
  INDEX idx_sort_order (sort_order),
  INDEX idx_is_active (is_active),
  FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '智能体预设问题表';

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
  id VARCHAR(36) NOT NULL COMMENT '会话ID（UUID）',
  agent_id INT NOT NULL COMMENT '智能体ID',
  title VARCHAR(255) DEFAULT '新对话' COMMENT '会话标题',
  status VARCHAR(50) DEFAULT 'active' COMMENT '状态：active-活跃，archived-归档，deleted-已删除',
  is_pinned TINYINT DEFAULT 0 COMMENT '是否置顶：0-否，1-是',
  user_id BIGINT COMMENT '用户ID',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_agent_id (agent_id),
  INDEX idx_user_id (user_id),
  INDEX idx_status (status),
  INDEX idx_is_pinned (is_pinned),
  INDEX idx_create_time (create_time),
  FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '聊天会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS chat_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(36) NOT NULL COMMENT '会话ID',
  role VARCHAR(20) NOT NULL COMMENT '角色：user-用户，assistant-助手，system-系统',
  content TEXT NOT NULL COMMENT '消息内容',
  message_type VARCHAR(50) DEFAULT 'text' COMMENT '消息类型：text-文本，sql-SQL查询，result-查询结果，error-错误',
  metadata JSON COMMENT '元数据（JSON格式）',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  INDEX idx_session_id (session_id),
  INDEX idx_role (role),
  INDEX idx_message_type (message_type),
  INDEX idx_create_time (create_time),
  FOREIGN KEY (session_id) REFERENCES chat_session(id) ON DELETE CASCADE
) ENGINE = InnoDB COMMENT = '聊天消息表';

-- 用户Prompt配置表
CREATE TABLE IF NOT EXISTS user_prompt_config (
  id VARCHAR(36) NOT NULL COMMENT '配置ID（UUID）',
  name VARCHAR(255) NOT NULL COMMENT '配置名称',
  prompt_type VARCHAR(100) NOT NULL COMMENT 'Prompt类型（如report-generator, planner等）',
  system_prompt TEXT NOT NULL COMMENT '用户自定义系统Prompt内容',
  enabled TINYINT DEFAULT 1 COMMENT '是否启用该配置：0-禁用，1-启用',
  description TEXT COMMENT '配置描述',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  creator VARCHAR(255) COMMENT '创建者',
  PRIMARY KEY (id),
  INDEX idx_prompt_type (prompt_type),
  INDEX idx_enabled (enabled),
  INDEX idx_create_time (create_time)
) ENGINE = InnoDB COMMENT = '用户Prompt配置表';
