<template>
  <el-container class="layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="logo" @click="$router.push('/')">贴吧 X</div>
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.name === 'postDetail'">帖子详情</el-breadcrumb-item>
      </el-breadcrumb>

      <el-input
        v-model="searchKeyword"
        class="search-box"
        placeholder="搜索帖子或吧"
        clearable
        maxlength="20"
        @keyup.enter="submitSearch"
      >
        <template #append>
          <el-button :icon="Search" aria-label="搜索" @click="submitSearch" />
        </template>
      </el-input>

      <!-- 登录态：未登录显示登录/注册，已登录显示昵称占位 + 退出 -->
      <div class="user-area">
        <template v-if="auth.isLoggedIn">
          <el-button text @click="$router.push('/bar/create')">创建吧</el-button>
          <el-dropdown @command="handleUserCommand">
            <span class="user-entry">
              <el-icon><User /></el-icon>
              <span class="user-name">已登录</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
        <template v-else>
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button type="primary" @click="$router.push('/register')">注册</el-button>
        </template>
      </div>
    </el-header>

    <!-- 内容区：窄栏居中，模仿贴吧版式 -->
    <el-main class="main">
      <router-view />
    </el-main>

    <nav class="bottom-nav" aria-label="底部导航">
      <router-link class="nav-item" :class="{ active: route.name === 'home' }" to="/">
        <el-icon><House /></el-icon>
        <span>首页</span>
      </router-link>
      <router-link class="nav-item" :class="{ active: route.name === 'userHome' }" to="/user">
        <el-icon><User /></el-icon>
        <span>我的</span>
      </router-link>
    </nav>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ref, watch } from 'vue'
import { ArrowDown, House, Search, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const searchKeyword = ref('')

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
  { immediate: true }
)

function submitSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) return
  router.push({ name: 'search', query: { keyword } })
}

async function handleUserCommand(command) {
  if (command === 'logout') {
    await auth.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>

<style scoped>
.layout {
  min-height: 100vh;
  padding-bottom: 72px;
}

.header {
  position: relative;
  display: flex;
  align-items: center;
  gap: 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
}

.search-box {
  position: absolute;
  left: 50%;
  width: min(420px, 38vw);
  transform: translateX(-50%);
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  cursor: pointer;
  white-space: nowrap;
}

.main {
  max-width: 860px;
  width: 100%;
  margin: 0 auto;
  padding-top: 20px;
}

.user-area {
  margin-left: auto;
  display: flex;
  align-items: center;
}

.user-entry {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #606266;
  outline: none;
}

.user-name {
  font-size: 14px;
}

.bottom-nav {
  position: fixed;
  z-index: 100;
  right: 0;
  bottom: 0;
  left: 0;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  height: 64px;
  border-top: 1px solid #ebeef5;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.04);
  backdrop-filter: blur(8px);
}

.nav-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #909399;
  font-size: 12px;
  text-decoration: none;
}

.nav-item .el-icon {
  font-size: 22px;
}

.nav-item.active {
  color: #409eff;
}

@media (max-width: 760px) {
  .breadcrumb {
    display: none;
  }

  .search-box {
    width: min(420px, calc(100% - 180px));
  }

  .user-area {
    display: none;
  }
}
</style>
