<template>
  <div class="note-view-container">
    <!-- 页面头部（与日程、标签页风格一致） -->
    <div class="page-header">
      <h2 class="page-title">笔记管理</h2>
      <el-button type="primary" @click="handleCreate">+ 新建笔记</el-button>
    </div>

    <!-- 查询条件卡片 -->
    <div class="filter-card">
      <el-form :inline="true" :model="queryParams" class="demo-form-inline">
        <el-form-item label="笔记标题">
          <el-input v-model="queryParams.title" placeholder="请输入标题" clearable style="width: 300px" />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="queryParams.tagId" placeholder="请选择标签" clearable style="width: 300px">
            <el-option
              v-for="tag in tagList"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="queryParams.createDate"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            clearable
            style="width: 300px"
          />
        </el-form-item>
        <el-form-item style="margin-left: 200px;">
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 笔记表格 -->
    <el-table
      :data="paginatedNotes"
      stripe
      border
      style="width: 100%"
      v-loading="loading"
    >
      <el-table-column prop="id" label="编号" width="80" align="center" />
      <el-table-column prop="title" label="标题" width="180" align="center" />
      <el-table-column prop="updateTime" label="更新日期" width="220" align="center">
        <template #default="{ row }">
          {{ formatDate(row.updateTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="tagName" label="标签" align="center" flex="1">
        <template #default="{ row }">
          <el-tag v-if="row.tagName" size="small" type="info">{{ row.tagName }}</el-tag>
          <span v-else class="muted">未分类</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="handleView(row)">查看正文</el-button>
          <el-button type="success" link size="small" @click="handleEdit(row)">修改</el-button>
          <!--<el-popconfirm title="确认删除该笔记？删除后可在日程中解除关联，可恢复。" @confirm="handleDelete(row)">
            <template #reference>
              <el-button type="danger" link size="small">删除</el-button>
            </template>
          </el-popconfirm>-->
          <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <div class="pagination">
      <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="filteredNotes.length"
        v-model:page-size="pageSize"
        v-model:current-page="currentPage"
        :page-sizes="[5, 10, 20, 50]"
        @size-change="handlePageChange"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 新建/编辑笔记弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @close="resetForm">
      <el-form :model="noteForm" label-width="100px" ref="formRef" :rules="formRules">
        <el-form-item label="标题" prop="title">
          <el-input v-model="noteForm.title" placeholder="请输入标题（可选，不填自动截取）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="noteForm.content"
            type="textarea"
            :rows="12"
            placeholder="请输入笔记内容（最多5000字符）"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="标签">
          <el-select v-model="noteForm.tagId" placeholder="请选择标签" clearable style="width: 100%">
            <el-option
              v-for="tag in tagList"
              :key="tag.id"
              :label="tag.name"
              :value="tag.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="saveNote" :loading="saving">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看正文弹窗 -->
    <el-dialog v-model="viewDialogVisible" title="笔记正文" width="700px">
      <div class="view-content">
        <h3>{{ viewNote.title || '无标题' }}</h3>
        <div class="view-meta">
          <span>更新于：{{ formatDate(viewNote.updateTime) }}</span>
          <el-tag v-if="viewNote.tagName" size="small">{{ viewNote.tagName }}</el-tag>
        </div>
        <el-divider />
        <div class="view-body">{{ viewNote.content }}</div>
      </div>
      <template #footer>
        <el-button @click="viewDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNoteList, deleteNote, addNote, updateNote } from '@/api/note'
import axios from 'axios'

// 查询参数
const queryParams = reactive({
  title: '',
  tagId: null,
  createDate: []
})

// 原始笔记列表
const allNotes = ref([])
const tagList = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)

// 获取所有标签
const fetchTags = async () => {
  try {
    const res = await axios.get('http://localhost:8080/api/tags')
    if (res.data.code === 200) {
      tagList.value = res.data.data || []
    }
  } catch (err) {
    console.error('获取标签列表失败', err)
  }
}

// 获取笔记列表并补充标签名
const fetchNotes = async () => {
  loading.value = true
  try {
    const res = await getNoteList()
    if (res.data.code === 200) {
      const notes = res.data.data || []
      for (const note of notes) {
        const tagRes = await axios.get('http://localhost:8080/api/tags/target', {
          params: { targetId: note.id, targetType: 'NOTE' }
        })
        if (tagRes.data.code === 200 && tagRes.data.data) {
          note.tagName = tagRes.data.data.name
          note.tagId = tagRes.data.data.id
        } else {
          note.tagName = null
          note.tagId = null
        }
      }
      allNotes.value = notes
    } else {
      ElMessage.error(res.data.message || '加载笔记失败')
    }
  } catch (err) {
    ElMessage.error('加载笔记失败，请检查网络')
  } finally {
    loading.value = false
  }
}

// 筛选后的笔记
const filteredNotes = computed(() => {
  let result = [...allNotes.value]
  if (queryParams.title) {
    const keyword = queryParams.title.toLowerCase()
    result = result.filter(n => n.title?.toLowerCase().includes(keyword))
  }
  if (queryParams.tagId) {
    result = result.filter(n => n.tagId === queryParams.tagId)
  }
  if (queryParams.createDate && queryParams.createDate.length === 2) {
    const [start, end] = queryParams.createDate
    result = result.filter(n => {
      const date = n.updateTime?.split('T')[0]
      return date >= start && date <= end
    })
  }
  return result
})

// 分页后数据
const paginatedNotes = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredNotes.value.slice(start, start + pageSize.value)
})

