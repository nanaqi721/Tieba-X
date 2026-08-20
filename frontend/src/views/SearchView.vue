<template>
  <section class="search-page">
    <div class="search-heading">
      <h1>“{{ keyword }}”的搜索结果</h1>
      <el-text type="info">可以切换查看相关帖子或吧</el-text>
    </div>

    <el-tabs v-model="activeType" class="result-tabs" @tab-change="handleTypeChange">
      <el-tab-pane label="帖子" name="posts" />
      <el-tab-pane label="吧" name="bars" />
    </el-tabs>

    <div v-loading="loading" class="result-area">
      <template v-if="records.length">
        <div v-if="activeType === 'posts'" class="result-list">
          <el-card
            v-for="post in records"
            :key="post.postId"
            class="post-card"
            shadow="hover"
            @click="openPost(post)"
          >
            <h2>{{ post.title }}</h2>
            <p>{{ post.content || '暂无内容摘要' }}</p>
            <div class="post-meta">
              <span class="bar-link" @click.stop="openBar(post.barId)">
                <el-avatar :size="22" :src="post.barAvatarUrl" />
                {{ post.barName || '未知吧' }}
              </span>
              <span>{{ formatTime(post.createTime) }}</span>
            </div>
          </el-card>
        </div>

        <div v-else class="result-list">
          <el-card
            v-for="bar in records"
            :key="bar.barId"
            class="bar-card"
            shadow="hover"
            @click="openBar(bar.barId)"
          >
            <el-avatar :size="56" :src="bar.avatarUrl" />
            <div>
              <h2>{{ bar.name }}</h2>
              <el-text type="info">
                {{ formatCount(bar.postCount) }} 帖 · {{ formatCount(bar.followerCount) }} 关注
              </el-text>
            </div>
          </el-card>
        </div>

        <div class="load-more">
          <el-button v-if="hasMore" :loading="loading" @click="loadMore">加载更多</el-button>
          <el-text v-else type="info">已经到底了</el-text>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="没有找到相关结果" />
    </div>
  </section>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { searchBars, searchPosts } from '../api'
import { formatCount, formatTime } from '../utils/format'

const route = useRoute()
const router = useRouter()
const keyword = ref('')
const activeType = ref('posts')
const records = ref([])
const cursor = ref('')
const hasMore = ref(false)
const loading = ref(false)
let searchVersion = 0

watch(
  () => route.query.keyword,
  (value) => {
    keyword.value = typeof value === 'string' ? value.trim() : ''
    resetAndSearch()
  },
  { immediate: true }
)

function handleTypeChange() {
  resetAndSearch()
}

async function resetAndSearch() {
  searchVersion += 1
  records.value = []
  cursor.value = ''
  hasMore.value = false
  if (keyword.value) await fetchResults(searchVersion)
}

async function fetchResults(version = searchVersion) {
  loading.value = true
  try {
    const search = activeType.value === 'posts' ? searchPosts : searchBars
    const data = await search(keyword.value, cursor.value || undefined, 10)
    if (version !== searchVersion) return
    records.value = cursor.value ? records.value.concat(data.records || []) : data.records || []
    cursor.value = data.nextCursor || ''
    hasMore.value = !!data.hasMore
  } catch {
    if (version === searchVersion && !cursor.value) records.value = []
  } finally {
    if (version === searchVersion) loading.value = false
  }
}

function loadMore() {
  if (hasMore.value && !loading.value) fetchResults()
}

function openPost(post) {
  router.push({ path: `/post/${post.postId}`, query: { barId: post.barId } })
}

function openBar(barId) {
  router.push(`/bar/${barId}`)
}
</script>

<style scoped>
.search-heading {
  margin-bottom: 14px;
}

.search-heading h1 {
  margin: 0 0 6px;
  color: #303133;
  font-size: 22px;
}

.result-area {
  min-height: 280px;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.post-card,
.bar-card {
  cursor: pointer;
}

.post-card h2,
.bar-card h2 {
  margin: 0;
  color: #303133;
  font-size: 17px;
}

.post-card p {
  margin: 10px 0;
  color: #606266;
  line-height: 1.6;
}

.post-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #909399;
  font-size: 13px;
}

.bar-link,
.bar-card {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-link {
  color: #409eff;
}

.load-more {
  padding: 20px 0 4px;
  text-align: center;
}
</style>
