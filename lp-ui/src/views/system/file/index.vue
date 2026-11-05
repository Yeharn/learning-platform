<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 系统管理 - 文件管理页面
-->

<template>
  <div class="app-container">
    <el-card class="table-card" shadow="never">
      <div class="toolbar">
        <el-radio-group v-model="query.category" @change="load">
          <el-radio-button value="">全部</el-radio-button>
          <el-radio-button value="image">图片</el-radio-button>
          <el-radio-button value="video">视频</el-radio-button>
          <el-radio-button value="file">文件</el-radio-button>
        </el-radio-group>
        <div class="toolbar-right">
          <el-input v-model="query.originalName" placeholder="文件名" clearable style="width: 180px" @keyup.enter="load" />
          <el-button type="primary" icon="Search" @click="load">搜索</el-button>
          <el-upload :show-file-list="false" multiple :before-upload="beforeUpload" v-hasPerm="['system:file:upload']">
            <el-button type="success" icon="Upload">上传</el-button>
          </el-upload>
          <el-popover trigger="click" width="280" v-hasPerm="['system:file:mode']">
            <template #reference>
              <el-button icon="Switch">存储模式：{{ modeText }}</el-button>
            </template>
            <div class="mode-panel">
              <p>当前存储目录/端点：</p>
              <el-text size="small" type="info">{{ modeInfo.localPath }}</el-text>
              <el-radio-group v-model="pendingMode" style="margin-top: 12px">
                <el-radio value="local">本地磁盘</el-radio>
                <el-radio value="minio">MinIO</el-radio>
                <el-radio value="rustfs">RustFS</el-radio>
              </el-radio-group>
              <el-button type="primary" style="width: 100%; margin-top: 12px" @click="applyMode">应用</el-button>
            </div>
          </el-popover>
          <el-button type="danger" icon="Delete" :disabled="!selection.length" v-hasPerm="['system:file:remove']" @click="handleDelete">删除</el-button>
        </div>
      </div>

      <el-table :data="rows" v-loading="loading" @selection-change="(s: any) => (selection = s)" row-key="fileId">
        <el-table-column type="selection" width="46" />
        <el-table-column label="预览" width="90">
          <template #default="{ row }">
            <el-image v-if="row.category === 'image'" :src="fileUrl(row.url)" :preview-src-list="[fileUrl(row.url)]" preview-teleported fit="cover" style="width: 56px; height: 40px; border-radius: 8px" />
            <el-icon v-else :size="28" class="file-icon">
              <VideoPlay v-if="row.category === 'video'" />
              <Document v-else />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="90">
          <template #default="{ row }">
            <el-tag :type="categoryTag(row.category)">{{ categoryText(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="storageType" label="存储" width="100">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.storageType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileSize" label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="createBy" label="上传人" width="110" />
        <el-table-column prop="createTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Download" @click="download(row)">下载</el-button>
            <el-button link type="danger" icon="Delete" v-hasPerm="['system:file:remove']" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination class="pagination" background layout="total, prev, pager, next, sizes" :total="total"
        v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" @change="load" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fileModeInfo, fileUrl, listFile, delFile, switchFileMode, uploadFile } from '@/api/extras'

const rows = ref<any[]>([])
const selection = ref<any[]>([])
const total = ref(0)
const loading = ref(false)
const modeInfo = ref<any>({})
const pendingMode = ref('local')
const query = reactive({ category: '', originalName: '', pageNum: 1, pageSize: 10 })

const modeText = computed(() => ({ local: '本地', minio: 'MinIO', rustfs: 'RustFS' } as any)[modeInfo.value.mode] || '本地')

function categoryText(c: string) {
  return ({ image: '图片', video: '视频', file: '文件' } as any)[c] || c
}
function categoryTag(c: string) {
  return ({ image: 'success', video: 'warning', file: 'primary' } as any)[c] || 'primary'
}
function formatSize(size: number) {
  if (!size) return '-'
  if (size < 1024) return size + ' B'
  if (size < 1024 * 1024) return (size / 1024).toFixed(1) + ' KB'
  if (size < 1024 * 1024 * 1024) return (size / 1024 / 1024).toFixed(2) + ' MB'
  return (size / 1024 / 1024 / 1024).toFixed(2) + ' GB'
}

async function load() {
  loading.value = true
  try {
    const res: any = await listFile(query)
    rows.value = res.rows || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function loadMode() {
  const res: any = await fileModeInfo()
  modeInfo.value = res
  pendingMode.value = res.mode || 'local'
}

async function beforeUpload(file: File) {
  const limits: any = { image: 10, video: 100, file: 50 }
  const limit = limits[query.category || 'file']
  if (file.size / 1024 / 1024 > limit) {
    ElMessage.error(`文件不能超过 ${limit}MB`)
    return false
  }
  await uploadFile(query.category || 'file', [file])
  ElMessage.success('上传成功')
  load()
  return false
}

async function applyMode() {
  await switchFileMode(pendingMode.value)
  ElMessage.success('存储模式已切换为 ' + modeText.value)
  loadMode()
}

function download(row: any) {
  const a = document.createElement('a')
  a.href = fileUrl(row.url)
  a.download = row.originalName
  a.target = '_blank'
  a.click()
}

async function handleDelete(row?: any) {
  const ids = row ? [row.fileId] : selection.value.map((s) => s.fileId)
  if (!ids.length) return
  await ElMessageBox.confirm('确认删除所选文件（同时删除存储端文件）？', '提示', { type: 'warning' })
  await delFile(ids)
  ElMessage.success('删除成功')
  load()
}

onMounted(() => {
  load()
  loadMode()
})
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.file-icon {
  color: var(--el-color-primary);
}

.mode-panel {
  p {
    margin: 0 0 6px;
  }
}
</style>
