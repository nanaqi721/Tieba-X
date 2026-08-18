<template>
  <el-container class="layout">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="logo" @click="$router.push('/')">贴吧 X</div>
      <el-breadcrumb separator="/" class="breadcrumb">
        <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item v-if="route.name === 'postDetail'">帖子详情</el-breadcrumb-item>
      </el-breadcrumb>

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
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowDown, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

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
}

.header {
  display: flex;
  align-items: center;
  gap: 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fff;
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
</style>
