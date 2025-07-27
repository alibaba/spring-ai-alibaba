-- 简化的数据库初始化脚本，兼容Spring Boot SQL初始化

-- 业务知识表
CREATE TABLE IF NOT EXISTS business_knowledge (
  id INT NOT NULL AUTO_INCREMENT,
  business_term VARCHAR(255) NOT NULL COMMENT '业务名词',
  description TEXT COMMENT '描述',
  synonyms TEXT COMMENT '同义词',
  is_recall INT DEFAULT 1 COMMENT '是否召回',
  data_set_id VARCHAR(255) COMMENT '数据集id',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_business_term (business_term),
  INDEX idx_data_set_id (data_set_id),
  INDEX idx_is_recall (is_recall)
) ENGINE = InnoDB COMMENT = '业务知识表';

-- 语义模型表
CREATE TABLE IF NOT EXISTS semantic_model (
  id INT NOT NULL AUTO_INCREMENT,
  field_name VARCHAR(255) NOT NULL DEFAULT '' COMMENT '智能体字段名称',
  synonyms TEXT COMMENT '字段名称同义词',
  data_set_id VARCHAR(255) COMMENT '数据集id',
  origin_name VARCHAR(255) DEFAULT '' COMMENT '原始字段名',
  description TEXT COMMENT '字段描述',
  origin_description VARCHAR(255) COMMENT '原始字段描述',
  type VARCHAR(255) DEFAULT '' COMMENT '字段类型 (integer, varchar....)',
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  is_recall TINYINT DEFAULT 1 COMMENT '0 停用 1 启用',
  status TINYINT DEFAULT 1 COMMENT '0 停用 1 启用',
  PRIMARY KEY (id),
  INDEX idx_field_name (field_name),
  INDEX idx_data_set_id (data_set_id),
  INDEX idx_status (status),
  INDEX idx_is_recall (is_recall)
) ENGINE = InnoDB COMMENT = '语义模型表';

-- 智能体表
CREATE TABLE IF NOT EXISTS agent (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(255) NOT NULL COMMENT '智能体名称',
  description TEXT COMMENT '智能体描述',
  avatar VARCHAR(500) COMMENT '头像URL',
  status VARCHAR(50) DEFAULT 'draft' COMMENT '状态：draft-待发布，published-已发布，offline-已下线',
  prompt TEXT COMMENT '自定义Prompt配置',
  category VARCHAR(100) COMMENT '分类',
  admin_id BIGINT COMMENT '管理员ID',
  tags TEXT COMMENT '标签，逗号分隔',
  create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  INDEX idx_name (name),
  INDEX idx_status (status),
  INDEX idx_category (category),
  INDEX idx_admin_id (admin_id)
) ENGINE = InnoDB COMMENT = '智能体表';