const handleQuery = () => { currentPage.value = 1 }
const resetQuery = () => {
  queryParams.title = ''
  queryParams.tagId = null
  queryParams.createDate = []
  handleQuery()
}
const handlePageChange = () => {}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${(d.getMonth()+1).toString().padStart(2,'0')}-${d.getDate().toString().padStart(2,'0')} ${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

// 新建/编辑弹窗
const dialogVisible = ref(false)
const dialogTitle = ref('新建笔记')
const dialogType = ref('create')
const formRef = ref()
const saving = ref(false)
const noteForm = reactive({
  id: null,
  title: '',
  content: '',
  tagId: null
})

const formRules = {
  content: [{ required: true, message: '内容不能为空', trigger: 'blur' }]
}

const handleCreate = () => {
  dialogType.value = 'create'
  dialogTitle.value = '新建笔记'
  resetForm()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  dialogTitle.value = '编辑笔记'
  noteForm.id = row.id
  noteForm.title = row.title || ''
  noteForm.content = row.content || ''
  noteForm.tagId = row.tagId
  dialogVisible.value = true
}

const saveNote = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      let res
      if (dialogType.value === 'create') {
        res = await addNote({
          title: noteForm.title,
          content: noteForm.content
        })
      } else {
        res = await updateNote({
          id: noteForm.id,
          title: noteForm.title,
          content: noteForm.content
        })
      }
      if (res.data.code === 200) {
        const savedNote = res.data.data
        if (noteForm.tagId) {
          await axios.post('http://localhost:8080/api/tags/bind', null, {
            params: {
              targetId: savedNote.id,
              targetType: 'NOTE',
              tagId: noteForm.tagId
            }
          })
        } else if (dialogType.value === 'edit' && noteForm.tagId === null) {
          await axios.delete('http://localhost:8080/api/tags/clear', {
            params: { targetId: savedNote.id, targetType: 'NOTE' }
          })
        }
        ElMessage.success(dialogType.value === 'create' ? '新建成功' : '更新成功')
        dialogVisible.value = false
        fetchNotes()
      } else {
        ElMessage.error(res.data.message || '操作失败')
      }
    } catch (err) {
      console.error('保存笔记失败', err)
      ElMessage.error('保存失败，请重试')
    } finally {
      saving.value = false
    }
  })
}

const resetForm = () => {
  noteForm.id = null
  noteForm.title = ''
  noteForm.content = ''
  noteForm.tagId = null
  formRef.value?.clearValidate()
}

// 删除笔记
const handleDelete = async (row) => {
  try {
    const res = await deleteNote(row.id)
    if (res.data.code === 200) {
      ElMessage.success('删除成功')
      if (paginatedNotes.value.length === 1 && currentPage.value > 1) {
        currentPage.value -= 1
      }
      fetchNotes()
    } else {
      ElMessage.error(res.data.message || '删除失败')
    }
  } catch (err) {
    ElMessage.error('删除失败')
  }
}

// 查看正文
const viewDialogVisible = ref(false)
const viewNote = ref({})
const handleView = (row) => {
  viewNote.value = row
  viewDialogVisible.value = true
}

onMounted(() => {
  fetchTags()
  fetchNotes()
})
</script>

<style scoped lang="scss">
.note-view-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.filter-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.muted {
  color: #909399;
  font-size: 12px;
}

.view-content {
  h3 {
    margin-top: 0;
  }
  .view-meta {
    display: flex;
    justify-content: space-between;
    align-items: center;
    color: #909399;
    font-size: 12px;
  }
  .view-body {
    white-space: pre-wrap;
    line-height: 1.6;
  }
}
</style>