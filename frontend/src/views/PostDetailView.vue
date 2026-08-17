<template>
  <div class="post-detail" v-loading="detailLoading">
    <!-- 帖子详情 -->
    <el-card v-if="detail" class="post-card">
      <!-- 吧信息 -->
      <div class="bar-info">
        <el-avatar :src="detail.barAvatarUrl" :size="28" />
        <span class="bar-name">{{ detail.barName || '未知吧' }}</span>
      </div>

      <h1 class="title">{{ detail.title }}</h1>

      <!-- 作者与统计 -->
      <div class="meta">
        <el-avatar :src="detail.authorAvatarUrl" :size="24" />
        <span class="author">{{ detail.authorNickname || '未知用户' }}</span>
        <span class="time">{{ formatTime(detail.createTime) }}</span>
        <el-divider direction="vertical" />
        <span>浏览 {{ formatCount(detail.viewCount) }}</span>
        <span>点赞 {{ formatCount(detail.likeCount) }}</span>
        <span>评论 {{ formatCount(detail.commentCount) }}</span>
        <span>收藏 {{ formatCount(detail.favoriteCount) }}</span>
      </div>

      <!-- 正文 -->
      <div class="content">{{ detail.content }}</div>
      <el-image
        v-if="detail.coverImage"
        :src="detail.coverImage"
        :preview-src-list="[detail.coverImage]"
        fit="cover"
        class="cover"
      />
    </el-card>

    <!-- 楼层区 -->
    <el-card class="floor-card">
      <template #header>
        <div class="floor-header">
          <span>全部楼层（{{ floorTotal }}）</span>
          <el-pagination
            small
            layout="prev, pager, next"
            :total="floorTotal"
            :page-size="floorPageSize"
            :current-page="floorPageNum"
            @current-change="changeFloorPage"
          />
        </div>
      </template>

      <div v-if="floors.length > 0" class="floor-list">
        <floor-item v-for="floor in floors" :key="floor.id" :item="floor" />
      </div>
      <el-empty v-else description="暂无楼层" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { getPostDetail, getFloors } from '../api'
import FloorItem from '../components/FloorItem.vue'
import { formatTime, formatCount } from '../utils/format'

const route = useRoute()
const postId = route.params.postId

// 帖子详情
const detail = ref(null)
const detailLoading = ref(false)

// 楼层分页
const floors = ref([])
const floorTotal = ref(0)
const floorPageNum = ref(1)
const floorPageSize = ref(10)

onMounted(() => {
  fetchDetail()
  fetchFloors()
})

// 帖子详情（匿名接口）
async function fetchDetail() {
  detailLoading.value = true
  try {
    detail.value = await getPostDetail(postId)
  } finally {
    detailLoading.value = false
  }
}

// 楼层分页（匿名接口，页码分页）
async function fetchFloors() {
  const data = await getFloors(postId, floorPageNum.value, floorPageSize.value)
  floors.value = data.records
  floorTotal.value = data.total
}

// 切换页码重新加载楼层
function changeFloorPage(page) {
  floorPageNum.value = page
  fetchFloors()
}
</script>

<style scoped>
.post-card {
  margin-bottom: 16px;
}

.bar-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.bar-name {
  font-size: 14px;
  color: #409eff;
}

.title {
  margin: 12px 0;
  font-size: 24px;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 13px;
  flex-wrap: wrap;
}

.content {
  margin: 16px 0;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.cover {
  max-width: 100%;
  border-radius: 4px;
}

.floor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.floor-list {
  display: flex;
  flex-direction: column;
}
</style>
