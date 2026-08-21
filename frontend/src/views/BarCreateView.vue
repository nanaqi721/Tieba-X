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
        <el-form-item label="贴吧头像">
          <el-upload
            v-model:file-list="avatarFiles"
            list-type="picture-card"
            accept=".jpg,.jpeg,.png,.gif,.webp,image/jpeg,image/png,image/gif,image/webp"
            :auto-upload="false"
            :limit="1"
            :on-change="validateAvatar"
            :on-exceed="handleAvatarExceed"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="upload-tip">支持 JPG、PNG、GIF、WebP，图片大小不超过 5MB</div>
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
import { Plus } from '@element-plus/icons-vue'
import { createBar, uploadImages } from '../api'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = reactive({ name: '', description: '', avatarUrl: '' })
const avatarFiles = ref([])

const rules = {
  name: [
    { required: true, message: '请输入吧名', trigger: 'blur' },
    { min: 2, max: 50, message: '吧名长度需为 2-50 个字符', trigger: 'blur' },
  ],
}

function validateAvatar(file, files) {
  const raw = file.raw
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  const allowedExtensions = ['jpg', 'jpeg', 'png', 'gif', 'webp']
  const extension = raw?.name.split('.').pop()?.toLowerCase()
  if (!raw || (!allowedTypes.includes(raw.type) && !allowedExtensions.includes(extension))) {
    ElMessage.warning('仅支持 JPG、PNG、GIF、WebP 图片')
    avatarFiles.value = files.filter((item) => item.uid !== file.uid)
    return
  }
  if (raw.size > 5 * 1024 * 1024) {
    ElMessage.warning('头像图片不能超过 5MB')
    avatarFiles.value = files.filter((item) => item.uid !== file.uid)
  }
}

function handleAvatarExceed() {
  ElMessage.warning('只能上传一张贴吧头像')
}

async function submit() {
  await formRef.value.validate()
  loading.value = true
  try {
    const avatar = avatarFiles.value[0]?.raw
    const uploaded = avatar ? await uploadImages([avatar]) : []
    const barId = await createBar({
      ...form,
      avatarUrl: uploaded[0]?.url || '',
    })
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

.upload-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 20px;
}
</style>
