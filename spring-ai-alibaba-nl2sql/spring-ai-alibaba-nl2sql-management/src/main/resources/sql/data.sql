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
