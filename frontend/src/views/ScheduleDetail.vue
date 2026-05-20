<template>
  <div class="detail-container">
    <div class="detail-header">
      <el-button @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>日程详情</h2>
      <el-button type="primary" @click="saveSchedule">保存</el-button>
    </div>

    <el-form :model="formData" :rules="formRules" ref="formRef" label-width="80px" class="detail-form">
      <el-form-item label="标题" prop="title">
        <el-input
            v-model="formData.title"
            placeholder="请输入日程标题"
            maxlength="20"
            show-word-limit
        />
      </el-form-item>

      <el-form-item label="时间类型" prop="timeType">
        <el-radio-group v-model="formData.timeType">
          <el-radio value="point">时间点</el-radio>
          <el-radio value="period">时间段</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item v-if="formData.timeType === 'point'" label="时间" prop="endTime">
        <el-date-picker
            v-model="formData.endTime"
            type="datetime"
            placeholder="选择日期时间"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            style="width: 100%"
        />
      </el-form-item>

      <template v-else>
        <el-form-item label="开始时间" prop="startTime">
          <el-date-picker
              v-model="formData.startTime"
              type="datetime"
              placeholder="选择开始时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="结束时间" prop="endTime">
          <el-date-picker
              v-model="formData.endTime"
              type="datetime"
              placeholder="选择结束时间"
              format="YYYY-MM-DD HH:mm"
              value-format="YYYY-MM-DDTHH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>
      </template>

      <el-form-item label="重复频率">
        <el-select v-model="formData.repeatRule" placeholder="不重复" style="width: 100%">
          <el-option label="无" value="none" />
          <el-option label="每天" value="daily" />
          <el-option label="每周" value="weekly" />
          <el-option label="每月" value="monthly" />
          <el-option label="每年" value="yearly" />
          <el-option label="工作日" value="workday" />
          <el-option label="节假日" value="holiday" />
        </el-select>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="5"
            placeholder="请输入备注"
            maxlength="800"
            show-word-limit
        />
      </el-form-item>

      <el-form-item label="标签" prop="tagId">
        <TagSelector
            v-model="formData.tagId"
            @tag-created="handleTagCreated"
        />
      </el-form-item>

      <el-form-item label="关联笔记">
        <div class="notes-display">
          <div class="notes-list">
            <el-tag
                v-for="note in selectedNotes"
                :key="note.id"
                closable
                @close="removeNote(note.id)"
                type="success"
                effect="plain"
                class="note-tag clickable"
                @click="goToNoteDetail(note.id)"
            >
              {{ note.title }}
            </el-tag>
            <span v-if="selectedNotes.length === 0" class="placeholder-text">未关联笔记</span>
          </div>
          <el-button size="small" @click="openNoteSelector">
            <el-icon><Plus /></el-icon> 选择笔记
          </el-button>
        </div>
      </el-form-item>
    </el-form>

    <!-- 笔记选择器弹窗 -->
    <el-dialog
        v-model="noteDialogVisible"
        title="选择关联笔记"
        width="600px"
        append-to-body
    >
      <div class="note-selector">
        <div class="note-search-bar">
          <el-input
              v-model="noteSearchKeyword"
              placeholder="按标题搜索"
              clearable
              prefix-icon="Search"
              style="width: 200px; margin-right: 12px;"
          />
          <el-select
              v-model="noteFilterTagId"
              placeholder="按标签筛选"
              clearable
              style="width: 150px"
          >
            <el-option
                v-for="tag in tagList"
                :key="tag.id"
                :label="tag.name"
                :value="tag.id"
            />
          </el-select>
        </div>
        <div class="note-list-selector">
          <div
              v-for="note in filteredNoteList"
              :key="note.id"
              class="note-item-selector"
              @click="toggleNoteSelection(note.id)"
          >
            <el-checkbox :model-value="tempSelectedNoteIds.includes(note.id)" @click.stop />
            <div class="note-info">
              <span class="note-title">{{ note.title || '无标题' }}</span>
              <span v-if="note.tagName" class="note-tag-name">#{{ note.tagName }}</span>
            </div>
            <el-button text @click.stop="viewNoteDetail(note)">
              <el-icon><Document /></el-icon> 查看
            </el-button>
          </div>
          <el-empty v-if="filteredNoteList.length === 0" description="暂无笔记" />
        </div>
      </div>
      <template #footer>
        <el-button @click="noteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmNoteSelection">确定</el-button>
      </template>
    </el-dialog>

    <!-- 笔记详情查看弹窗 -->
    <el-dialog
        v-model="viewNoteDialogVisible"
        :title="currentViewNote?.title || '笔记详情'"
        width="500px"
        append-to-body
    >
      <div class="note-view-content">
        <div class="note-view-meta">
          <span>更新于：{{ formatDate(currentViewNote?.updateTime) }}</span>
          <el-tag v-if="currentViewNote?.tagName" size="small">{{ currentViewNote.tagName }}</el-tag>
        </div>
        <el-divider />
        <div class="note-view-body">{{ currentViewNote?.content }}</div>
      </div>
      <template #footer>
        <el-button @click="viewNoteDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Plus, Document, Search } from '@element-plus/icons-vue'
