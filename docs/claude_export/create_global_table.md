“请扮演一名资深数据库架构师。我们现在要设计一套数据库表结构，但我需要你采用‘引导式讨论’的方式，而不是直接给我最终答案。

请严格遵守以下步骤：
第一步： 先向我提问，了解业务背景、核心实体、关键业务流程和查询频率（读多写少还是写多读少），不要生成任何建表语句。
第二步： 在我回答后，基于需求给出概念模型（即有哪些实体和关系），并向我确认是否有遗漏。
第三步： 确认无误后，再给出详细的字段设计、类型建议和索引策略。

现在，请先向我提出你需要的业务背景信息，我们一步步来。”

● 我先快速了解一下这个项目（Teiba-X）的现状和技术栈，这样能问出更有针对性的业务问题。

● Explore(探索 Teiba-X 项目结构)
  ⎿  Done (14 tool uses · 16.0k tokens · 21s)
  (ctrl+o to expand)

● 这个项目目前基本是空骨架（Java 17 + Maven 意向，无源码、无 pom.xml），只有一份设计笔记
  docs/sql_design/post_sql.md，提到了「帖子表」以及浏览量用 Redis 缓存 + 定时落库的思路。项目名 Teiba-X
  看起来是贴吧类社区。

  按你的第一步要求，我只提问、不下结论。以下是我需要确认的几个业务维度。

● User answered Claude's questions:
  ⎿  · Teiba-X 的核心功能范围是什么？（这决定了实体数量和表规模） → 完整社区
     · 系统的读写特性（查询频率）大致是？ → 读写均衡
     · 目标数据规模与并发量级预期是？（影响是否需要分库分表等设计） → 中型
     · 用户体系与权限模型怎么设计？ → 完整权限

● 收到。基于你的回答（完整社区 / 读写均衡 / 中型规模 / 完整权限），进入第二步：给出概念模型（实体与关系），先不做字段级设计。

  ---
  概念模型（实体 + 关系）  
  核心实体（约 11 类）


  ┌─────┬───────────────────┬────────────────────────────────────────────────────────────────────┐
  │  #  │       实体        │                              职责要点                              │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 1   │ 用户 User         │ 账号、昵称、头像、简介、状态（封禁等）                             │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 2   │ 吧 Bar            │ 版块：吧名、简介、创建者、状态                                     │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 3   │ 帖子 Post         │ 作者、所属吧、标题、正文、浏览量等冗余计数                         │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 4   │ 评论 Comment      │ 作者、所属帖子、父评论（楼中楼）                                   │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 5   │ 点赞 Like         │ 用户对帖子/评论的表态（多态对象）                                  │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 6   │ 关注 Follow       │ 关注吧 + 关注用户（两类关系）                                      │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 7   │ 收藏 Favorite     │ 用户收藏帖子                                                       │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 8   │ 私信 Message      │ 发送者/接收者                                                      │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 9   │ 通知 Notification │ 互动通知（被回复/点赞/关注）+ 系统通知                             │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 10  │ 举报 Report       │ 举报者 + 举报对象（帖子/评论，多态）+ 处理状态                     │
  ├─────┼───────────────────┼────────────────────────────────────────────────────────────────────┤
  │ 11  │ 角色权限 Role     │ 全局角色（管理员/普通用户）+ 吧级角色（吧主关联表：bar+user+role） │
  └─────┴───────────────────┴────────────────────────────────────────────────────────────────────┘

  实体间关系
  - User 1—N：Post / Comment / Message / Report / Notification（作者/操作者）
  - Bar 1—N：Post（帖子必须属于一个吧）
  - Post 1—N：Comment；Comment 自引用 1—N（楼中楼）
  - User N—M Post：经 Like / Favorite 关系表
  - User N—M Bar：经 Follow（关注吧）
  - User N—M User：经 Follow（关注用户，自引用多对多）
  - User N—M Bar：经吧主关联表（带吧主角色属性，一个吧可多个吧主）

  关键业务约束（进入字段设计前需定的规则）
  - 点赞 / 收藏 / 关注需唯一约束（user + 对象），防止重复
  - 帖子/吧上的计数冗余：帖数、回帖数、点赞数、收藏数
  - 浏览量沿用你的方案：帖子表存冗余字段，Redis 递增 + 定时落库

  ---
  在确认这套模型之前，我想先核实几个可能的遗漏点和影响表结构的决策。
