<template>
  <div class="floor-item">
    <div class="floor-head">
      <el-avatar :src="item.avatarUrl" :size="32" />
      <div class="head-info">
        <div class="nickname">
          <span>{{ item.nickname || '未知用户' }}</span>
        </div>
        <div class="floor-tag">
          <span>{{ floorLabel }}</span>
          <span>{{ formatTime(item.createTime) }}</span>
        </div>
      </div>
      <el-button class="reply-action" text size="small" @click.stop="emit('reply', item)">回复</el-button>
    </div>

    <div class="floor-content">
      <template v-if="item.replyToNickname">
        <span class="reply-target">@{{ item.replyToNickname }}</span>
      </template>
      {{ item.content }}
    </div>

    <div v-if="item.images && item.images.length > 0" class="comment-images">
      <el-image
        v-for="image in item.images"
        :key="image"
        :src="image"
        :preview-src-list="item.images"
        fit="cover"
        class="comment-image"
      />
    </div>

    <div class="floor-foot">
      <span class="like-count">赞 {{ formatCount(item.likeCount) }}</span>
      <el-button text size="small" @click.stop="emit('like', item)">点赞</el-button>
      <el-button
        v-if="auth.isLoggedIn"
        text
        type="danger"
        size="small"
        @click.stop="emit('delete', item)"
      >
        删除
      </el-button>
    </div>

    <div v-if="item.children && item.children.length > 0" class="children">
      <div v-for="child in item.children" :key="child.id" class="child">
        <floor-item
          :item="child"
          :show-view-replies="showViewReplies"
          @reply="emit('reply', $event)"
          @like="emit('like', $event)"
          @delete="emit('delete', $event)"
          @view-replies="emit('view-replies', $event)"
        />
      </div>
    </div>

    <el-button
      v-if="showViewReplies && item.floor > 0 && Number(item.replyCount || 0) >= 3"
      class="more-replies"
      text
      size="small"
      @click.stop="emit('view-replies', item)"
    >
      展开 {{ item.replyCount }} 条回复
    </el-button>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '../stores/auth'
import { formatTime, formatCount } from '../utils/format'

const emit = defineEmits(['reply', 'like', 'delete', 'view-replies'])
const auth = useAuthStore()

const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
  showViewReplies: {
    type: Boolean,
    default: true,
  },
})

const floorLabel = computed(() => {
  return props.item.floor > 0 ? `${props.item.floor} 楼` : '回复'
})
</script>

<style scoped>
.floor-item {
  padding: 12px 0;
  border-bottom: 1px dashed #ebeef5;
}

.floor-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.head-info {
  min-width: 0;
}

.nickname {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
}

.floor-tag {
  display: flex;
  gap: 10px;
  color: #909399;
  font-size: 12px;
}

.floor-content {
  margin: 8px 0 8px 42px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-target {
  margin-right: 6px;
  color: #315b9d;
}

.reply-action {
  margin-left: auto;
  flex: none;
}

.floor-foot {
  display: flex;
  align-items: center;
  gap: 2px;
  flex-wrap: wrap;
  color: #909399;
  font-size: 12px;
  margin-left: 42px;
}

.comment-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 8px 0 8px 42px;
}

.comment-image {
  width: 96px;
  height: 96px;
  border-radius: 4px;
}

.children {
  margin-top: 8px;
  padding-left: 42px;
}

.child {
  margin-bottom: 2px;
}

.more-replies {
  margin: 4px 0 0 42px;
  color: #315b9d;
  font-weight: 600;
}
</style>
