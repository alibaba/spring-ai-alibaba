-- 初始化数据文件
-- 只在表为空时插入示例数据

-- 业务知识示例数据
INSERT IGNORE INTO `business_knowledge` (`id`, `business_term`, `description`, `synonyms`, `is_recall`, `data_set_id`, `created_time`, `updated_time`) VALUES 
(1, 'Customer Satisfaction', 'Measures how satisfied customers are with the service or product.', 'customer happiness, client contentment', 1, 'dataset_001', NOW(), NOW()),
(2, 'Net Promoter Score', 'A measure of the likelihood of customers recommending a company to others.', 'NPS, customer loyalty score', 1, 'dataset_002', NOW(), NOW()),
(3, 'Customer Retention Rate', 'The percentage of customers who continue to use a service over a given period.', 'retention, customer loyalty', 1, 'dataset_003', NOW(), NOW());

-- 语义模型示例数据
INSERT IGNORE INTO `semantic_model` (`id`, `field_name`, `synonyms`, `data_set_id`, `origin_name`, `description`, `origin_description`, `type`, `created_time`, `updated_time`, `is_recall`, `status`) VALUES 
(1, 'customerSatisfactionScore', 'satisfaction score, customer rating', 'dataset_001', 'csat_score', 'Customer satisfaction rating from 1-10', 'Customer satisfaction score', 'integer', NOW(), NOW(), 1, 1),
(2, 'netPromoterScore', 'NPS, promoter score', 'dataset_002', 'nps_value', 'Net Promoter Score from -100 to 100', 'NPS calculation result', 'integer', NOW(), NOW(), 1, 1),
(3, 'customerRetentionRate', 'retention rate, loyalty rate', 'dataset_003', 'retention_pct', 'Percentage of retained customers', 'Customer retention percentage', 'decimal', NOW(), NOW(), 1, 1);

-- 智能体示例数据
INSERT IGNORE INTO `agent` (`id`, `name`, `description`, `avatar`, `status`, `prompt`, `category`, `admin_id`, `tags`, `create_time`, `update_time`) VALUES 
(1, '中国人口GDP数据智能体', '专门处理中国人口和GDP相关数据查询分析的智能体', '/avatars/china-gdp-agent.png', 'published', '你是一个专业的数据分析助手，专门处理中国人口和GDP相关的数据查询。请根据用户的问题，生成准确的SQL查询语句。', '数据分析', 2100246635, '人口数据,GDP分析,经济统计', NOW(), NOW()),
(2, '销售数据分析智能体', '专注于销售数据分析和业务指标计算的智能体', '/avatars/sales-agent.png', 'published', '你是一个销售数据分析专家，能够帮助用户分析销售趋势、客户行为和业务指标。', '业务分析', 2100246635, '销售分析,业务指标,客户分析', NOW(), NOW()),
(3, '财务报表智能体', '专门处理财务数据和报表分析的智能体', '/avatars/finance-agent.png', 'draft', '你是一个财务分析专家，专门处理财务数据查询和报表生成。', '财务分析', 2100246635, '财务数据,报表分析,会计', NOW(), NOW()),
(4, '库存管理智能体', '专注于库存数据管理和供应链分析的智能体', '/avatars/inventory-agent.png', 'published', '你是一个库存管理专家，能够帮助用户查询库存状态、分析供应链数据。', '供应链', 2100246635, '库存管理,供应链,物流', NOW(), NOW());
