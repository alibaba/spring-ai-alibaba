-- 初始化数据文件
-- 只在表为空时插入示例数据

-- 业务知识示例数据
INSERT IGNORE INTO `business_knowledge` (`id`, `business_term`, `description`, `synonyms`, `is_recall`, `data_set_id`, `agent_id`, `created_time`, `updated_time`) VALUES 
(1, 'Customer Satisfaction', 'Measures how satisfied customers are with the service or product.', 'customer happiness, client contentment', 1, 'dataset_001', 1, NOW(), NOW()),
(2, 'Net Promoter Score', 'A measure of the likelihood of customers recommending a company to others.', 'NPS, customer loyalty score', 1, 'dataset_002', 1, NOW(), NOW()),
(3, 'Customer Retention Rate', 'The percentage of customers who continue to use a service over a given period.', 'retention, customer loyalty', 1, 'dataset_003', 2, NOW(), NOW());

-- 语义模型示例数据
INSERT IGNORE INTO `semantic_model` (`id`, `agent_id`, `field_name`, `synonyms`, `origin_name`, `description`, `origin_description`, `type`, `created_time`, `updated_time`, `is_recall`, `status`) VALUES 
(1, 1, 'customerSatisfactionScore', 'satisfaction score, customer rating', 'csat_score', 'Customer satisfaction rating from 1-10', 'Customer satisfaction score', 'integer', NOW(), NOW(), 1, 1),
(2, 1, 'netPromoterScore', 'NPS, promoter score', 'nps_value', 'Net Promoter Score from -100 to 100', 'NPS calculation result', 'integer', NOW(), NOW(), 1, 1),
(3, 2, 'customerRetentionRate', 'retention rate, loyalty rate', 'retention_pct', 'Percentage of retained customers', 'Customer retention percentage', 'decimal', NOW(), NOW(), 1, 1);

-- 智能体示例数据
INSERT IGNORE INTO `agent` (`id`, `name`, `description`, `avatar`, `status`, `prompt`, `category`, `admin_id`, `tags`, `create_time`, `update_time`, `human_review_enabled`) VALUES 
(1, '中国人口GDP数据智能体', '专门处理中国人口和GDP相关数据查询分析的智能体', '/avatars/china-gdp-agent.png', 'published', '你是一个专业的数据分析助手，专门处理中国人口和GDP相关的数据查询。请根据用户的问题，生成准确的SQL查询语句。', '数据分析', 2100246635, '人口数据,GDP分析,经济统计', NOW(), NOW(), 0),
(2, '销售数据分析智能体', '专注于销售数据分析和业务指标计算的智能体', '/avatars/sales-agent.png', 'published', '你是一个销售数据分析专家，能够帮助用户分析销售趋势、客户行为和业务指标。', '业务分析', 2100246635, '销售分析,业务指标,客户分析', NOW(), NOW(), 0),
(3, '财务报表智能体', '专门处理财务数据和报表分析的智能体', '/avatars/finance-agent.png', 'draft', '你是一个财务分析专家，专门处理财务数据查询和报表生成。', '财务分析', 2100246635, '财务数据,报表分析,会计', NOW(), NOW(), 0),
(4, '库存管理智能体', '专注于库存数据管理和供应链分析的智能体', '/avatars/inventory-agent.png', 'published', '你是一个库存管理专家，能够帮助用户查询库存状态、分析供应链数据。', '供应链', 2100246635, '库存管理,供应链,物流', NOW(), NOW(), 0);