import axios from 'axios'
import TagSelector from "@/components/TagSelector.vue";
import { onBeforeRouteLeave } from 'vue-router'

const route = useRoute()
const router = useRouter()
const formRef = ref(null)

const scheduleId = ref(route.query.id)
const tagList = ref([])

// 所有笔记列表（从后端获取）
const allNotes = ref([])

// 格式化日期时间
const formatDateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
}

// 表单数据
const formData = ref({
  id: null,
  title: '',
  timeType: 'point',
  startTime: '',
  endTime: '',
  repeatRule: 'none',
  remark: '',
  tagId: null,
  noteIds: []
})

// 表单校验规则
const formRules = {
  title: [
    { required: true, message: '请输入日程标题', trigger: 'blur' },
    { max: 20, message: '标题不能超过20个字符', trigger: 'blur' }
  ],
  endTime: [{ required: true, message: '请选择时间', trigger: 'change' }],
  remark: [
    { max: 800, message: '备注不能超过800个字符', trigger: 'blur' }
  ]
}

// ---------- 标签和笔记显示相关 ----------
const selectedTag = ref(null)
const selectedNotes = ref([])

// ---------- 笔记选择器相关 ----------
const noteSearchKeyword = ref('')
const noteFilterTagId = ref(null)
const noteDialogVisible = ref(false)
const tempSelectedNoteIds = ref([])
const viewNoteDialogVisible = ref(false)
const currentViewNote = ref(null)

// 过滤后的笔记列表
const filteredNoteList = computed(() => {
  let result = [...allNotes.value]

  if (noteSearchKeyword.value.trim()) {
    const keyword = noteSearchKeyword.value.trim().toLowerCase()
    result = result.filter(note =>
        note.title?.toLowerCase().includes(keyword)
    )
  }

  if (noteFilterTagId.value) {
    result = result.filter(note => note.tagId === noteFilterTagId.value)
  }

  return result
})

// 根据标签ID获取标签对象
const getTagById = (tagId) => {
  return tagList.value.find(t => t.id === tagId)
}

// 根据笔记ID列表获取笔记对象列表
const getNotesByIds = (noteIds) => {
  return allNotes.value.filter(n => noteIds.includes(n.id))
}

// 切换笔记选中状态
const toggleNoteSelection = (noteId) => {
  const index = tempSelectedNoteIds.value.indexOf(noteId)
  if (index > -1) {
    tempSelectedNoteIds.value.splice(index, 1)
  } else {
    tempSelectedNoteIds.value.push(noteId)
  }
}

// 打开笔记选择器
const openNoteSelector = () => {
  tempSelectedNoteIds.value = [...formData.value.noteIds]
  noteSearchKeyword.value = ''
  noteFilterTagId.value = null
  noteDialogVisible.value = true
}

// 确认选择笔记
const confirmNoteSelection = () => {
  selectedNotes.value = allNotes.value.filter(n => tempSelectedNoteIds.value.includes(n.id))
  formData.value.noteIds = tempSelectedNoteIds.value
  noteDialogVisible.value = false
}

// 移除笔记
const removeNote = (noteId) => {
  selectedNotes.value = selectedNotes.value.filter(n => n.id !== noteId)
  formData.value.noteIds = selectedNotes.value.map(n => n.id)
}

// 查看笔记详情（弹窗内）
const viewNoteDetail = (note) => {
  currentViewNote.value = note
  viewNoteDialogVisible.value = true
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// 获取日程详情
const fetchScheduleDetail = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/schedule/detail', {
      params: { id: scheduleId.value }
    })
    if (response.data.code === 200) {
      const data = response.data.data
      formData.value.id = data.id
      formData.value.title = data.title
      formData.value.repeatRule = data.repeatRule || 'none'
      formData.value.remark = data.remark || ''

      if (data.tagId) {
        selectedTag.value = getTagById(data.tagId)
        formData.value.tagId = data.tagId
      }

      if (data.noteIds && data.noteIds.length > 0) {
        // 先获取所有笔记列表，再筛选出关联的
        await fetchNoteList()  // 确保笔记列表已加载
        selectedNotes.value = allNotes.value.filter(n => data.noteIds.includes(n.id))
        formData.value.noteIds = data.noteIds
      } else {
        selectedNotes.value = []
        formData.value.noteIds = []
      }

      // 时间类型处理
      if (!data.startTime) {
        formData.value.timeType = 'point'
        formData.value.endTime = data.endTime
        formData.value.startTime = ''
      } else {
        formData.value.timeType = 'period'
        formData.value.startTime = data.startTime
        formData.value.endTime = data.endTime
      }
    }
  } catch (error) {
    console.error('获取日程详情失败', error)
    ElMessage.error('加载失败')
  }
}

