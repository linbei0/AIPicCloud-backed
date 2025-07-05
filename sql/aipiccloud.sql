create database if not exists aipiccloud;
use aipiccloud;
-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '用户昵称',
    userAvatar   varchar(1024)                          null comment '用户头像',
    userProfile  varchar(512)                           null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;
-- 图片表  
CREATE TABLE IF NOT EXISTS picture  
(  
    id           BIGINT AUTO_INCREMENT COMMENT 'id' PRIMARY KEY,  
    url          VARCHAR(512)                       NOT NULL COMMENT '图片 URL',  
    name         VARCHAR(128)                       NOT NULL COMMENT '图片名称',  
    thumbnailUrl VARCHAR(512) 						NULL COMMENT '缩略图 url';
    introduction VARCHAR(512)                       NULL COMMENT '简介',  
    category     VARCHAR(64)                        NULL COMMENT '分类',  
    tags         VARCHAR(512)                       NULL COMMENT '标签（JSON 数组）',  
    picSize      BIGINT                             NULL COMMENT '图片体积',  
    picWidth     INT                                NULL COMMENT '图片宽度',  
    picHeight    INT                                NULL COMMENT '图片高度',  
    picScale     DOUBLE                             NULL COMMENT '图片宽高比例',  
    picFormat    VARCHAR(32)                        NULL COMMENT '图片格式',
	picColor 	 VARCHAR(16)  						NULL COMMENT '图片主色调',
    userId       BIGINT                             NOT NULL COMMENT '创建用户 ID',
    spaceId  	 BIGINT  							NULL COMMENT '空间 id（为空表示公共空间）';  
    createTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',  
    editTime     DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '编辑时间',  
    updateTime   DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',  
    reviewStatus INT      DEFAULT 0                 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',  
    reviewMessage VARCHAR(512)                      NULL COMMENT '审核信息',  
    reviewerId    BIGINT                            NULL COMMENT '审核人 ID',  
    reviewTime    DATETIME                          NULL COMMENT '审核时间',  
    isDelete     TINYINT  DEFAULT 0                 NOT NULL COMMENT '是否删除',  
    INDEX idx_name (name),  
    INDEX idx_introduction (introduction),  
    INDEX idx_category (category),  
    INDEX idx_tags (tags),  
    INDEX idx_userId (userId),
    INDEX idx_picture (spaceId),  
    INDEX idx_reviewStatus (reviewStatus)  
) COMMENT '图片' COLLATE = utf8mb4_unicode_ci;
-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
	spaceType  int 		default 0 				  not null comment '空间类型：0-私有 1-团队';
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel), -- 提升按空间级别查询的效率
	index idx_spaceType (spaceType)
) comment '空间' collate = utf8mb4_unicode_ci;
-- 空间成员表
create table if not exists space_user
(
    id         bigint auto_increment comment 'id' primary key,
    spaceId    bigint                                 not null comment '空间 id',
    userId     bigint                                 not null comment '用户 id',
    spaceRole  varchar(128) default 'viewer'          null comment '空间角色：viewer/editor/admin',
    createTime datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;
