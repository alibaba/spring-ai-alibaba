create table memory
(
    id           bigint auto_increment comment 'id',
    conversation_id  varchar(256)                       null comment 'conversationId' primary key,
    messages     varchar(6144)                      null comment 'JSON of messages'
)