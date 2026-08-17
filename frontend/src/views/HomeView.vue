<template>
  <div class="home" v-loading="loading">
    <!-- 首次加载骨架屏 -->
    <div v-if="loading && records.length === 0" class="skeleton-list">
      <el-skeleton v-for="i in 3" :key="i" animated>
        <template #template>
          <el-skeleton-item variant="h3" style="width: 40%" />
          <el-skeleton-item variant="text" />
          <el-skeleton-item variant="text" style="width: 70%" />
        </template>
      </el-skeleton>
    </div>

    <!-- 帖子流 -->
    <template v-else-if="records.length > 0">
      <!-- v-infinite-scroll：滚动到底自动加载下一页；hasMore=false 时停载 -->
      <div
        v-infinite-scroll="loadMore"
        :infinite-scroll-disabled="!hasMore || loading"
        :infinite-scroll-distance="100"
        class="feed-list"
      >
        <feed-card v-for="post in records" :key="post.postId" :post="post" />
      </div>

      <div class="feed-footer">
        <el-text v-if="loading" type="info">加载中...</el-text>
        <el-text v-else-if="!hasMore" type="info">已经到底啦</el-text>
      </div>
    </template>

    <!-- 空态 -->
    <el-empty v-else description="暂时没有帖子" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getFeed } from '../api'
import FeedCard from '../components/FeedCard.vue'

const records = ref([]) // 已加载的帖子列表（滚动追加）
const cursor = ref('') // 下一页游标，首次为空
const hasMore = ref(true) // 是否还有下一页
const loading = ref(false) // 请求中标记，防止重复触发

// 请求首页第一页
onMounted(fetchFeed)

// 加载一页：首次（cursor 空）替换列表，滚动加载（cursor 非空）追加
async function fetchFeed() {
  if (loading.value) return
  loading.value = true
  try {
    const data = await getFeed(cursor.value)
    records.value = cursor.value ? records.value.concat(data.records) : data.records
    cursor.value = data.nextCursor || ''
    hasMore.value = !!data.hasMore
  } catch (e) {
    // 错误信息已在拦截器统一弹出，这里兜底处理
    hasMore.value = false
  } finally {
    loading.value = false
  }
}

// 无限滚动回调：只触发"追加下一页"
function loadMore() {
  if (hasMore.value) fetchFeed()
}
</script>

<style scoped>
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feed-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.feed-footer {
  padding: 16px 0;
  text-align: center;
}
</style>
