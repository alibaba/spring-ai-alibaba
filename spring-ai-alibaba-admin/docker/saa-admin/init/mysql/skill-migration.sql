-- Incremental migration for existing deployments
CREATE TABLE IF NOT EXISTS `skill`
(
    `id`           BIGINT(20) UNSIGNED AUTO_INCREMENT NOT NULL COMMENT 'pk',
    `skill_id`     VARCHAR(64)                        NOT NULL COMMENT 'biz id',
    `workspace_id` VARCHAR(64)                        NOT NULL COMMENT 'workspace id',
    `name`         VARCHAR(255)                       NOT NULL COMMENT 'display name',
    `description`  VARCHAR(4096)                               DEFAULT NULL COMMENT 'skill description',
    `skill_name`   VARCHAR(64)                        NOT NULL COMMENT 'SKILL.md name field',
    `storage_path` VARCHAR(1024)                      NOT NULL COMMENT 'extracted skill root path',
    `status`       TINYINT(4)                         NOT NULL DEFAULT 1 COMMENT 'status: 0-deleted, 1-normal',
    `source`       VARCHAR(64)                        NOT NULL DEFAULT 'user' COMMENT 'skill source',
    `gmt_create`   DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `gmt_modified` DATETIME                           NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `creator`      VARCHAR(64)                        NOT NULL COMMENT 'creator uid',
    `modifier`     VARCHAR(64)                        NOT NULL COMMENT 'modifier uid',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_skill_id` (`skill_id`),
    KEY `idx_workspace_status` (`workspace_id`, `status`),
    KEY `idx_workspace_skill_name` (`workspace_id`, `skill_name`)
) ENGINE = InnoDB
  AUTO_INCREMENT = 10000
  DEFAULT CHARSET = utf8mb4
    COMMENT ='agent skill info';
