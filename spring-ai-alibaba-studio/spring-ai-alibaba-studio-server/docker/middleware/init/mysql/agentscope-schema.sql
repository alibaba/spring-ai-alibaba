/******************************************/
/*   table = account                      */
/******************************************/
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account`
(

    `id`             BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `account_id`     VARCHAR(64)                        NOT NULL COMMENT 'account id',
    `username`       VARCHAR(255)                       NOT NULL COMMENT 'account name',
    `email`          VARCHAR(255)                                DEFAULT NULL COMMENT 'account email',
    `mobile`         VARCHAR(255)                                DEFAULT NULL COMMENT 'account mobile',
    `password`       VARCHAR(255)                       NOT NULL COMMENT 'password',
    `nickname`       VARCHAR(255)                                DEFAULT NULL COMMENT 'nickname',
    `icon`           VARCHAR(255)                                DEFAULT NULL COMMENT 'account icon',
    `type`           VARCHAR(64)                        NOT NULL COMMENT 'type: basic, admin',
    `status`         TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0- deleted, 1- normal',
    `gmt_create`     DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_last_login` DATETIME                                    DEFAULT NULL COMMENT 'account last login time',
    `creator`        VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`       VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_id` (`account_id`),
    KEY `idx_email_password` (`username`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='account info';

/******************************************/
/*   table = application                  */
/******************************************/
DROP TABLE IF EXISTS `application`;
CREATE TABLE `application`
(
    `id`           BIGINT UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `workspace_id` VARCHAR(64)                    NOT NULL COMMENT 'workspace id',
    `app_id`       VARCHAR(64)                    NOT NULL COMMENT 'app id',
    `name`         VARCHAR(255)                   NOT NULL COMMENT 'app name',
    `description`  VARCHAR(4096)                           DEFAULT NULL COMMENT 'app description',
    `icon`         VARCHAR(255)                            DEFAULT NULL COMMENT 'app icon',
    `source`       VARCHAR(64)                    NOT NULL COMMENT 'app source',
    `type`         VARCHAR(64)                    NOT NULL COMMENT 'type, agent, workflow',
    `status`       TINYINT(4)                     NOT NULL DEFAULT 1 COMMENT 'status, 0-deleted 1-draft; 2-published; 3-publishedEditing',
    `gmt_create`   DATETIME                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                    NOT NULL COMMENT '创建者uid',
    `modifier`     VARCHAR(64)                    NOT NULL COMMENT '修改者uid',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_app_id` (`app_id`),
    KEY `idx_workspace_type` (`workspace_id`, `type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='app info';

/******************************************/
/*   table = application_version          */
/******************************************/
DROP TABLE IF EXISTS `application_version`;
CREATE TABLE `application_version`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `app_id`       VARCHAR(64)                        NOT NULL COMMENT 'app id',
    `workspace_id` VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `config`       LONGTEXT                                    DEFAULT NULL COMMENT 'app config',
    `status`       TINYINT(4)                         NOT NULL COMMENT 'status, 0-deleted 1-draft; 2-published; 3-publishedEditing',
    `version`      VARCHAR(32)                        NOT NULL DEFAULT '0.0.1' COMMENT 'version name',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'version description',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (id),
    KEY `idx_workspace_app_version` (`workspace_id`, `app_id`, `version`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='app version info';

/******************************************/
/*   table = workspace                    */
/******************************************/
DROP TABLE if exists `workspace`;
CREATE TABLE `workspace`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `workspace_id` VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `account_id`   VARCHAR(64)                        NOT NULL COMMENT 'account id',
    `status`       TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `name`         VARCHAR(255)                       NOT NULL COMMENT 'workspace name',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'workspace description',
    `config`       TEXT                                        DEFAULT NULL COMMENT 'workspace config',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_workspace_id` (`workspace_id`),
    KEY `idx_account_id` (`account_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='workspace info';

/******************************************/
/*   table = api_key                       */
/******************************************/
DROP TABLE IF EXISTS `api_key`;
CREATE TABLE `api_key`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `account_id`   VARCHAR(64)                        NOT NULL COMMENT 'uid',
    `api_key`      VARCHAR(512)                       NOT NULL COMMENT 'api key',
    `status`       TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'api key description',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_api_key` (`api_key`),
    KEY `idx_account_id` (`account_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='api key info';

/******************************************/
/*   table = plugin                       */
/******************************************/
DROP TABLE IF EXISTS `plugin`;
CREATE TABLE `plugin`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `plugin_id`    VARCHAR(64)                        NOT NULL COMMENT 'biz id',
    `workspace_id` VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `type`         VARCHAR(64)                        NOT NULL COMMENT 'type: 1: official, 2: custom',
    `status`       TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `name`         VARCHAR(255)                       NOT NULL COMMENT 'plugin name',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'plugin description',
    `config`       TEXT                                        DEFAULT NULL COMMENT 'plugin config',
    `source`       VARCHAR(64)                        NOT NULL COMMENT 'plugin source',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_plugin_id` (`plugin_id`),
    KEY `idx_workspace_type` (`workspace_id`, `type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='plugin info';


/******************************************/
/*   table = tool                         */
/******************************************/
DROP TABLE IF EXISTS `tool`;
CREATE TABLE `tool`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `plugin_id`    VARCHAR(64)                        NOT NULL COMMENT 'plugin id',
    `tool_id`      VARCHAR(64)                        NOT NULL COMMENT 'tool id',
    `workspace_id` VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `status`       TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `enabled`      TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'enabled: 0-disabled, 1-enabled',
    `test_status`  TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'test status: 1: not tested, 2: passed, 3: failed',
    `name`         VARCHAR(255)                       NOT NULL COMMENT 'tool name',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'tool description',
    `config`       LONGTEXT                           NOT NULL COMMENT 'tool config',
    `api_schema`   LONGTEXT                           NOT NULL COMMENT 'tool api schema',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_tool_id` (`tool_id`),
    KEY `idx_workspace_plugin` (`workspace_id`, `plugin_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='tool info';

/******************************************/
/*   table = knowledge_base                      */
/******************************************/
DROP TABLE IF EXISTS `knowledge_base`;
CREATE TABLE `knowledge_base`
(
    `id`             BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `workspace_id`   VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `kb_id`          VARCHAR(64)                        NOT NULL COMMENT 'knowledge base id',
    `type`           VARCHAR(64)                        NOT NULL COMMENT 'unstructured',
    `status`         TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `name`           VARCHAR(255)                       NOT NULL COMMENT 'knowledge base name',
    `description`    VARCHAR(4096)                               DEFAULT NULL COMMENT 'knowledge base description',
    `process_config` TEXT                                        DEFAULT NULL COMMENT 'process config',
    `index_config`   TEXT                                        DEFAULT NULL COMMENT 'index config',
    `search_config`  TEXT                                        DEFAULT NULL COMMENT 'search config',
    `total_docs`     BIGINT(20) UNSIGNED                NOT NULL DEFAULT 0 comment 'total docs count',
    `gmt_create`     DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`        VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`       VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_kb_id` (`kb_id`),
    KEY `idx_workspace_status_name` (`workspace_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='knowledge base info';

/******************************************/
/*   table = document                      */
/******************************************/
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document`
(
    `id`             BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `workspace_id`   VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `kb_id`          VARCHAR(64)                        NOT NULL COMMENT 'knowledge base id',
    `doc_id`         VARCHAR(64)                        NOT NULL COMMENT 'document id',
    `type`           varchar(64)                        NOT NULL COMMENT 'type: file, url',
    `status`         TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `enabled`        TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'enabled: 0-disabled, 1-enabled',
    `name`           VARCHAR(255)                       NOT NULL COMMENT 'document name',
    `format`         VARCHAR(64)                        NOT NULL COMMENT 'document format',
    `size`           BIGINT(20)                         NOT NULL DEFAULT 0 COMMENT 'document size',
    `metadata`       TEXT                                        DEFAULT NULL COMMENT 'document metadata',
    `index_status`   TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'Index status: 1: pending, 2: processing, 3: completed',
    `path`           VARCHAR(512)                       NOT NULL COMMENT 'storage path',
    `parsed_path`    VARCHAR(512)                                DEFAULT NULL COMMENT 'parsed path',
    `process_config` TEXT                                        DEFAULT NULL COMMENT 'document chunk config',
    `source`         VARCHAR(255)                                DEFAULT NULL COMMENT 'doc source',
    `error`          TEXT                                        DEFAULT NULL COMMENT '',
    `gmt_create`     TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified`   TIMESTAMP                          NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`        VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`       VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (id),
    UNIQUE KEY `uk_document_id` (`doc_id`),
    KEY `idx_workspace_kb` (`workspace_id`, `kb_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='document info';

/******************************************/
/*   DatabaseName = agentscope   */
/*   TableName = application_component   */
/******************************************/
DROP TABLE IF EXISTS `application_component`;
CREATE TABLE `application_component`
(
    `id`           bigint unsigned NOT NULL AUTO_INCREMENT COMMENT 'pk',
    `gmt_create`   datetime        NOT NULL COMMENT 'create time',
    `gmt_modified` datetime        NOT NULL COMMENT 'modified time',
    `code`         varchar(64)     NOT NULL COMMENT 'component code',
    `name`         varchar(128)    NOT NULL COMMENT 'name',
    `workspace_id` varchar(64)     NOT NULL COMMENT 'workspace id',
    `type`         varchar(64)     NOT NULL COMMENT 'type, agent, workflow',
    `app_id`       varchar(64)   DEFAULT NULL,
    `config`       longtext COMMENT 'component config',
    `description`  varchar(4096) DEFAULT NULL COMMENT 'description ',
    `status`       tinyint       DEFAULT NULL COMMENT 'status：0-deleted, 1, normal, 2-published',
    `creator`      varchar(64)   DEFAULT NULL COMMENT 'creator uid',
    `modifier`     varchar(64)   DEFAULT NULL COMMENT 'modifier uid',
    `need_update`  tinyint       DEFAULT NULL COMMENT '0-no need update, 1-need update',
    PRIMARY KEY (`id`),
    KEY `idx_workspace_type_status_appcode` (`workspace_id`, `type`, `app_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='app component info';

/******************************************/
/*   table = reference                    */
/******************************************/
DROP TABLE IF EXISTS `reference`;
CREATE TABLE `reference`
(
    `id`           BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'pk',
    `gmt_create`   DATETIME            NOT NULL COMMENT 'create time',
    `gmt_modified` DATETIME            NOT NULL COMMENT 'modified time',
    `main_code`    VARCHAR(64)         NOT NULL COMMENT 'entity code',
    `main_type`    TINYINT             NOT NULL COMMENT 'entity time',
    `refer_code`   VARCHAR(64)         NOT NULL COMMENT 'refer code',
    `refer_type`   TINYINT             NOT NULL COMMENT 'refer type',
    `workspace_id` VARCHAR(64)         NOT NULL DEFAULT '1' COMMENT 'workspace id',
    PRIMARY KEY (`id`),
    KEY `idx_refer_code` (`refer_code`),
    KEY `idx_main_code_workspace_type` (`workspace_id`, `main_code`, `main_type`)
) DEFAULT CHARACTER SET = utf8mb4 COMMENT ='reference info';


/******************************************/
/*   table = mcp_server                   */
/******************************************/
DROP TABLE IF EXISTS `mcp_server`;
CREATE TABLE `mcp_server`
(
    `id`            BIGINT(20) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'pk',
    `gmt_create`    DATETIME            NOT NULL COMMENT 'create time',
    `gmt_modified`  DATETIME            NOT NULL COMMENT 'modified time',
    `server_code`   VARCHAR(64)         NOT NULL COMMENT 'server code',
    `name`          VARCHAR(64)         NOT NULL COMMENT 'server name',
    `description`   VARCHAR(1024)       NULL COMMENT 'description',
    `source`        VARCHAR(128)        NULL COMMENT 'server source',
    `deploy_env`    VARCHAR(16)         NULL COMMENT 'deploy environment local/remote',
    `type`          VARCHAR(32)         NOT NULL COMMENT 'server type OFFICIAL/CUSTOMER',
    `deploy_config` TEXT                NOT NULL COMMENT 'deploy config',
    `workspace_id`  VARCHAR(64)         NULL COMMENT 'workspace id',
    `account_id`    VARCHAR(64)         NULL COMMENT 'uid',
    `status`        TINYINT             NOT NULL COMMENT 'status 0 unable 1 normal 3 deleted',
    `biz_type`      VARCHAR(512)        NULL COMMENT 'biz type',
    `detail_config` TEXT                NULL COMMENT 'server detail',
    `host`          VARCHAR(1024)       NULL COMMENT 'host address',
    `install_type`  VARCHAR(32)         NULL COMMENT 'install_type npx/uvx/sse',
    PRIMARY KEY (`id`),
    KEY `idx_code` (`server_code`),
    KEY `idx_server_name` (`name`),
    KEY `idx_name_status` (`status`, `name`),
    KEY `idx_type` (`type`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='mcp server info';

/******************************************/
/*   DatabaseName = agentscope   */
/*   TableName = provider   */
/******************************************/
CREATE TABLE `provider`
(
    `id`                    bigint       NOT NULL AUTO_INCREMENT COMMENT 'pk',
    `workspace_id`          varchar(64)           DEFAULT NULL COMMENT 'workspace id',
    `icon`                  varchar(255)          DEFAULT NULL COMMENT 'provider icon',
    `name`                  varchar(255)          DEFAULT NULL COMMENT 'provider name',
    `description`           varchar(1024)         DEFAULT NULL COMMENT 'provider description',
    `provider`              varchar(255) NOT NULL COMMENT 'provider',
    `enable`                tinyint(1)            DEFAULT '1' COMMENT 'enable, 0: disabled, 1: enabled',
    `source`                varchar(64)  NOT NULL DEFAULT 'preset' COMMENT 'source, preset, custom',
    `credential`            varchar(1024)         DEFAULT NULL COMMENT 'access credential，json',
    `supported_model_types` varchar(255)          DEFAULT NULL COMMENT 'model type',
    `protocol`              varchar(64)           DEFAULT NULL COMMENT 'protocol, openai',
    `gmt_create`            datetime              DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `gmt_modified`          datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
    `creator`               varchar(64)           DEFAULT NULL COMMENT 'creator',
    `modifier`              varchar(64)           DEFAULT NULL COMMENT 'modifier',
    PRIMARY KEY (`id`),
    KEY `idx_account_workspace_provider` (`workspace_id`, `provider`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='provider info';


/******************************************/
/*   DatabaseName = agentscope   */
/*   TableName = model   */
/******************************************/
CREATE TABLE `model`
(
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT 'pk',
    `workspace_id` varchar(64)           DEFAULT NULL COMMENT 'workspace id',
    `icon`         varchar(255)          DEFAULT NULL COMMENT 'icon',
    `name`         varchar(100)          DEFAULT NULL COMMENT 'model name',
    `type`         varchar(100)          DEFAULT 'LLM' COMMENT 'model type: LLM',
    `mode`         varchar(100)          DEFAULT 'chat' COMMENT 'mode',
    `model_id`     varchar(100) NOT NULL COMMENT 'model id',
    `provider`     varchar(100) NOT NULL COMMENT 'provider',
    `enable`       tinyint(1)            DEFAULT '1' COMMENT 'enable, 0: disabled, 1: enabled',
    `tags`         varchar(255)          DEFAULT NULL COMMENT 'tags',
    `source`       varchar(100) NOT NULL DEFAULT 'preset' COMMENT 'source, preset, custom',
    `gmt_create`   datetime              DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `gmt_modified` datetime              DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'modified time',
    `creator`      varchar(64)           DEFAULT NULL COMMENT 'creator',
    `modifier`     varchar(64)           DEFAULT NULL COMMENT 'modifier',
    PRIMARY KEY (`id`),
    KEY `idx_account_workspace_provider_model` (`workspace_id`, `provider`, `model_id`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='model info';

#init account
INSERT INTO account (account_id, username, email, mobile, password, type, status, gmt_create,
                     gmt_modified, creator, modifier)
VALUES ('10000', 'saa', 'ken.lj.hz@gmail.com', null,
        '$argon2id$v=19$m=66536,t=2,p=1$KSDQowfZxDjKLqBtxFNRng$znU0oQFQs2shR9la4S11n7d0LpGApmSBXvDOXuhbR40', 'admin', 1,
        now(), now(), '10000', '10000');

#init workspace
INSERT INTO workspace (workspace_id, account_id, status, name, description, config, gmt_create, gmt_modified,
                                  creator, modifier)
VALUES ('1', '10000', 1, 'Default Workspace', 'Default Workspace', null, now(), now(), '10000', '10000');

#init model
INSERT INTO provider (workspace_id, icon, name, description, provider, enable, source, credential,
                                 supported_model_types, protocol, gmt_create, gmt_modified, creator, modifier)
VALUES ( '1', null, 'Tongyi', 'Tongyi', 'Tongyi', 1, 'preset','{"endpoint":"https://dashscope.aliyuncs.com/compatible-mode"}',
        null, 'OpenAI', now(), now(), null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-max','llm','chat','qwen-max','Tongyi',1,'web_search,function_call','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-max-latest','llm','chat','qwen-max-latest','Tongyi',1,'web_search,function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-plus','llm','chat','qwen-plus','Tongyi',1,'web_search,function_call','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-plus-latest','llm','chat','qwen-plus-latest','Tongyi',1,'web_search,function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-turbo','llm','chat','qwen-turbo','Tongyi',1,'web_search,function_call','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-turbo-latest','llm','chat','qwen-turbo-latest','Tongyi',1,'web_search,function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-235b-a22b','llm','chat','qwen3-235b-a22b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-30b-a3b','llm','chat','qwen3-30b-a3b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-32b','llm','chat','qwen3-32b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-14b','llm','chat','qwen3-14b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-8b','llm','chat','qwen3-8b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-4b','llm','chat','qwen3-4b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-1.7b','llm','chat','qwen3-1.7b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen3-0.6b','llm','chat','qwen3-0.6b','Tongyi',1,'function_call,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-vl-max','llm','chat','qwen-vl-max','Tongyi',1,'vision,function_call','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwen-vl-plus','llm','chat','qwen-vl-plus','Tongyi',1,'vision,function_call','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qvq-max','llm','chat','qvq-max','Tongyi',1,'vision,reasoning','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'qwq-plus','llm','chat','qwq-plus','Tongyi',1,'reasoning,function_call','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'text-embedding-v1','text_embedding','chat','text-embedding-v1','Tongyi',1,'embedding','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'text-embedding-v2','text_embedding','chat','text-embedding-v2','Tongyi',1,'embedding','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'text-embedding-v3','text_embedding','chat','text-embedding-v3','Tongyi',1,'embedding','preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'gte-rerank-v2','rerank','chat','gte-rerank-v2','Tongyi',1,null,'preset',now(),now(),null,null);

INSERT INTO `model` (`workspace_id`,`icon`,`name`,`type`,`mode`,`model_id`,`provider`,`enable`,`tags`,`source`,`gmt_create`,`gmt_modified`,`creator`,`modifier`) VALUES ('1',null,'deepseek-r1','llm','chat','deepseek-r1','Tongyi',1,'reasoning','preset',now(),now(),null,null);

/******************************************/
/*   table = agent_schema                 */
/******************************************/
DROP TABLE IF EXISTS `agent_schema`;
CREATE TABLE `agent_schema`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `agent_id`     VARCHAR(64)                                 DEFAULT NULL COMMENT 'agent id',
    `workspace_id` VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `name`         VARCHAR(255)                       NOT NULL COMMENT 'agent name',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'agent description',
    `type`         VARCHAR(64)                        NOT NULL COMMENT 'agent type: ReactAgent, ParallelAgent, SequentialAgent, LLMRoutingAgent, LoopAgent',
    `instruction`  TEXT                                        DEFAULT NULL COMMENT 'system instruction',
    `input_keys`   TEXT                                        DEFAULT NULL COMMENT 'input keys JSON',
    `output_key`   VARCHAR(255)                                DEFAULT NULL COMMENT 'output key',
    `handle`       LONGTEXT                                    DEFAULT NULL COMMENT 'handle configuration JSON',
    `sub_agents`   LONGTEXT                                    DEFAULT NULL COMMENT 'sub agents configuration JSON',
    `yaml_schema`  LONGTEXT                                    DEFAULT NULL COMMENT 'generated YAML schema',
    `status`       VARCHAR(64)                        NOT NULL DEFAULT 'DRAFT' COMMENT 'agent status: DRAFT, PUBLISHED, ARCHIVED',
    `enabled`      TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'enabled: 0-disabled, 1-enabled',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_agent_id` (`agent_id`),
    KEY `idx_workspace_type` (`workspace_id`, `type`),
    KEY `idx_workspace_status` (`workspace_id`, `status`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='agent schema info';
