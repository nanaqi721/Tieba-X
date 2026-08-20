# 全站帖子搜索

状态：已确认（2026-08-20）

## 范围

- 在 `post-server` 新增全站帖子关键词搜索。
- 贴吧名称搜索属于 `bar-server`，不在本功能范围内。
- 新接口允许匿名访问，因此 gateway 需要加入对应白名单。

## HTTP 契约

- `GET /api/posts/v1/search`
- 查询参数：
  - `keyword`：必填；去除首尾空白后按 Java `String.length()` 计算，长度必须为 1–20。
  - `pageSize`：可选，默认 10，有效范围 1–10；越界时返回参数错误。
  - `cursor`：可选；未传、空字符串或纯空白表示第一页。
- 参数错误沿用项目约定：HTTP 200，响应体业务码 `A0001`。

## 匹配与数据

- 数据源为 MySQL `post` 表。
- 在标题和完整正文中进行子串匹配，任一字段命中即可。
- 英文字母不区分大小写，沿用 MySQL 字段现有排序规则。
- `%`、`_` 与转义字符只表示普通文本，不开放 `LIKE` 通配语义。
- 只返回 `deleted = 0` 的帖子；暂不引入审核状态过滤。
- 使用现有 `create_time` 列，按 `create_time DESC, id DESC` 稳定排序。

## 分页

- 采用不透明字符串游标，内部包含创建时间、帖子 ID 和规范化搜索词摘要。
- 游标使用 Base64URL 编码，不使用 HMAC 签名。
- 游标必须与当前搜索词绑定；损坏、字段缺失或搜索词不匹配时返回 `A0001`。
- 响应包含 `records`、`nextCursor` 和 `hasMore`。
- 仅在还有下一页时返回非空 `nextCursor`；空结果和末页的游标为 `null`。

## 搜索结果

每条记录返回：

- `postId`
- `barId`
- `barName`
- `barAvatarUrl`
- `title`
- `content`
- `createTime`

正文从数据库取出后处理：长度不超过 30 时原样返回；超过 30 时返回 `substring(0, 30) + "..."`。

通过现有贴吧批量接口补充吧名和吧头像。贴吧服务失败或找不到对应贴吧时保留帖子，并返回 `barName = "未知吧"`、`barAvatarUrl = ""`。

## 验收

- 第一版按全站帖子不超过 10 万条设计。
- 具备代表性数据后，性能目标为 P95 不超过 1 秒。
- 自动化测试覆盖 Service 行为和 Controller/MockMvc HTTP 契约，数据访问层使用 Mock。
- 真实 MySQL 的大小写规则、SQL 排序和 10 万条性能留待后续集成验收。
