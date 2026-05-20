<template>
  <div class="note-edit-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button
                v-if="returnToScheduleId"
                @click="goBackToSchedule"
                size="small"
            >
              <el-icon><ArrowLeft /></el-icon> 返回日程
            </el-button>
            <span>{{ isEdit ? '编辑笔记' : 'a新建笔记' }}</span>
          </div>
          <div>
            <el-button @click="goBack">取消</el-button>
            <el-button type="primary" @click="saveNote" :loading="saving">保存</el-button>
          </div>
        </div>
      </template>

      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="标题（可选，不填将自动从正文截取）" maxlength="500" show-word-limit />
        </el-form-item>

        <el-form-item label="正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="15"
            placeholder="请输入内容（最多5000字符）"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="标签">
          <TagSelector v-model="selectedTagId" @tag-created="handleTagCreated" />
        </el-form-item>

        <!-- 版本管理（仅编辑模式显示） -->
        <el-divider v-if="isEdit" />
        <el-form-item v-if="isEdit" label="版本历史">
          <el-button text @click="showVersions = !showVersions">
            {{ showVersions ? '收起' : '查看历史版本' }}
          </el-button>
          <div v-if="showVersions" class="version-list">
            <div v-for="ver in versionList" :key="ver.versionNo" class="version-item">
              <span>版本 {{ ver.versionNo }} - {{ formatDate(ver.saveTime) }}</span>
              <el-button text type="primary" size="small" @click="previewVersion(ver)">预览</el-button>
              <el-button text type="warning" size="small" @click="handleRecoverVersion(ver.versionNo)">恢复此版本</el-button>
            </div>
            <el-empty v-if="versionList.length === 0" description="暂无历史版本" />
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 版本预览弹窗 -->
    <el-dialog v-model="previewVisible" title="版本内容预览" width="60%">
      <pre class="version-preview">{{ previewContent }}</pre>
      <template #footer>
        <el-button @click="previewVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import TagSelector from '@/components/TagSelector.vue'
import { addNote, getNoteById, updateNote, getNoteVersions, recoverVersion } from '@/api/note'
import axios from 'axios'
import { ArrowLeft } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const isEdit = computed(() => !!route.params.id)
const noteId = computed(() => route.params.id ? parseInt(route.params.id) : null)

// 获取来源页面（从日程详情进入还是从笔记管理进入）
const fromSchedule = ref(route.query.from === 'schedule' || sessionStorage.getItem('fromSchedule') === 'true')
const returnToScheduleId = ref(sessionStorage.getItem('returnToSchedule'))

const formRef = ref(null)
const saving = ref(false)
const selectedTagId = ref(null)

const form = reactive({
  id: null,
  title: '',
  content: ''
})

const rules = {
  content: [{ required: true, message: '正文不能为空', trigger: 'blur' }]
}

// 版本管理
const showVersions = ref(false)
const versionList = ref([])
const previewVisible = ref(false)
const previewContent = ref('')

const loadNote = async () => {
  if (!isEdit.value) return
  try {
    const res = await getNoteById(noteId.value)
    if (res.data.code === 200) {
      const note = res.data.data
      form.id = note.id
      form.title = note.title || ''
      form.content = note.content || ''
      // 加载当前笔记的标签
      const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
        params: { targetId: note.id, targetType: 'NOTE' }
      })
      if (tagRes.data.code === 200 && tagRes.data.data) {
        selectedTagId.value = tagRes.data.data.id
      }
      // 加载版本列表
      const verRes = await getNoteVersions(note.id)
      if (verRes.data.code === 200) {
        versionList.value = verRes.data.data || []
      }
    } else {
      ElMessage.error('加载笔记失败')
      goBack()
    }
  } catch (err) {
    ElMessage.error('加载笔记失败')
    goBack()
  }
}

const saveNote = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    let res
    if (isEdit.value) {
      res = await updateNote({
        id: form.id,
        title: form.title,
        content: form.content
      })
    } else {
      res = await addNote({
        title: form.title,
        content: form.content
      })
    }
    if (res.data.code === 200) {
      const savedNote = res.data.data
      // 绑定标签
      if (selectedTagId.value) {
        await axios.post('http://localhost:8080/api/tags/bind', null, {
          params: {
            targetId: savedNote.id,
            targetType: 'NOTE',
            tagId: selectedTagId.value
          }
        })
      } else {
        // 清除标签（如果之前有）
        await axios.delete('http://localhost:8080/api/tags/clear', {
          params: { targetId: savedNote.id, targetType: 'NOTE' }
        })
      }
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      goBack() // 统一使用 goBack 方法
    } else {
      ElMessage.error(res.data.message || '操作失败')
    }
  } catch (err) {
    ElMessage.error('保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 返回逻辑：根据来源决定返回哪里
const goBack = () => {
  if (fromSchedule.value && returnToScheduleId.value) {
    // 从日程详情进入，返回日程详情页
    // 注意：这里不清除 isReturningFromNote，让日程详情页自己处理
    router.push({ path: '/schedule/detail', query: { id: returnToScheduleId.value } })
  } else {
    // 从笔记管理进入，返回笔记管理页
    router.push('/notes')
  }
}

// 返回日程详情页（专门用于点击"返回日程"按钮）
const goBackToSchedule = () => {
  // 注意：不清除 isReturningFromNote，让日程详情页自己处理
  router.push({ path: '/schedule/detail', query: { id: returnToScheduleId.value } })
}

const handleTagCreated = () => {
  // 标签创建后无需额外操作
}

// 预览版本
const previewVersion = (ver) => {
  previewContent.value = ver.content
  previewVisible.value = true
}

// 恢复版本（调用 API）
const handleRecoverVersion = async (versionNo) => {
  try {
    const res = await recoverVersion(noteId.value, versionNo)
    if (res.data.code === 200) {
      ElMessage.success('恢复成功，页面即将刷新')
      loadNote()  // 重新加载笔记和版本列表
    } else {
      ElMessage.error(res.data.message || '恢复失败')
    }
  } catch (err) {
    ElMessage.error('恢复失败')
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

onMounted(() => {
  loadNote()
})
</script>

<style scoped>
.note-edit-container {
  max-width: 900px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.version-list {
  margin-top: 8px;
  border: 1px solid #eee;
  border-radius: 4px;
  padding: 8px;
  max-height: 300px;
  overflow-y: auto;
}
.version-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #f0f0f0;
}
.version-preview {
  white-space: pre-wrap;
  background: #f9f9f9;
  padding: 12px;
  border-radius: 4px;
  max-height: 400px;
  overflow: auto;
}
</style>