<template>
  <div class="post-detail" v-loading="loading">
    <el-card v-if="post" class="post-card">
      <div class="post-meta">
        <span>所属吧：{{ post.barName || barId }}</span>
        <span>{{ formatTime(post.createTime) }}</span>
      </div>
      <h1>{{ post.title }}</h1>
      <div class="post-content">{{ post.content }}</div>
      <div v-if="post.images?.length" class="post-images">
        <el-image
          v-for="image in post.images"
          :key="image"
          :src="image"
          :preview-src-list="post.images"
          fit="cover"
          class="post-image"
        />
      </div>
      <div class="post-stats">
        <span>浏览 {{ formatCount(post.viewCount) }}</span>
        <span>点赞 {{ formatCount(post.likeCount) }}</span>
        <span>评论 {{ formatCount(post.commentCount) }}</span>
      </div>
    </el-card>

    <el-card class="comment-card">
      <template #header>
        <div class="section-title">评论</div>
      </template>

      <div v-if="auth.isLoggedIn" class="comment-editor">
        <el-input v-model="commentContent" type="textarea" :rows="4" maxlength="1000" show-word-limit />
        <div class="editor-footer">
          <el-button type="primary" :loading="commentLoading" @click="submitComment()">发表评论</el-button>
        </div>
      </div>
      <el-alert v-else title="登录后可以发表评论、回复和点赞" type="info" show-icon :closable="false" />

      <el-divider />

      <el-empty v-if="!commentsLoading && comments.length === 0" description="还没有评论" />
      <div v-else v-loading="commentsLoading" class="floor-list">
        <floor-item
          v-for="item in comments"
          :key="item.id"
          :item="item"
          @reply="openReply"
          @like="like"
          @delete="removeComment"
          @view-replies="openReplies"
        />
      </div>

      <div v-if="floorPage.total > floorPage.pageSize" class="pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="floorPage.pageNum"
          :page-size="floorPage.pageSize"
          :total="floorPage.total"
          @current-change="loadFloors"
        />
      </div>
    </el-card>

    <el-dialog v-model="replyVisible" :title="`回复 ${replyTarget?.nickname || '评论'}`" width="520px">
      <el-input v-model="replyContent" type="textarea" :rows="5" maxlength="1000" show-word-limit />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="commentLoading" @click="submitComment(replyTarget)">
          发送回复
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="repliesVisible" title="楼层详情" width="680px">
      <div v-loading="repliesLoading">
        <template v-if="replyRoot">
          <div class="thread-label">主楼</div>
          <floor-item
            :item="replyRoot"
            :show-view-replies="false"
            @reply="openReply"
            @like="like"
            @delete="removeComment"
          />
          <el-divider content-position="left">全部回复（{{ replyTotal }}）</el-divider>
        </template>
        <floor-item
          v-for="item in replies"
          :key="item.id"
          :item="item"
          @reply="openReply"
          @like="like"
          @delete="removeComment"
        />
        <el-empty v-if="!repliesLoading && replies.length === 0" description="暂无回复" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createComment,
  deleteComment,
  getReplies,
  getFloors,
  getPostSummary,
  likeComment,
} from '../api'
import FloorItem from '../components/FloorItem.vue'
import { useAuthStore } from '../stores/auth'
import { formatTime, formatCount } from '../utils/format'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const postId = String(route.params.postId)
const barId = String(route.query.barId || '')

const loading = ref(false)
const commentsLoading = ref(false)
const commentLoading = ref(false)
const post = ref(null)
const comments = ref([])
const commentContent = ref('')
const floorPage = reactive({ total: 0, pageNum: 1, pageSize: 10 })

const replyVisible = ref(false)
const replyTarget = ref(null)
const replyContent = ref('')

const repliesVisible = ref(false)
const repliesLoading = ref(false)
const replies = ref([])
const replyRoot = ref(null)
const replyRootId = ref('')
const replyTotal = ref(0)

onMounted(loadPage)

