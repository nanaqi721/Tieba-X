-- =====================================================================
-- Teiba-X 数据库表结构
-- MySQL 8.x / InnoDB / utf8mb4
--
-- 通用约定：
--   1. 主键 id BIGINT，雪花ID，由应用侧生成（如 MyBatis-Plus ASSIGN_ID / Hutool IdUtil.getSnowflake）
--   2. 字符集 utf8mb4，排序规则 utf8mb4_0900_ai_ci
--   3. 所有内容表含 deleted 软删除标志（全部软删除，后台可查可恢复）
--   4. 不建物理外键，关联字段一律建普通索引，一致性由应用层保证
--   5. 计数均为冗余字段，互动操作在同一事务内同步更新冗余计数；
--      浏览量走 Redis 递增 + 定时任务落库（见 post_sql.md Q1）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 用户与权限模块
-- ---------------------------------------------------------------------

-- 用户（含全局角色：管理员 / 普通用户）
CREATE TABLE `user` (
  `id`                BIGINT       NOT NULL                COMMENT '雪花ID',
  `username`          VARCHAR(50)  NOT NULL                COMMENT '登录名',
  `password_hash`     VARCHAR(255) NOT NULL                COMMENT '密码哈希(BCrypt)',
  `nickname`          VARCHAR(50)  DEFAULT NULL            COMMENT '昵称(展示用，可改)',
  `email`             VARCHAR(100) DEFAULT NULL            COMMENT '邮箱(找回密码/通知，可空不唯一)',
  `bio`               VARCHAR(255) DEFAULT NULL            COMMENT '个人简介',
  `sex`               TINYINT      NOT NULL DEFAULT 0      COMMENT '性别 0保密 1男 2女',
  `avatar_url`        VARCHAR(255) DEFAULT NULL            COMMENT '头像',
  `role`              TINYINT      NOT NULL DEFAULT 0      COMMENT '全局角色 0普通 1管理员',
  `status`            TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0正常 1封禁',
  `post_count`        INT          NOT NULL DEFAULT 0      COMMENT '发帖数(冗余)',
  `comment_count`     INT          NOT NULL DEFAULT 0      COMMENT '回帖数(冗余，该用户所有评论含楼中楼)',
  `follower_count`    INT          NOT NULL DEFAULT 0      COMMENT '粉丝数(冗余)',
  `following_count`   INT          NOT NULL DEFAULT 0      COMMENT '关注用户数(冗余)',
  `followed_bar_count` INT         NOT NULL DEFAULT 0      COMMENT '关注吧数(冗余，follow.target_type=1)',
  `created_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`           TINYINT      NOT NULL DEFAULT 0      COMMENT '软删 0未删 1已删',
  PRIMARY KEY (`id`),
  -- 注意：username 唯一索引与软删冲突（删号后无法重注册）。若需可重注册，
  --       软删时改写唯一值为 `username + '#del_' + id`，或用 (username, deleted) 复合唯一。
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_status` (`status`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户';

-- 吧（版块）
CREATE TABLE `bar` (
  `id`             BIGINT       NOT NULL                COMMENT '雪花ID',
  `name`           VARCHAR(50)  NOT NULL                COMMENT '吧名',
  `description`    VARCHAR(255) DEFAULT NULL            COMMENT '吧简介',
  `avatar_url`     VARCHAR(255) DEFAULT NULL            COMMENT '吧图标',
  `creator_id`     BIGINT       NOT NULL                COMMENT '创建者 user.id',
  `status`         TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0正常 1封吧',
  `post_count`     INT          NOT NULL DEFAULT 0      COMMENT '帖子数(冗余)',
  `follower_count` INT          NOT NULL DEFAULT 0      COMMENT '关注数(冗余)',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`        TINYINT      NOT NULL DEFAULT 0      COMMENT '软删 0未删 1已删',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`),
  KEY `idx_creator` (`creator_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '吧';

-- 吧主关联（吧级角色：吧主 / 小吧主）
CREATE TABLE `bar_manager` (
  `id`         BIGINT   NOT NULL                        COMMENT '雪花ID',
  `bar_id`     BIGINT   NOT NULL                        COMMENT '吧 id',
  `user_id`    BIGINT   NOT NULL                        COMMENT '用户 id',
  `role`       TINYINT  NOT NULL DEFAULT 1              COMMENT '吧级角色 1吧主 2小吧主',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bar_user` (`bar_id`, `user_id`),
  KEY `idx_user` (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '吧主关联';

-- ---------------------------------------------------------------------
-- 内容模块
-- ---------------------------------------------------------------------

-- 帖子
CREATE TABLE `post` (
  `id`            BIGINT       NOT NULL                COMMENT '雪花ID',
  `user_id`       BIGINT       NOT NULL                COMMENT '作者 user.id',
  `bar_id`        BIGINT       NOT NULL                COMMENT '所属吧 bar.id',
  `title`         VARCHAR(30)  NOT NULL                COMMENT '标题(不超过30字)',
  `content`       MEDIUMTEXT   NOT NULL                COMMENT '图文正文(图片经 attachment 关联)',
  `cover_image`   VARCHAR(255) DEFAULT NULL            COMMENT '封面图',
  `is_top`        TINYINT      NOT NULL DEFAULT 0      COMMENT '置顶 0否 1是',
  `is_essence`    TINYINT      NOT NULL DEFAULT 0      COMMENT '加精 0否 1是',
  `status`        TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0正常 1审核中 2已删',
  `view_count`    INT          NOT NULL DEFAULT 0      COMMENT '浏览量(Redis递增+定时落库)',
  `comment_count` INT          NOT NULL DEFAULT 0      COMMENT '评论数(冗余)',
  `like_count`    INT          NOT NULL DEFAULT 0      COMMENT '点赞数(冗余)',
  `favorite_count` INT         NOT NULL DEFAULT 0      COMMENT '收藏数(冗余)',
  `last_reply_at` DATETIME     DEFAULT NULL            COMMENT '最后回复时间',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`       TINYINT      NOT NULL DEFAULT 0      COMMENT '软删 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_bar_created` (`bar_id`, `created_at` DESC),     -- 吧内帖子列表(默认时间排序)
  KEY `idx_bar_top` (`bar_id`, `is_top`),                  -- 吧内置顶/加精
  KEY `idx_bar_lastreply` (`bar_id`, `last_reply_at` DESC),-- 吧内按最后回复排序
  KEY `idx_user_created` (`user_id`, `created_at` DESC),   -- 某用户发布的帖子
  KEY `idx_status_created` (`status`, `created_at` DESC)   -- 管理后台审核/删除列表
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子';

-- 评论（parent_id 支持楼中楼）
CREATE TABLE `comment` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
  `post_id`    BIGINT       NOT NULL                COMMENT '所属帖子 post.id',
  `user_id`    BIGINT       NOT NULL                COMMENT '评论者 user.id',
  `parent_id`  BIGINT       DEFAULT NULL            COMMENT '父评论 id(NULL=顶层评论)',
  `content`    VARCHAR(2000) NOT NULL               COMMENT '评论内容',
  `like_count` INT          NOT NULL DEFAULT 0      COMMENT '点赞数(冗余)',
  `status`     TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0正常 1已删',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`    TINYINT      NOT NULL DEFAULT 0      COMMENT '软删 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_post` (`post_id`, `parent_id`, `created_at`),  -- 帖子评论列表 / 楼中楼
  KEY `idx_user` (`user_id`, `created_at`)                -- 某用户的评论
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '评论';

-- 标签
CREATE TABLE `tag` (
  `id`         BIGINT      NOT NULL                COMMENT '雪花ID',
  `name`       VARCHAR(50) NOT NULL                COMMENT '标签名',
  `post_count` INT         NOT NULL DEFAULT 0      COMMENT '帖子数(冗余)',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '标签';

-- 帖子-标签关联
CREATE TABLE `post_tag` (
  `id`         BIGINT   NOT NULL                    COMMENT '雪花ID',
  `post_id`    BIGINT   NOT NULL                    COMMENT '帖子 id',
  `tag_id`     BIGINT   NOT NULL                    COMMENT '标签 id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_tag` (`post_id`, `tag_id`),
  KEY `idx_tag` (`tag_id`, `post_id`)               -- 按标签查帖子
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '帖子标签关联';

-- 图片/附件（biz_type + biz_id 多态关联到帖子或评论）
CREATE TABLE `attachment` (
  `id`         BIGINT       NOT NULL                COMMENT '雪花ID',
  `biz_type`   TINYINT      NOT NULL                COMMENT '业务类型 1帖子 2评论',
  `biz_id`     BIGINT       NOT NULL                COMMENT '业务 id(post.id / comment.id)',
  `url`        VARCHAR(255) NOT NULL                COMMENT '文件访问地址',
  `file_name`  VARCHAR(255) DEFAULT NULL            COMMENT '原始文件名',
  `file_size`  BIGINT       DEFAULT NULL            COMMENT '文件大小(字节)',
  `width`      INT          DEFAULT NULL            COMMENT '图片宽度',
  `height`     INT          DEFAULT NULL            COMMENT '图片高度',
  `sort_order` INT          NOT NULL DEFAULT 0      COMMENT '排序',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_biz` (`biz_type`, `biz_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图片附件';

-- 吧内公告
CREATE TABLE `announcement` (
  `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
  `bar_id`       BIGINT       NOT NULL                COMMENT '所属吧 bar.id',
  `publisher_id` BIGINT       NOT NULL                COMMENT '发布者(吧主) user.id',
  `title`        VARCHAR(30)  NOT NULL                COMMENT '标题(不超过30字)',
  `content`      MEDIUMTEXT   NOT NULL                COMMENT '正文',
  `is_top`       TINYINT      NOT NULL DEFAULT 0      COMMENT '置顶 0否 1是',
  `status`       TINYINT      NOT NULL DEFAULT 0      COMMENT '状态 0正常 1已删',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`      TINYINT      NOT NULL DEFAULT 0      COMMENT '软删 0未删 1已删',
  PRIMARY KEY (`id`),
  KEY `idx_bar` (`bar_id`, `is_top`, `created_at`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '吧内公告';

-- ---------------------------------------------------------------------
-- 互动与关系模块
-- ---------------------------------------------------------------------

-- 点赞（表名避开 MySQL 保留字 like）
CREATE TABLE `post_like` (
  `id`          BIGINT   NOT NULL                    COMMENT '雪花ID',
  `user_id`     BIGINT   NOT NULL                    COMMENT '点赞者 user.id',
  `target_type` TINYINT  NOT NULL                    COMMENT '目标类型 1帖子 2评论',
  `target_id`   BIGINT   NOT NULL                    COMMENT '目标 id',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),  -- 防重复点赞(应用层用 INSERT IGNORE / ON DUPLICATE)
  KEY `idx_target` (`target_type`, `target_id`)                        -- 查某目标被谁点赞
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '点赞';

-- 关注（target_type 区分关注吧 / 关注用户）
CREATE TABLE `follow` (
  `id`          BIGINT   NOT NULL                    COMMENT '雪花ID',
  `user_id`     BIGINT   NOT NULL                    COMMENT '关注者 user.id',
  `target_type` TINYINT  NOT NULL                    COMMENT '目标类型 1吧 2用户',
  `target_id`   BIGINT   NOT NULL                    COMMENT '目标 id',
  `created_at`  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_target` (`user_id`, `target_type`, `target_id`),  -- 防重复关注
  KEY `idx_target` (`target_type`, `target_id`)                        -- 查某目标的粉丝
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '关注';

-- 收藏
CREATE TABLE `favorite` (
  `id`         BIGINT   NOT NULL                    COMMENT '雪花ID',
  `user_id`    BIGINT   NOT NULL                    COMMENT '用户 id',
  `post_id`    BIGINT   NOT NULL                    COMMENT '帖子 id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_post` (`user_id`, `post_id`),  -- 防重复收藏
  KEY `idx_post` (`post_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏';

-- 私信
CREATE TABLE `message` (
  `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
  `from_user_id` BIGINT       NOT NULL                COMMENT '发送者 user.id',
  `to_user_id`   BIGINT       NOT NULL                COMMENT '接收者 user.id',
  `content`      VARCHAR(2000) NOT NULL               COMMENT '内容',
  `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读 0未读 1已读',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_conv` (`from_user_id`, `to_user_id`, `created_at`),  -- 单方会话记录
  KEY `idx_to_unread` (`to_user_id`, `is_read`)                -- 未读查询
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '私信';

-- 通知
CREATE TABLE `notification` (
  `id`           BIGINT       NOT NULL                COMMENT '雪花ID',
  `user_id`      BIGINT       NOT NULL                COMMENT '接收者 user.id',
  `type`         TINYINT      NOT NULL                COMMENT '类型 1被回复 2被点赞 3被关注 4系统',
  `from_user_id` BIGINT       DEFAULT NULL            COMMENT '触发者 user.id',
  `target_type`  TINYINT      DEFAULT NULL            COMMENT '目标类型(可选)',
  `target_id`    BIGINT       DEFAULT NULL            COMMENT '目标 id(可选)',
  `content`      VARCHAR(255) DEFAULT NULL            COMMENT '通知摘要',
  `is_read`      TINYINT      NOT NULL DEFAULT 0      COMMENT '是否已读 0未读 1已读',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_unread` (`user_id`, `is_read`, `created_at`)  -- 未读通知列表
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '通知';

-- 举报
CREATE TABLE `report` (
  `id`          BIGINT       NOT NULL                COMMENT '雪花ID',
  `reporter_id` BIGINT       NOT NULL                COMMENT '举报者 user.id',
  `target_type` TINYINT      NOT NULL                COMMENT '目标类型 1帖子 2评论',
  `target_id`   BIGINT       NOT NULL                COMMENT '目标 id',
  `reason`      VARCHAR(500) DEFAULT NULL            COMMENT '举报理由',
  `status`      TINYINT      NOT NULL DEFAULT 0      COMMENT '处理状态 0待处理 1已处理 2驳回',
  `handled_by`  BIGINT       DEFAULT NULL            COMMENT '处理人 user.id',
  `handled_at`  DATETIME     DEFAULT NULL            COMMENT '处理时间',
  `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`, `created_at`)          -- 后台处理队列
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '举报';
