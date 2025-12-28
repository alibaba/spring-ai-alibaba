/******************************************/
/*  SAA Admin Database Tables        */
/******************************************/


/******************************************/
/*   TableName = dataset                  */
/******************************************/

DROP TABLE IF EXISTS dataset;
CREATE TABLE dataset
(
    id             BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    name           VARCHAR(255) NOT NULL COMMENT 'Dataset name',
    description    TEXT                  DEFAULT NULL COMMENT 'Dataset description',
    columns_config LONGTEXT              DEFAULT NULL COMMENT 'Column structure configuration (JSON format)',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted        TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    PRIMARY KEY (id),
    KEY            idx_deleted (deleted)
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Evaluation Dataset Table';


/******************************************/
/*   TableName = dataset_version          */
/******************************************/
DROP TABLE IF EXISTS dataset_version;
CREATE TABLE dataset_version
(
    id            BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    dataset_id    BIGINT(20) UNSIGNED NOT NULL COMMENT 'Dataset ID',
    version       VARCHAR(32) NOT NULL COMMENT 'Version number',
    description   TEXT                 DEFAULT NULL COMMENT 'Version description',
    data_count    INT(11) NOT NULL DEFAULT 0 COMMENT 'Data count for this version',
    status        VARCHAR(32) NOT NULL DEFAULT 'DRAFT' COMMENT 'Version status: DRAFT, PUBLISHED, ARCHIVED',
    experiments   TEXT                 DEFAULT NULL COMMENT 'Experiment collection (one-to-many relationship, JSON format)',
    dataset_items TEXT                 DEFAULT NULL COMMENT 'Dataset item collection (one-to-many relationship, JSON format)',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dataset_version (dataset_id, version),
    KEY           idx_dataset_id (dataset_id),
    CONSTRAINT fk_dataset_version_dataset FOREIGN KEY (dataset_id) REFERENCES dataset (id) ON DELETE CASCADE
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Dataset Version Table';


/******************************************/
/*   TableName = dataset_item             */
/******************************************/
DROP TABLE IF EXISTS dataset_item;
CREATE TABLE dataset_item
(
    id             BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    dataset_id     BIGINT(20) UNSIGNED NOT NULL COMMENT 'Dataset ID',
    columns_config LONGTEXT             DEFAULT NULL COMMENT 'Column structure configuration (JSON format)',
    data_content   LONGTEXT    NOT NULL COMMENT 'Data content (JSON format)',
    create_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted        TINYINT(1)  NOT NULL DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    PRIMARY KEY (id),
    KEY            idx_dataset_id (dataset_id),
    KEY            idx_deleted (deleted),
    CONSTRAINT fk_dataset_item_dataset FOREIGN KEY (dataset_id) REFERENCES dataset (id) ON DELETE CASCADE
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Dataset Item Table';


/******************************************/
/*   TableName = evaluator                */
/******************************************/
DROP TABLE IF EXISTS evaluator;
CREATE TABLE evaluator
(
    id          BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    name        VARCHAR(255) NOT NULL COMMENT 'Evaluator name',
    description TEXT                  DEFAULT NULL COMMENT 'Evaluator description',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    deleted     TINYINT(1)   NOT NULL DEFAULT 0 COMMENT 'Logical delete flag: 0-not deleted, 1-deleted',
    PRIMARY KEY (id),
    KEY         idx_deleted (deleted)
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Evaluator Table';

/******************************************/
/*   TableName = evaluator_version        */
/******************************************/

DROP TABLE IF EXISTS evaluator_version;
CREATE TABLE evaluator_version
(
    id            BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    evaluator_id  BIGINT(20) UNSIGNED NOT NULL COMMENT 'Evaluator ID',
    description   TEXT                 DEFAULT NULL COMMENT 'Evaluator description',
    version       VARCHAR(32) NOT NULL COMMENT 'Version number',
    model_config  TEXT        NOT NULL COMMENT 'Model config',
    prompt        LONGTEXT             DEFAULT NULL COMMENT 'Prompt configuration (JSON format)',
    variables     LONGTEXT             DEFAULT NULL COMMENT 'The variable parameters in the evaluator prompt',
    status        VARCHAR(32)          DEFAULT NULL COMMENT 'Version status: DRAFT, PUBLISHED, ARCHIVED',
    experiments   TEXT                 DEFAULT NULL COMMENT 'Experiment collection (one-to-many relationship, JSON format)',
    create_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (id),
    UNIQUE KEY uk_evaluator_version (evaluator_id, version),
    KEY           idx_evaluator_id (evaluator_id),
    CONSTRAINT fk_evaluator_version_evaluator FOREIGN KEY (evaluator_id) REFERENCES evaluator (id) ON DELETE CASCADE
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Evaluator Version Table';

/******************************************/
/*  TableName = evaluator_prompt_template */
/******************************************/

DROP TABLE IF EXISTS evaluator_template;
CREATE TABLE evaluator_template
(
    id                     BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    evaluator_template_key VARCHAR(255) NOT NULL,
    template_desc          VARCHAR(255) DEFAULT NULL,
    template               LONGTEXT,
    variables              LONGTEXT     DEFAULT NULL,
    model_config           LONGTEXT     DEFAULT NULL COMMENT '推荐使用的模型参数',
    PRIMARY KEY (id),
    UNIQUE KEY evaluator_template_key_UNIQUE (evaluator_template_key),
    KEY         idx_template_desc (template_desc)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;



/******************************************/
/*   TableName = experiment               */
/******************************************/
DROP TABLE IF EXISTS experiment;
CREATE TABLE experiment
(
    id                       BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    name                     VARCHAR(255) NOT NULL COMMENT 'Experiment name',
    description              TEXT                  DEFAULT NULL COMMENT 'Experiment description',
    dataset_id               BIGINT(20) UNSIGNED NOT NULL COMMENT 'Dataset ID',
    dataset_version_id       BIGINT(20) UNSIGNED NOT NULL COMMENT 'Dataset version ID',
    dataset_version          VARCHAR(32)  NOT NULL COMMENT 'Dataset version',
    evaluation_object_config LONGTEXT              DEFAULT NULL COMMENT 'Evaluation object configuration (JSON format)',
    evaluator_config         TEXT         NOT NULL COMMENT 'Evaluator Config',
    status                   VARCHAR(32)  NOT NULL DEFAULT 'DRAFT' COMMENT 'Status: DRAFT, RUNNING, COMPLETED, FAILED, STOPPED',
    progress                 INT(3) NOT NULL DEFAULT 0 COMMENT 'Progress percentage: 0-100',
    complete_time            DATETIME              DEFAULT NULL COMMENT 'Complete time',
    create_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time              DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (id),
    KEY                      idx_dataset_id (dataset_id),
    KEY                      idx_dataset_version_id (dataset_version_id),
    KEY                      idx_status (status),
    KEY                      idx_create_time (create_time)
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Experiment Table';

/******************************************/
/*   TableName = experiment_result        */
/******************************************/
DROP TABLE IF EXISTS experiment_result;
CREATE TABLE experiment_result
(
    id                   BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    experiment_id        BIGINT(20) UNSIGNED NOT NULL COMMENT 'Experiment ID',
    input                LONGTEXT NOT NULL COMMENT 'Input content',
    actual_output        LONGTEXT NOT NULL COMMENT 'Actual output from evaluation object',
    reference_output     LONGTEXT COMMENT 'Reference output for comparison',
    score                DECIMAL(3, 2)     DEFAULT NULL COMMENT 'Evaluation score: 0.0-1.0',
    reason               TEXT              DEFAULT NULL COMMENT 'Evaluation reason',
    evaluation_time      DATETIME          DEFAULT NULL COMMENT 'Evaluation execution time',
    evaluator_version_id BIGINT(20) UNSIGNED NOT NULL COMMENT 'Evaluator version ID',
    create_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    update_time          DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    PRIMARY KEY (id),
    KEY                  idx_experiment_id (experiment_id),
    KEY                  idx_evaluator_version_id (evaluator_version_id),
    KEY                  idx_create_time (create_time)
) ENGINE = InnoDB
AUTO_INCREMENT = 10000
DEFAULT CHARSET = utf8mb4 COLLATE=utf8mb4_0900_ai_ci
COMMENT ='Experiment Result Table';


/******************************************/
/*   table = prompt                       */
/******************************************/

DROP TABLE IF EXISTS prompt;
CREATE TABLE prompt
(
    id             BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    prompt_key     VARCHAR(255) NOT NULL,
    prompt_desc    VARCHAR(255) DEFAULT NULL,
    latest_version VARCHAR(32)  DEFAULT NULL,
    tags           VARCHAR(255) DEFAULT NULL,
    create_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    update_time    DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY prompt_key_UNIQUE (prompt_key),
    KEY         idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

/******************************************/
/*   table = prompt_version               */
/******************************************/

DROP TABLE IF EXISTS prompt_version;
CREATE TABLE prompt_version
(
    id               BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    version          VARCHAR(32)  NOT NULL,
    prompt_key       VARCHAR(255) NOT NULL,
    version_desc     VARCHAR(255)          DEFAULT NULL,
    template         LONGTEXT COMMENT 'Prompt模版内容',
    variables        LONGTEXT              DEFAULT NULL COMMENT 'Prompt模版里的可变参数',
    model_config     LONGTEXT              DEFAULT NULL COMMENT '调试该prompt的模型参数,JSON',
    status           VARCHAR(32)  NOT NULL DEFAULT 'pre' COMMENT '版本状态：pre-预发布版本，release-正式版本',
    create_time      DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    previous_version VARCHAR(32)           DEFAULT NULL COMMENT '前置版本，用于对比',
    PRIMARY KEY (id),
    UNIQUE KEY prompt_key_version_UNIQUE (prompt_key,version),
    KEY         idx_prompt_key (prompt_key),
    KEY         idx_status (status),
    KEY         idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


/******************************************/
/*   table = prompt_build_template        */
/******************************************/

DROP TABLE IF EXISTS prompt_build_template;
CREATE TABLE prompt_build_template
(
    id                  BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'Primary Key ID',
    prompt_template_key VARCHAR(255) NOT NULL,
    tags                VARCHAR(255) DEFAULT NULL,
    template_desc       VARCHAR(255) DEFAULT NULL,
    template            LONGTEXT,
    variables           LONGTEXT     DEFAULT NULL,
    model_config        LONGTEXT     DEFAULT NULL COMMENT '推荐使用的模型参数',
    PRIMARY KEY (id),
    UNIQUE KEY prompt_template_key_UNIQUE (prompt_template_key),
    KEY         idx_tags (tags)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;


/******************************************/
/*   table = model_config                 */
/******************************************/
DROP TABLE IF EXISTS `model_config`;
CREATE TABLE `model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '模型名称',
  `provider` varchar(50) NOT NULL COMMENT '提供商(openai, azure, etc)',
  `model_name` varchar(100) NOT NULL COMMENT '模型标识符(gpt-4, gpt-3.5-turbo等)',
  `base_url` varchar(500) NOT NULL COMMENT '模型服务地址',
  `api_key` varchar(500) NOT NULL COMMENT 'API密钥',
  `default_parameters` json COMMENT '默认参数配置(JSON格式)',
  `supported_parameters` json COMMENT '支持的参数定义(JSON格式)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态:1-启用,0-禁用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) NOT NULL DEFAULT '0' COMMENT '逻辑删除标识：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_provider` (`provider`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型配置表';





-- ===================================================
-- Prompt模块测试数据
-- ===================================================



-- ===================================================
-- 3. prompt_build_template表测试数据
-- ===================================================

-- 对话式AI模板
INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('conversational_ai', 'chat,dialogue', '对话式AI模板',
        '你是一个{{role}}，具有以下特点：\n{{personality}}\n\n在与用户对话时，请遵循以下原则：\n1. {{principle_1}}\n2. {{principle_2}}\n3. {{principle_3}}\n\n用户：{{user_input}}\n\n请回复：',
        'role,personality,principle_1,principle_2,principle_3,user_input',
        '{"model": "qwen-max", "temperature": 0.7, "max_tokens": 2000}');

INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('社交媒体推销文案', 'social,goods,promotion', '社交媒体推销文案生成Prompt模板',
        '你是一个擅长撰写社交媒体文案的 AI 助手，请根据提供的产品信息生成一条适合发布在{{platform}}平台的推广文案。\n\n要求：\n\n1. 使用轻松、亲切的口吻，像朋友分享好物；\n\n2. 结尾添加相关话题标签，如 #好物推荐；',
        'platform',
        '{"model": "qwen-max", "temperature": 0.7, "max_tokens": 2000}');

INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('商品推广文案', 'goods,promotion', '商品推广Prompt模板',
        '请为以下商品写一段推广文案：商品名称：{{product_name}}  商品特点：{{features}}',
        'product_name,features',
        '{"model": "qwen-max", "temperature": 0.7, "max_tokens": 2000}');

-- 任务执行模板
INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('task_executor', 'task,execution', '任务执行模板',
        '你是一个专业的{{domain}}专家，请完成以下任务：\n\n## 任务描述\n{{task_description}}\n\n## 输入信息\n{{input_data}}\n\n## 输出要求\n{{output_requirements}}\n\n## 约束条件\n{{constraints}}\n\n请按要求完成任务：',
        'domain,task_description,input_data,output_requirements,constraints',
        '{"model": "qwen-max", "temperature": 0.3, "max_tokens": 3000}');

-- 分析报告模板
INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('analysis_report', 'analysis,report', '分析报告模板',
        '请对以下{{analysis_subject}}进行深入分析：\n\n## 分析对象\n{{subject_details}}\n\n## 分析维度\n{{analysis_dimensions}}\n\n## 参考标准\n{{reference_standards}}\n\n## 报告结构\n1. 摘要\n2. 详细分析\n3. 关键发现\n4. 结论和建议\n\n请生成完整的分析报告：',
        'analysis_subject,subject_details,analysis_dimensions,reference_standards',
        '{"model": "qwen-max", "temperature": 0.4, "max_tokens": 4000}');

-- 创意生成模板
INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('creative_generator', 'creative,generation', '创意生成模板',
        '请为{{project_type}}项目生成创意方案：\n\n## 项目背景\n{{background}}\n\n## 目标群体\n{{target_audience}}\n\n## 核心需求\n{{core_requirements}}\n\n## 创意约束\n{{creative_constraints}}\n\n## 输出要求\n- 提供3-5个不同的创意方向\n- 每个方向包含核心概念和执行要点\n- 评估可行性和预期效果\n\n请开始生成创意：',
        'project_type,background,target_audience,core_requirements,creative_constraints',
        '{"model": "qwen-max", "temperature": 0.9, "max_tokens": 3000}');

-- 问题解决模板
INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('problem_solver', 'problem,solution', '问题解决模板',
        '作为{{expert_role}}，请帮助解决以下问题：\n\n## 问题描述\n{{problem_description}}\n\n## 现状分析\n{{current_situation}}\n\n## 已尝试方案\n{{attempted_solutions}}\n\n## 限制条件\n{{limitations}}\n\n## 解决方案要求\n1. 分析问题根因\n2. 提供多个可选方案\n3. 评估方案的可行性和风险\n4. 推荐最优方案和实施步骤\n\n请提供解决方案：',
        'expert_role,problem_description,current_situation,attempted_solutions,limitations',
        '{"model": "qwen-max", "temperature": 0.5, "max_tokens": 3500}');

-- 教学辅导模板
INSERT INTO prompt_build_template (prompt_template_key, tags, template_desc, template, variables, model_config)
VALUES ('teaching_assistant', 'education,teaching', '教学辅导模板',
        '你是一位经验丰富的{{subject}}老师，请为学生提供学习指导：\n\n## 学生信息\n- 学习水平：{{student_level}}\n- 学习目标：{{learning_goal}}\n\n## 教学内容\n{{teaching_content}}\n\n## 学生问题\n{{student_question}}\n\n## 教学要求\n1. 用简单易懂的语言解释\n2. 提供具体的例子\n3. 给出练习建议\n4. 鼓励学生思考\n\n请开始教学：',
        'subject,student_level,learning_goal,teaching_content,student_question',
        '{"model": "qwen-max", "temperature": 0.6, "max_tokens": 2500}');

-- ===================================================
-- 4. evaluator_template表测试数据
-- ===================================================

-- 文本相似度评估模板
INSERT INTO evaluator_template (evaluator_template_key, template_desc, template, variables, model_config)
VALUES ('text_similarity', '文本相似度评估',
        '请评估以下两个文本的相似度，分数范围为0-1，保留两位小数。\n\n文本1：{{reference_output}}\n\n文本2：{{actual_output}}\n\n相似度分数：',
        'reference_output,actual_output',
        '{"modelId": "qwen-max", "temperature": 0.1, "max_tokens": 100}');

-- 代码质量评估模板
INSERT INTO evaluator_template (evaluator_template_key, template_desc, template, variables, model_config)
VALUES ('code_quality', '代码质量评估',
        '请评估以下代码的质量，从可读性、效率和最佳实践三个方面进行分析，并给出0-1的总分，保留两位小数。\n\n代码：\n{{code}}\n\n评估报告：',
        'code',
        '{"modelId": "qwen-max", "temperature": 0.2, "max_tokens": 1000}');

-- 情感分析评估模板
INSERT INTO evaluator_template (evaluator_template_key, template_desc, template, variables, model_config)
VALUES ('sentiment_analysis', '情感分析评估',
        '请分析以下文本的情感倾向，输出-1到1之间的情感分数，其中-1表示非常负面，0表示中性，1表示非常正面，保留两位小数。\n\n文本：{{text}}\n\n情感分数：',
        'text',
        '{"modelId": "qwen-max", "temperature": 0.1, "max_tokens": 200}');

