# 帖子表的数据库建设

> 完整建表脚本见 [teiba-x.sql](teiba-x.sql)（共 15 张表：user / bar / bar_manager / post / comment / tag / post_tag / attachment / announcement / post_like / follow / favorite / message / notification / report）

## sql语句

### 建表语句

```sql
```

## Q&A

**Q1:** 将浏览量设计到帖子表中那么每次访问都需要记录次数这样的性能问题怎么解决

A:将帖子的基本信息缓存到redis，每次访问redis递增，添加定时任务同步数据库，如果同步失败打印日志下次再同步



