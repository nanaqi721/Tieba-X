<template>
  <el-card
    class="feed-card"
    shadow="hover"
    :body-style="{ padding: '16px' }"
    @click="goDetail"
  >
    <div class="bar-header" @click.stop="goBar">
      <el-avatar :src="post.barAvatarUrl" :size="46" />
      <div class="bar-summary">
        <div class="bar-name">{{ post.barName || '未知吧' }}</div>
        <div class="bar-stats">
          <span>关注 {{ formatCount(post.barFollowerCount) }}</span>
          <span>帖子 {{ formatCount(post.barPostCount) }}</span>
        </div>
      </div>
    </div>

    <h3 class="title">{{ post.title }}</h3>
    <p class="content">{{ post.content }}</p>

    <el-image
      v-if="post.coverImage"
      :src="post.coverImage"
      :preview-src-list="[post.coverImage]"
      fit="cover"
      class="cover"
      @click.stop
    />

    <div class="metrics">
      <span>收藏 {{ formatCount(post.favoriteCount) }}</span>
      <span>评论 {{ formatCount(post.commentCount) }}</span>
      <span>点赞 {{ formatCount(post.likeCount) }}</span>
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
  router.push({
    path: `/post/${props.post.postId}`,
    query: { barId: props.post.barId },
  })
}

// 点击吧名进入吧主页
function goBar() {
  router.push(`/bar/${props.post.barId}`)
}
</script>

<style scoped>
.feed-card {
  cursor: pointer;
  background: #f8f9fb;
}

.bar-header {
  display: flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
}

.bar-summary {
  min-width: 0;
}

.bar-name {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.bar-header:hover .bar-name {
  color: #409eff;
}

.bar-stats {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.title {
  margin: 16px 0 8px;
  color: #1f2329;
  font-size: 18px;
}

.content {
  margin: 0 0 12px;
  color: #303133;
  font-size: 15px;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  white-space: pre-wrap;
  word-break: break-word;
}

.cover {
  display: block;
  width: min(100%, 520px);
  height: 300px;
  border-radius: 10px;
}

.metrics {
  display: flex;
  justify-content: space-around;
  gap: 16px;
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  color: #606266;
  font-size: 14px;
}

@media (max-width: 640px) {
  .cover {
    height: 220px;
  }
}
</style>