// 获取标签列表
const fetchTagList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/tags')
    if (response.data.code === 200) {
      tagList.value = response.data.data
    }
  } catch (error) {
    console.error('获取标签失败', error)
  }
}

// 获取笔记列表
const fetchNoteList = async () => {
  try {
    const response = await axios.get('http://localhost:8080/api/note/list')
    if (response.data.code === 200) {
      const notes = response.data.data || []
      for (const note of notes) {
        const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
          params: { targetId: note.id, targetType: 'NOTE' }
        })
        if (tagRes.data.code === 200 && tagRes.data.data) {
          note.tagName = tagRes.data.data.name
          note.tagId = tagRes.data.data.id
        }
      }
      allNotes.value = notes
    }
  } catch (error) {
    console.error('获取笔记失败', error)
  }
}

// 保存日程
const saveSchedule = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请填写必填项')
      return
    }

    ElMessageBox.confirm('确定要保存修改吗？', '确认', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        const submitData = {
          id: formData.value.id,
          title: formData.value.title,
          repeatRule: formData.value.repeatRule,
          remark: formData.value.remark,
          tagId: formData.value.tagId,
          noteIds: formData.value.noteIds
        }

        if (formData.value.timeType === 'point') {
          submitData.startTime = null
          submitData.endTime = formData.value.endTime
        } else {
          submitData.startTime = formData.value.startTime
          submitData.endTime = formData.value.endTime
        }

        const response = await axios.put('http://localhost:8080/api/schedule/update', submitData)

        if (response.data.code === 200) {
          ElMessage.success('保存成功')
          router.push('/schedules')
        } else {
          ElMessage.error(response.data.message || '保存失败')
        }
      } catch (error) {
        console.error('保存失败', error)
        ElMessage.error('保存失败')
      }
    }).catch(() => {})
  })
}

// 标记是否是从笔记页返回
const isReturningFromNote = ref(sessionStorage.getItem('isReturningFromNote') === 'true')

// 保存标记到 sessionStorage
const setReturningFromNote = (value) => {
  isReturningFromNote.value = value
  sessionStorage.setItem('isReturningFromNote', value)
}

// 跳转到笔记详情页
const goToNoteDetail = (noteId) => {
  if (noteId) {
    // 设置标记，表示要从日程详情跳转到笔记
    sessionStorage.setItem('returnToSchedule', scheduleId.value)
    sessionStorage.setItem('fromSchedule', 'true')
    sessionStorage.setItem('isReturningFromNote', 'true')
    router.push({ path: `/notes/edit/${noteId}`, query: { from: 'schedule' } })
  }
}

// 返回日程列表页
const goBack = () => {
  // 检查 sessionStorage 中的标记
  const isReturning = sessionStorage.getItem('isReturningFromNote') === 'true'
  const fromSchedule = sessionStorage.getItem('fromSchedule') === 'true'

  if (isReturning && fromSchedule) {
    // 清除所有相关标记
    sessionStorage.removeItem('isReturningFromNote')
    sessionStorage.removeItem('fromSchedule')
    router.push('/schedules')
  } else {
    router.back()
  }
}

const handleTagCreated = () => {
  fetchTagList()
}

onMounted(async () => {
  await fetchNoteList()
  await fetchTagList()
  await fetchScheduleDetail()
})

</script>

<style scoped>
.detail-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 30px;
}

.detail-header h2 {
  margin: 0;
}

.detail-form {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.tag-display {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.notes-display {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notes-list {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.placeholder-text {
  color: #c0c4cc;
  font-size: 13px;
}

.note-tag.clickable {
  cursor: pointer;
  transition: all 0.2s;
}

.note-tag.clickable:hover {
  transform: scale(1.02);
  opacity: 0.8;
}

/* 笔记选择器样式 */
.note-selector {
  padding: 8px 0;
}

.note-search-bar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.note-list-selector {
  max-height: 400px;
  overflow-y: auto;
}

.note-item-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.note-item-selector:hover {
  background-color: #f5f7fa;
}

.note-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.note-title {
  font-size: 14px;
  color: #303133;
}

.note-tag-name {
  font-size: 12px;
  color: #909399;
}

.note-view-content {
  max-height: 400px;
  overflow-y: auto;
}

.note-view-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

.note-view-body {
  white-space: pre-wrap;
  line-height: 1.6;
}
</style>