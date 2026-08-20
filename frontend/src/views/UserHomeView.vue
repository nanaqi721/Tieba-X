<template>
  <section class="user-home" v-loading="loading">
    <el-card v-if="user" class="profile-card" shadow="never">
      <div class="profile-header">
        <el-avatar :size="88" :src="user.avatarUrl || undefined">
          <el-icon :size="42"><UserFilled /></el-icon>
        </el-avatar>
        <div class="profile-summary">
          <h1>{{ user.nickname || user.username }}</h1>
          <el-text type="info">@{{ user.username }}</el-text>
        </div>
      </div>

      <el-divider />

      <div class="statistics" aria-label="用户数据">
        <div class="statistic-item">
          <strong>{{ user.postCount ?? 0 }}</strong>
          <span>发帖</span>
        </div>
        <div class="statistic-item">
          <strong>{{ user.commentCount ?? 0 }}</strong>
          <span>评论</span>
        </div>
        <div class="statistic-item">
          <strong>{{ user.followerCount ?? 0 }}</strong>
          <span>粉丝</span>
        </div>
      </div>
    </el-card>

    <el-empty v-else-if="!loading" description="暂时无法获取用户信息">
      <el-button type="primary" @click="fetchUser">重新加载</el-button>
    </el-empty>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { UserFilled } from '@element-plus/icons-vue'
import { getCurrentUser } from '../api'

const user = ref(null)
const loading = ref(false)

onMounted(fetchUser)

async function fetchUser() {
  loading.value = true
  try {
    user.value = await getCurrentUser()
  } catch {
    user.value = null
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.profile-card {
  border-radius: 12px;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 12px 4px;
}

.profile-summary h1 {
  margin: 0 0 8px;
  color: #303133;
  font-size: 26px;
}

.statistics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.statistic-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: #909399;
  font-size: 14px;
}

.statistic-item + .statistic-item {
  border-left: 1px solid #ebeef5;
}

.statistic-item strong {
  color: #303133;
  font-size: 24px;
}

@media (max-width: 520px) {
  .profile-header {
    gap: 16px;
  }

  .profile-summary h1 {
    font-size: 22px;
  }
}
</style>
