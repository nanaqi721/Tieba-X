<template>
  <el-card
    class="feed-card"
    shadow="hover"
    :body-style="{ padding: '16px' }"
    @click="goDetail"
  >
    <div class="card-body">
      <!-- 封面图 -->
      <el-image
        v-if="post.coverImage"
        :src="post.coverImage"
        fit="cover"
        class="cover"
      />

      <div class="info">
        <h3 class="title">{{ post.title }}</h3>
        <p class="content">{{ post.content }}</p>

        <!-- 所属吧信息（feed 已聚合返回，后端有兜底值） -->
        <div class="bar">
          <el-avatar :src="post.barAvatarUrl" :size="20" />
          <span class="bar-name">{{ post.barName || '未知吧' }}</span>
          <span class="bar-stat">
            {{ formatCount(post.barPostCount) }} 帖 · {{ formatCount(post.barFollowerCount) }} 关注
          </span>
        </div>

        <div class="foot">
          <span>热度 {{ formatCount(post.hotScore) }}</span>
          <span class="more">查看详情 ></span>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { formatCount } from '../utils/format'

// 帖子卡片：展示 FeedPostVO，点击进入帖子详情
const props = defineProps({
  post: {
    type: Object,
    required: true,
  },
})

const router = useRouter()

function goDetail() {
  router.push(`/post/${props.post.postId}`)
}
</script>

<style scoped>
.feed-card {
  cursor: pointer;
}

.card-body {
  display: flex;
  gap: 16px;
}

.cover {
  width: 120px;
  height: 90px;
  border-radius: 4px;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
}

.title {
  margin: 0 0 8px;
  font-size: 17px;
}

.content {
  margin: 0 0 8px;
  color: #606266;
  font-size: 14px;
  /* 最多两行，超出省略 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.bar-name {
  color: #409eff;
  font-size: 13px;
}

.bar-stat {
  color: #909399;
  font-size: 12px;
}

.foot {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
}

.more {
  color: #409eff;
}
</style>