-- 智能体知识示例数据
INSERT IGNORE INTO `agent_knowledge` (`id`, `agent_id`, `title`, `content`, `type`, `category`, `tags`, `status`, `source_url`, `file_type`, `embedding_status`, `creator_id`, `create_time`, `update_time`) VALUES 
(1, 1, '中国人口统计数据说明', '中国人口统计数据包含了历年的人口总数、性别比例、年龄结构、城乡分布等详细信息。数据来源于国家统计局，具有权威性和准确性。查询时请注意数据的时间范围和统计口径。', 'document', '数据说明', '人口统计,数据源,统计局', 'active', 'http://stats.gov.cn/population', 'text', 'completed', 2100246635, NOW(), NOW()),
(2, 1, 'GDP数据使用指南', 'GDP（国内生产总值）数据反映了国家经济发展水平。包含名义GDP、实际GDP、GDP增长率等指标。数据按季度和年度进行统计，支持按地区、行业进行分类查询。', 'document', '使用指南', 'GDP,经济指标,增长率', 'active', 'http://stats.gov.cn/gdp', 'text', 'completed', 2100246635, NOW(), NOW()),
(3, 1, '常见查询问题', '问：如何查询2023年的人口数据？\n答：可以使用"SELECT * FROM population WHERE year = 2023"进行查询。\n\n问：如何计算GDP增长率？\n答：GDP增长率 = (当年GDP - 上年GDP) / 上年GDP * 100%', 'qa', '常见问题', '查询示例,FAQ,SQL语句', 'active', NULL, 'text', 'completed', 2100246635, NOW(), NOW()),
(4, 2, '销售数据字段说明', '销售数据表包含以下关键字段：\n- sales_amount：销售金额\n- customer_id：客户ID\n- product_id：产品ID\n- sales_date：销售日期\n- region：销售区域\n- sales_rep：销售代表', 'document', '数据字典', '销售数据,字段说明,数据结构', 'active', NULL, 'text', 'completed', 2100246635, NOW(), NOW()),
(5, 2, '客户分析指标体系', '客户分析包含多个维度：\n1. 客户价值分析：RFM模型（最近购买时间、购买频次、购买金额）\n2. 客户生命周期：新客户、活跃客户、流失客户\n3. 客户满意度：NPS评分、满意度调研\n4. 客户行为分析：购买偏好、渠道偏好', 'document', '分析方法', '客户分析,RFM,生命周期,满意度', 'active', NULL, 'text', 'processing', 2100246635, NOW(), NOW()),
(6, 3, '财务报表模板', '标准财务报表包含：\n1. 资产负债表：反映企业财务状况\n2. 利润表：反映企业经营成果\n3. 现金流量表：反映企业现金流动情况\n4. 所有者权益变动表：反映股东权益变化', 'document', '报表模板', '财务报表,资产负债表,利润表,现金流', 'active', 'https://finance.example.com/templates', 'pdf', 'completed', 2100246635, NOW(), NOW()),
(7, 4, '库存管理最佳实践', '库存管理的核心要点：\n1. 安全库存设置：确保不断货\n2. ABC分类管理：重点管理A类物料\n3. 先进先出原则：避免库存积压\n4. 定期盘点：确保数据准确性\n5. 供应商管理：建立稳定供应关系', 'document', '最佳实践', '库存管理,安全库存,ABC分类,盘点', 'active', NULL, 'text', 'completed', 2100246635, NOW(), NOW());

-- 数据源示例数据
-- 示例数据源可以运行docker-compose-datasource.yml建立，或者手动修改为自己的数据源
INSERT IGNORE INTO `datasource` (`id`, `name`, `type`, `host`, `port`, `database_name`, `username`, `password`, `connection_url`, `status`, `test_status`, `description`, `creator_id`, `create_time`, `update_time`) VALUES 
(1, '生产环境MySQL数据库', 'mysql', 'mysql-data', 3306, 'product_db', 'root', 'root', 'jdbc:mysql://mysql-data:3306/product_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true', 'active', 'success', '生产环境主数据库，包含核心业务数据', 2100246635, NOW(), NOW()),
(2, '数据仓库PostgreSQL', 'postgresql', 'postgres-data', 5432, 'data_warehouse', 'postgres', 'postgres', 'jdbc:postgresql://postgres-data:5432/data_warehouse', 'active', 'success', '数据仓库，用于数据分析和报表生成', 2100246635, NOW(), NOW());

-- 智能体数据源关联示例数据
INSERT IGNORE INTO `agent_datasource` (`id`, `agent_id`, `datasource_id`, `is_active`, `create_time`, `update_time`) VALUES 
(1, 1, 2, 1, NOW(), NOW()),  -- 中国人口GDP数据智能体使用数据仓库
(2, 2, 1, 1, NOW(), NOW()),  -- 销售数据分析智能体使用生产环境数据库
(3, 3, 1, 1, NOW(), NOW()),  -- 财务报表智能体使用生产环境数据库
(4, 4, 1, 1, NOW(), NOW());  -- 库存管理智能体使用生产环境数据库
