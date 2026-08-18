<template>
  <div class="bar-home" v-loading="feedLoading">
    <!-- 吧头部 -->
    <el-card v-if="bar" class="bar-header">
      <div class="bar-head">
        <el-avatar :src="bar.avatarUrl" :size="48" />
        <div class="bar-info">
          <h2 class="bar-name">{{ bar.name }}</h2>
          <span class="bar-stat">
            {{ formatCount(bar.postCount) }} 帖 · {{ formatCount(bar.followerCount) }} 关注
          </span>
        </div>
        <div v-if="auth.isLoggedIn" class="bar-actions">
          <el-button type="primary" size="small" @click="openCreatePost">发帖</el-button>
          <el-button type="danger" plain size="small" @click="removeBar">删除吧</el-button>
        </div>
      </div>
    </el-card>

    <!-- 排序 tab：切换时清空游标重新拉第一页 -->
    <el-tabs v-model="orderBy" class="sort-tabs" @tab-change="onTabChange">
      <el-tab-pane label="最新" name="createTime" />
      <el-tab-pane label="热度" name="hot" />
    </el-tabs>

    <!-- 首次加载骨架屏 -->
    <div v-if="feedLoading && posts.length === 0" class="skeleton-list">
      <el-skeleton v-for="i in 3" :key="i" animated>
        <template #template>
          <el-skeleton-item variant="h3" style="width: 40%" />
          <el-skeleton-item variant="text" />
          <el-skeleton-item variant="text" style="width: 70%" />
        </template>
      </el-skeleton>
    </div>

    <!-- 帖子流：滚动到底自动加载下一页 -->
    <template v-else-if="posts.length > 0">
      <div
        v-infinite-scroll="loadMore"
        :infinite-scroll-disabled="!hasMore || feedLoading"
        :infinite-scroll-distance="100"
        class="post-list"
      >
        <el-card
          v-for="post in posts"
          :key="post.postId"
          class="post-card"
          shadow="hover"
          @click="goPost(post)"
        >
          <div class="card-body">
            <h3 class="title">{{ post.title }}</h3>
            <p class="content">{{ post.content }}</p>
            <div class="foot">
              <span class="author">
                <el-avatar :src="post.avatarUrl" :size="18" />
                {{ post.nickName || '未知' }}
              </span>
              <span>{{ formatTime(post.createTime) }}</span>
              <span>浏览 {{ formatCount(post.viewCount) }}</span>
              <span>赞 {{ formatCount(post.likeCount) }}</span>
              <span>评 {{ formatCount(post.commentCount) }}</span>
              <span v-if="orderBy === 'hot'">热度 {{ formatCount(post.hotScore) }}</span>
            </div>
          </div>
        </el-card>
      </div>

      <div class="feed-footer">
        <el-text v-if="feedLoading" type="info">加载中...</el-text>
        <el-text v-else-if="!hasMore" type="info">已经到底啦</el-text>
      </div>
    </template>

    <!-- 空态 -->
    <el-empty v-else description="这个吧还没有帖子" />

    <el-dialog v-model="editorVisible" title="创建帖子" width="520px">
      <el-form ref="postFormRef" :model="postForm" :rules="postRules" label-position="top">
        <el-form-item label="标题" prop="title">
          <el-input v-model="postForm.title" maxlength="30" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="postForm.content" type="textarea" :rows="8" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editorVisible = false">取消</el-button>
        <el-button type="primary" :loading="editorLoading" @click="submitPost">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createPost, deleteBar, getBarFeed, queryBar } from '../api'
import { useAuthStore } from '../stores/auth'
import { formatTime, formatCount } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const barId = route.params.barId

const bar = ref(null) // 吧信息（名称/头像/帖数/关注数）
const posts = ref([]) // 已加载帖子（滚动追加）
const orderBy = ref('createTime') // 排序：createTime | hot
const cursor = ref(null) // 下一页游标，首次 null
const hasMore = ref(true) // 是否还有下一页
const feedLoading = ref(false) // 请求中标记，防止重复触发
const editorVisible = ref(false)
const editorLoading = ref(false)
const postFormRef = ref()
const postForm = reactive({ title: '', content: '' })

const postRules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 30, message: '标题长度需为 5-30 个字符', trigger: 'blur' },
  ],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
}

onMounted(() => {
  // 吧信息失败时头部不渲染，feed 接口也会兜底校验吧存在
  queryBar(barId)
    .then((data) => (bar.value = data))
    .catch(() => (bar.value = null))
  fetchFeed()
})

// 加载一页：首次（cursor null）替换列表，滚动加载追加
async function fetchFeed() {
  if (feedLoading.value) return
  feedLoading.value = true
  try {
    const data = await getBarFeed(barId, {
      orderBy: orderBy.value,
      pageSize: 10,
      cursor: cursor.value,
    })
    posts.value = cursor.value ? posts.value.concat(data.data) : data.data
    hasMore.value = !!data.hasNext
    // 没有下一页时游标无意义，置空避免误传
    cursor.value = data.hasNext ? data.cursor : null
  } catch (e) {
    // 错误信息已由拦截器统一弹出，这里兜底停载
    hasMore.value = false
  } finally {
    feedLoading.value = false
  }
}

// 无限滚动回调：只触发"追加下一页"
function loadMore() {
  if (hasMore.value) fetchFeed()
}

// 切换排序：清空游标，重新拉第一页（两种排序的游标字段语义不同，必须重置）
function onTabChange() {
  posts.value = []
  cursor.value = null
  hasMore.value = true
  fetchFeed()
}

function goPost(post) {
  router.push({ path: `/post/${post.postId}`, query: { barId } })
}

function openCreatePost() {
  Object.assign(postForm, { title: '', content: '' })
  editorVisible.value = true
}

async function submitPost() {
  await postFormRef.value.validate()
  editorLoading.value = true
  try {
    await createPost(barId, { title: postForm.title, content: postForm.content })
    ElMessage.success('帖子已发布')
    editorVisible.value = false
    posts.value = []
    cursor.value = null
    hasMore.value = true
    await fetchFeed()
  } finally {
    editorLoading.value = false
  }
}

async function removeBar() {
  await ElMessageBox.confirm('删除吧后将无法继续访问，确定继续吗？', '删除确认', { type: 'warning' })
  await deleteBar(barId)
  ElMessage.success('吧已删除')
  router.push('/')
}
</script>

<style scoped>
.bar-home {
  max-width: 760px;
  margin: 0 auto;
  padding: 16px;
}

.bar-header {
  margin-bottom: 8px;
}

.bar-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.bar-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.bar-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.bar-name {
  margin: 0;
  font-size: 20px;
}

.bar-stat {
  color: #909399;
  font-size: 13px;
}

.sort-tabs {
  margin-bottom: 8px;
}

.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  cursor: pointer;
}

.card-body {
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

.foot {
  display: flex;
  align-items: center;
  gap: 12px;
  color: #909399;
  font-size: 12px;
  flex-wrap: wrap;
}

.author {
  display: flex;
  align-items: center;
  gap: 4px;
}

.feed-footer {
  padding: 16px 0;
  text-align: center;
}
</style>