● User answered Claude's questions:
  ⎿  · 除上述 11 类实体外，以下哪些也需要纳入？（勾选你需要的，不需要的可跳过） → 标签/话题, 吧内公告/置顶, 图片/附件
     · 帖子正文的形式是？（决定 Post 字段类型和是否需要附件表） → 图文（含附件）
     · 被删除内容（帖子/评论）的处理策略？（影响 deleted 标志位设计） → 全部软删除

● 已确认。概念模型更新后为：11 类核心实体 + 标签/话题 + 吧内公告/置顶 + 图片/附件（黑名单不纳入）；正文采用图文（含附件）；删除策略全部软删除。

  进入第三步（详细字段设计）之前，还有 3 个技术前提会直接决定字段类型和索引写法的细节，先确认一下。

● User answered Claude's questions:
  ⎿  · 具体数据库用哪个？ → MySQL 8.x
     · 主键采用什么策略？（影响 id 字段类型与生成方式） → 雪花ID
     · 「完整社区」里的搜索功能怎么实现？（决定是否需额外索引架构） → 数据库 LIKE（初期）

● 技术前提已确定：MySQL 8.x / 雪花ID（BIGINT，应用侧生成）/ 搜索初期用 LIKE。

  现在进入第三步：详细字段设计、类型建议与索引策略。

  ---
  通用约定（适用于所有表）

  - 引擎 InnoDB，字符集 utf8mb4，排序规则 utf8mb4_0900_ai_ci
  - 主键 id BIGINT（雪花 ID，应用侧生成，如 MyBatis-Plus ASSIGN_ID / Hutool IdUtil.getSnowflake）
  - 每张表含 created_at / updated_at DATETIME；内容表含 deleted TINYINT NOT NULL DEFAULT 0（全部软删除）
  - 不建物理外键（应用层保证一致性），关联字段一律建普通索引

  用户与权限模块

  -- 用户
  CREATE TABLE `user` (
    `id` BIGINT NOT NULL COMMENT '雪花ID',
    `username` VARCHAR(50) NOT NULL COMMENT '登录名',
    `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
    `nickname` VARCHAR(50) DEFAULT NULL,
    `avatar_url` VARCHAR(255) DEFAULT NULL,
    `bio` VARCHAR(255) DEFAULT NULL,
    `role` TINYINT NOT NULL DEFAULT 0 COMMENT '全局角色 0普通 1管理员',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1封禁',
    `post_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `follower_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `following_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_status` (`status`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

  -- 吧（版块）
  CREATE TABLE `bar` (
    `id` BIGINT NOT NULL, `name` VARCHAR(50) NOT NULL, `description` VARCHAR(255) DEFAULT NULL,
    `avatar_url` VARCHAR(255) DEFAULT NULL, `creator_id` BIGINT NOT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1封吧',
    `post_count` INT NOT NULL DEFAULT 0 COMMENT '冗余', `follower_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_name` (`name`), KEY `idx_creator` (`creator_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='吧';

  -- 吧主（吧级角色）
  CREATE TABLE `bar_manager` (
    `id` BIGINT NOT NULL, `bar_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '1吧主 2小吧主',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_bar_user` (`bar_id`,`user_id`), KEY `idx_user` (`user_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='吧主关联';

  内容模块

  -- 帖子
  CREATE TABLE `post` (
    `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL COMMENT '作者', `bar_id` BIGINT NOT NULL COMMENT '所属吧',
    `title` VARCHAR(200) NOT NULL, `content` MEDIUMTEXT NOT NULL COMMENT '图文正文',
    `cover_image` VARCHAR(255) DEFAULT NULL,
    `is_top` TINYINT NOT NULL DEFAULT 0 COMMENT '置顶', `is_essence` TINYINT NOT NULL DEFAULT 0 COMMENT '加精',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1审核中 2已删',
    `view_count` INT NOT NULL DEFAULT 0 COMMENT '浏览量(Redis落库)',
    `comment_count` INT NOT NULL DEFAULT 0 COMMENT '冗余', `like_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `favorite_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `last_reply_at` DATETIME DEFAULT NULL COMMENT '最后回复时间',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_bar_created` (`bar_id`,`created_at` DESC),        -- 吧内列表(默认排序)
    KEY `idx_bar_top` (`bar_id`,`is_top`),                      -- 置顶/加精查询
    KEY `idx_bar_lastreply` (`bar_id`,`last_reply_at` DESC),    -- 按最后回复排序
    KEY `idx_user_created` (`user_id`,`created_at` DESC),       -- 某用户的帖子
    KEY `idx_status_created` (`status`,`created_at` DESC)       -- 管理后台
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子';

  -- 评论（楼中楼）
  CREATE TABLE `comment` (
    `id` BIGINT NOT NULL, `post_id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
    `parent_id` BIGINT DEFAULT NULL COMMENT '楼中楼父评论',
    `content` VARCHAR(2000) NOT NULL, `like_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0正常 1已删',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_post` (`post_id`,`parent_id`,`created_at`),   -- 帖子评论/楼中楼
    KEY `idx_user` (`user_id`,`created_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';

  -- 标签 / 帖子-标签
  CREATE TABLE `tag` (
    `id` BIGINT NOT NULL, `name` VARCHAR(50) NOT NULL, `post_count` INT NOT NULL DEFAULT 0 COMMENT '冗余',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_name` (`name`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';
  CREATE TABLE `post_tag` (
    `id` BIGINT NOT NULL, `post_id` BIGINT NOT NULL, `tag_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_post_tag` (`post_id`,`tag_id`), KEY `idx_tag` (`tag_id`,`post_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='帖子标签关联';

  -- 图片/附件（图文帖子）
  CREATE TABLE `attachment` (
    `id` BIGINT NOT NULL, `biz_type` TINYINT NOT NULL COMMENT '1帖子 2评论', `biz_id` BIGINT NOT NULL,
    `url` VARCHAR(255) NOT NULL, `file_name` VARCHAR(255) DEFAULT NULL, `file_size` BIGINT DEFAULT NULL,
    `width` INT DEFAULT NULL, `height` INT DEFAULT NULL, `sort_order` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_biz` (`biz_type`,`biz_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图片附件';

  -- 吧内公告
  CREATE TABLE `announcement` (
    `id` BIGINT NOT NULL, `bar_id` BIGINT NOT NULL, `publisher_id` BIGINT NOT NULL COMMENT '吧主',
    `title` VARCHAR(200) NOT NULL, `content` MEDIUMTEXT NOT NULL, `is_top` TINYINT NOT NULL DEFAULT 0,
    `status` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`), KEY `idx_bar` (`bar_id`,`is_top`,`created_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='吧内公告';

  互动与关系模块

  -- 点赞（表名避开 MySQL 保留字 like）
  CREATE TABLE `post_like` (
    `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL,
    `target_type` TINYINT NOT NULL COMMENT '1帖子 2评论', `target_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`),  -- 防重复点赞
    KEY `idx_target` (`target_type`,`target_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞';

  -- 关注（吧 + 用户，type 区分）
  CREATE TABLE `follow` (
    `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL COMMENT '关注者',
    `target_type` TINYINT NOT NULL COMMENT '1吧 2用户', `target_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_target` (`user_id`,`target_type`,`target_id`),
    KEY `idx_target` (`target_type`,`target_id`)                        -- 查粉丝
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关注';

  -- 收藏
  CREATE TABLE `favorite` (
    `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL, `post_id` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), UNIQUE KEY `uk_user_post` (`user_id`,`post_id`), KEY `idx_post` (`post_id`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏';

  -- 私信
  CREATE TABLE `message` (
    `id` BIGINT NOT NULL, `from_user_id` BIGINT NOT NULL, `to_user_id` BIGINT NOT NULL,
    `content` VARCHAR(2000) NOT NULL, `is_read` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_conv` (`from_user_id`,`to_user_id`,`created_at`),  -- 单方会话
    KEY `idx_to_unread` (`to_user_id`,`is_read`)                -- 未读查询
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信';

  -- 通知
  CREATE TABLE `notification` (
    `id` BIGINT NOT NULL, `user_id` BIGINT NOT NULL COMMENT '接收者',
    `type` TINYINT NOT NULL COMMENT '1被回复 2被点赞 3被关注 4系统',
    `from_user_id` BIGINT DEFAULT NULL, `target_type` TINYINT DEFAULT NULL, `target_id` BIGINT DEFAULT NULL,
    `content` VARCHAR(255) DEFAULT NULL, `is_read` TINYINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_unread` (`user_id`,`is_read`,`created_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知';

  -- 举报
  CREATE TABLE `report` (
    `id` BIGINT NOT NULL, `reporter_id` BIGINT NOT NULL,
    `target_type` TINYINT NOT NULL COMMENT '1帖子 2评论', `target_id` BIGINT NOT NULL,
    `reason` VARCHAR(500) DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2驳回',
    `handled_by` BIGINT DEFAULT NULL, `handled_at` DATETIME DEFAULT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`), KEY `idx_status` (`status`,`created_at`)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='举报';

  ---
  关键设计决策说明

  1. 计数冗余 + 一致性：post/comment/bar/user/tag 的计数都是冗余字段。鉴于读写均衡，建议互动操作（点赞/收藏/回帖）在同一事务内同步更新主表与冗余计数，简单可靠；浏览量按你的既定方案走 Redis 递增 + 定时落库。
  2. 软删除与唯一键冲突：username、bar.name 唯一索引 + 软删，会阻止"删号后重注册"。若需要，唯一列在软删时改写（如 username + '#deleted_' + id）或用 (column, deleted) 复合唯一。
  3. 搜索：初期 title/content 走 LIKE '%xx%'，无法命中索引，中型规模可接受；量级上来后迁移 MySQL FULLTEXT(ngram) 或 ES。
  4. 无物理外键：删除校验在应用层，避免写放大与死锁风险。
  5. 楼中楼：comment.parent_id 自引用 + idx_post(post_id,parent_id,created_at) 一次查出整帖评论树。
  6. 雪花 ID：id 无 AUTO_INCREMENT，插入需应用层赋值，注意 MySQL 需要 strict 模式下显式提供。

  这套设计我会落盘为 docs/sql_design/teiba-x.sql（完整建表脚本）并补充一份设计说明。先写执行计划。

● Updated plan
  ⎿  /plan to preview

● 方案设计已完成。完整字段设计已在对话中呈现，执行计划已写入 plan 文件——批准后我会把 15 张表的 DDL 落盘为 docs/sql_design/teiba-x.sql 并更新设计说明。

●BUser approved Claude's plan         s)
  ⎿  Plan saved to: C:\Users\lixia\.claude\plans\serialized-hugging-whisper.md · /rlan to edit
     Teiba-X 数据库表结构设计方案
                                                                                                                                                                                                                        Context
                                                                                                                                                                                                                        Teiba-X 是贴吧类社区项目（Java 17 + Maven，尚未有源码）。当前仅有 docs/sql_design/post_sql.md 提到「帖子表」及「浏览量用 Redis 缓存 + 定时落库」的思路。本项目需要从零设计一套完整社区的表结构。            y

     已确认的业务决策：
     - 功能范围：完整社区（用户/吧/帖子/评论/点赞/关注/收藏/私信/通知/举报/搜索 + 标签话题 + 吧内公告/置顶 + 图片附件）
     - 读写特性：读写均衡
     - 规模：中型（十万级用户、千万级帖子，单库单表即可）
     - 权限：完整权限（全局管理员角色 + 吧级吧主）
     - 删除策略：全部软删除
     - 技术栈：MySQL 8.x、雪花 ID（BIGINT 应用侧生成）、搜索初期用 LIKE
     - 帖子正文：图文（含附件）

     交付物

     1. docs/sql_design/teiba-x.sql — 15 张表的完整 CREATE TABLE 建表脚本
     2. 更新/新增设计说明文档，记录通用约定与关键决策

     表清单（15 张）

     ┌──────────┬────────────────┬──────────────────────────────────────────────┐
     │   模块   │       表       │                     说明                     │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │ 用户权限 │ user           │ 用户 + 全局角色（role 字段）                 │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ bar            │ 吧（版块）                                   │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ bar_manager    │ 吧主/小吧主关联（吧级角色）                  │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │ 内容     │ post           │ 帖子（置顶/加精/冗余计数/浏览量 Redis 落库） │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ comment        │ 评论（parent_id 楼中楼）                     │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ tag + post_tag │ 标签 + 帖子标签关联                          │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ attachment     │ 图片/附件（biz_type+biz_id 多态）            │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ announcement   │ 吧内公告/置顶                                │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │ 互动关系 │ post_like      │ 点赞（target_type 多态，唯一索引防重）       │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ follow         │ 关注（target_type 区分关注吧/用户）          │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ favorite       │ 收藏                                         │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ message        │ 私信                                         │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ notification   │ 通知                                         │
     ├──────────┼────────────────┼──────────────────────────────────────────────┤
     │          │ report         │ 举报                                         │
     └──────────┴────────────────┴──────────────────────────────────────────────┘

     关键设计决策

     - 主键：雪花 ID BIGINT，应用侧生成，无 AUTO_INCREMENT
     - 软删除：内容表含 deleted TINYINT DEFAULT 0；注意 username/bar.name 唯一键与软删冲突 → 用 (column, deleted) 复合唯一 或 软删时改写唯一值
     - 计数冗余：post/comment/bar/user/tag 的计数冗余字段；互动操作同事务更新冗余计数，浏览量走 Redis 递增 + 定时落库
     - 索引：吧内列表 (bar_id, created_at DESC)、置顶 (bar_id, is_top)、按最后回复 (bar_id, last_reply_at DESC)、用户帖子 (user_id, created_at DESC)、后台 (status, created_at
     DESC)；互动表唯一索引防重复；点赞/收藏/关注/吧主/标签关联均唯一约束
     - 外键：不建物理外键，应用层保证一致性，关联字段建普通索引
     - 搜索：初期 LIKE，后续可迁移 FULLTEXT(ngram)/ES

     实施步骤

     1. 创建 docs/sql_design/teiba-x.sql，写入 15 张表完整 DDL（含通用约定头注释）
     2. 更新 docs/sql_design/post_sql.md 或在同目录新增说明文档，记录表清单、通用约定与上述关键决策

     验证

     - 在 MySQL 8.x 环境执行 teiba-x.sql，确认无语法/类型错误（如已装 MySQL 可直接 mysql < teiba-x.sql，否则用 EXPLAIN 抽查关键查询）
     - 抽查关键查询走索引：
       - 吧内帖子列表：SELECT * FROM post WHERE bar_id=? AND deleted=0 ORDER BY created_at DESC → 命中 idx_bar_created
       - 楼中楼：SELECT * FROM comment WHERE post_id=? AND parent_id=? AND deleted=0 → 命中 idx_post
       - 重复点赞防护：INSERT ... ON DUPLICATE KEY 命中 uk_user_target