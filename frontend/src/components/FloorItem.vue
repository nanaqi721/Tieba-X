<template>
  <div class="floor-item">
    <!-- 楼层主体 -->
    <div class="floor-head">
      <el-avatar :src="item.avatarUrl" :size="32" />
      <div class="head-info">
        <div class="nickname">{{ item.nickname || '未知用户' }}</div>
        <div class="floor-tag">
          <span>{{ floorLabel }}</span>
          <span>{{ formatTime(item.createTime) }}</span>
        </div>
      </div>
    </div>

    <div class="floor-content">{{ item.content }}</div>

    <div class="floor-foot">
      <span class="like-count">👍 {{ formatCount(item.likeCount) }}</span>
    </div>

    <!-- 楼中楼子树：递归渲染 -->
    <div v-if="item.children && item.children.length > 0" class="children">
      <div v-for="child in item.children" :key="child.id" class="child">
        <floor-item :item="child" />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { formatTime, formatCount } from '../utils/format'

/**
 * 楼层节点：递归组件
 * - 顶层楼层 floor 为 1,2,3...，展示 "N 楼"
 * - 楼中楼（parentId 非空）floor 为 0，展示 "回复"
 * - children 挂楼中楼子树，模板内自引用 <floor-item> 递归
 */
const props = defineProps({
  item: {
    type: Object,
    required: true,
  },
})

const floorLabel = computed(() => {
  if (!props.item) return ''
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

.nickname {
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
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.floor-foot {
  color: #909399;
  font-size: 12px;
}

/* 楼中楼缩进，视觉上形成层级 */
.children {
  margin-top: 8px;
  padding-left: 42px;
}

.child {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 0 12px;
  margin-bottom: 6px;
}
</style>
