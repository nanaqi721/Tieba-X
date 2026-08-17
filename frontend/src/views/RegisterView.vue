<template>
  <div class="auth-page">
    <el-card class="auth-card">
      <h2 class="title">注册贴吧 X</h2>

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
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="再次输入密码"
            @keyup.enter="submit"
          />
        </el-form-item>
        <el-form-item label="昵称（选填）" prop="nickname">
          <el-input v-model="form.nickname" placeholder="不填则默认用用户名" clearable />
        </el-form-item>
        <el-form-item label="邮箱（选填）" prop="email">
          <el-input v-model="form.email" placeholder="用于找回密码等" clearable />
        </el-form-item>
        <el-form-item label="性别（选填）" prop="sex">
          <el-radio-group v-model="form.sex">
            <el-radio :value="0">女</el-radio>
            <el-radio :value="1">男</el-radio>
            <el-radio :value="2">保密</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-button type="primary" class="submit" :loading="loading" @click="submit">
          注册
        </el-button>
      </el-form>

      <div class="switch">
        已有账号？
        <router-link to="/login">去登录</router-link>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const formRef = ref()
const loading = ref(false)
const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  email: '',
  sex: undefined,
})

// 校验规则与后端 CreateUserRequest 约束一致
const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度需在 4-20 之间', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6-20 之间', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    await auth.register(form)
    ElMessage.success('注册成功，请登录')
    // 后端注册只返回 userId 不返回 token，跳转登录页
    router.push('/login')
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
