<template>
  <div class="form-page">
    <el-card class="form-card">
      <template #header>
        <div class="card-title">创建吧</div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="吧名" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit clearable />
        </el-form-item>
        <el-form-item label="简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="头像 URL" prop="avatarUrl">
          <el-input v-model="form.avatarUrl" placeholder="可选，例如 https://..." clearable />
        </el-form-item>

        <div class="actions">
          <el-button @click="router.back()">取消</el-button>
          <el-button type="primary" :loading="loading" @click="submit">创建</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createBar } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ name: '', description: '', avatarUrl: '' })

const rules = {
  name: [
    { required: true, message: '请输入吧名', trigger: 'blur' },
    { min: 2, max: 50, message: '吧名长度需为 2-50 个字符', trigger: 'blur' },
  ],
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const barId = await createBar(form)
    ElMessage.success('创建成功')
    router.push(`/bar/${barId}`)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.form-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 24px 16px;
}

.form-card {
  width: 100%;
}

.card-title {
  font-weight: 700;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}
</style>
