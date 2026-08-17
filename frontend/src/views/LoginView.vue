<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2 class="title">登录贴吧 X</h2>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="4-20 位用户名" clearable />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="6-20 位密码"
            @keyup.enter="submit"
          />
        </el-form-item>

        <el-button type="primary" class="submit" :loading="loading" @click="submit">
          登录
        </el-button>
      </el-form>

      <div class="switch">
        还没有账号？
        <router-link to="/register">去注册</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
})

// 校验规则与后端 LoginRequest 约束一致
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需在 4-20 之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6-20 之间', trigger: 'blur' },
  ],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    ElMessage.success('登录成功')
    // 登录后跳回来源页，没有则回首页
    router.push(route.query.redirect || '/')
  } catch (e) {
    // 错误信息已在 request 拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
}

.auth-card {
  width: 380px;
  padding: 8px 12px;
}

.title {
  text-align: center;
  margin: 0 0 20px;
}

.submit {
  width: 100%;
}

.switch {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
</style>