async function loadPage() {
  if (!barId) {
    ElMessage.error('缺少帖子所属吧信息')
    return
  }
  loading.value = true
  try {
    const [postData] = await Promise.all([
      getPostSummary(postId),
      loadFloors(1),
    ])
    post.value = postData
  } finally {
    loading.value = false
  }
}

async function loadFloors(page = floorPage.pageNum) {
  commentsLoading.value = true
  try {
    const data = await getFloors(postId, page, floorPage.pageSize)
    comments.value = decorateFloorPreviews(data?.records || [])
    floorPage.total = data?.total || 0
    floorPage.pageNum = data?.pageNum || page
    floorPage.pageSize = data?.pageSize || 10
  } finally {
    commentsLoading.value = false
  }
}

function openReply(item) {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  replyTarget.value = item
  replyContent.value = ''
  replyVisible.value = true
}

async function submitComment(target = null) {
  const content = target ? replyContent.value : commentContent.value
  if (!content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  commentLoading.value = true
  try {
    await createComment(postId, {
      parentId: target?.id || null,
      content: content.trim(),
    })
    ElMessage.success(target ? '回复成功' : '评论成功')
    commentContent.value = ''
    replyContent.value = ''
    replyVisible.value = false
    await loadFloors(1)
    if (repliesVisible.value && replyRootId.value) {
      await loadReplies()
    }
  } finally {
    commentLoading.value = false
  }
}

async function openReplies(item) {
  replyRootId.value = item.id
  replyRoot.value = { ...item, children: [] }
  repliesVisible.value = true
  await loadReplies()
}

async function loadReplies() {
  if (!replyRootId.value) return
  repliesLoading.value = true
  try {
    const data = await getReplies(postId, replyRootId.value, 1, 50)
    const records = data?.records || []
    const commentMap = new Map([
      [replyRoot.value.id, replyRoot.value],
      ...records.map((item) => [item.id, item]),
    ])
    replies.value = records.map((item) => ({
      ...item,
      replyToNickname: commentMap.get(item.parentId)?.nickname || '已删除评论',
    }))
    replyTotal.value = data?.total || 0
  } finally {
    repliesLoading.value = false
  }
}

function decorateFloorPreviews(floors) {
  return floors.map((root) => {
    const children = root.children || []
    const commentMap = new Map([[root.id, root], ...children.map((item) => [item.id, item])])
    return {
      ...root,
      children: children.map((item) => ({
        ...item,
        replyToNickname: commentMap.get(item.parentId)?.nickname || '',
      })),
    }
  })
}

async function like(item) {
  if (!auth.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  item.likeCount = await likeComment(item.id)
}

async function removeComment(item) {
  const deletingRoot = item.id === replyRootId.value
  await ElMessageBox.confirm('确定删除这条评论吗？', '删除确认', { type: 'warning' })
  await deleteComment(item.id)
  ElMessage.success('评论已删除')
  await loadFloors(floorPage.pageNum)
  if (deletingRoot) {
    repliesVisible.value = false
    replyRoot.value = null
    replyRootId.value = ''
  } else if (repliesVisible.value) {
    await loadReplies()
  }
}
</script>

<style scoped>
.post-detail {
  max-width: 860px;
  margin: 0 auto;
  padding: 16px;
}

.post-card,
.comment-card {
  margin-bottom: 16px;
}

.post-meta,
.post-stats {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 13px;
  flex-wrap: wrap;
}

.post-card h1 {
  margin: 14px 0;
  font-size: 26px;
}

.post-content {
  min-height: 100px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

.post-images {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.post-image {
  width: 100%;
  height: 180px;
  border-radius: 6px;
}

.post-stats {
  margin-top: 18px;
}

.section-title {
  font-weight: 700;
}

.comment-editor {
  margin-bottom: 12px;
}

.editor-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}

.floor-list {
  min-height: 120px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 18px;
}

.thread-label {
  margin-bottom: 4px;
  color: #909399;
  font-size: 13px;
  font-weight: 600;
}
</style>
