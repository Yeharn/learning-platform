<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 动态表单渲染器：按设计器表单定义渲染字段，支持附件上传与只读展示
-->

<template>
  <el-form :model="model" label-width="112px" class="flow-form" :disabled="readonly">
    <el-row :gutter="12">
      <el-col v-for="field in fields" :key="field.key" :span="field.span || 24">
        <el-form-item :label="field.label" :required="field.required">
          <el-input v-if="field.type === 'input'" v-model="model[field.key]" :placeholder="field.placeholder || '请输入'"
            clearable />

          <el-input v-else-if="field.type === 'textarea'" v-model="model[field.key]" type="textarea" :rows="3"
            :placeholder="field.placeholder || '请输入'" />

          <el-input-number v-else-if="field.type === 'number'" v-model="model[field.key]" :controls-position="'right'"
            style="width: 100%" :placeholder="field.placeholder || '请输入数字'" />

          <el-input-number v-else-if="field.type === 'amount'" v-model="model[field.key]" :precision="2" :step="100"
            :controls-position="'right'" style="width: 100%" :placeholder="field.placeholder || '请输入金额'" />

          <el-date-picker v-else-if="field.type === 'date'" v-model="model[field.key]" type="date"
            value-format="YYYY-MM-DD" style="width: 100%" :placeholder="field.placeholder || '请选择日期'" />

          <el-select v-else-if="field.type === 'select'" v-model="model[field.key]" clearable style="width: 100%"
            :placeholder="field.placeholder || '请选择'">
            <el-option v-for="opt in field.options || []" :key="opt" :label="opt" :value="opt" />
          </el-select>

          <el-radio-group v-else-if="field.type === 'radio'" v-model="model[field.key]">
            <el-radio v-for="opt in field.options || []" :key="opt" :value="opt">{{ opt }}</el-radio>
          </el-radio-group>

          <el-checkbox-group v-else-if="field.type === 'checkbox'" v-model="model[field.key]">
            <el-checkbox v-for="opt in field.options || []" :key="opt" :value="opt">{{ opt }}</el-checkbox>
          </el-checkbox-group>

          <el-select v-else-if="field.type === 'user'" v-model="model[field.key]" multiple filterable
            style="width: 100%" :placeholder="field.placeholder || '请选择人员'">
            <el-option v-for="user in candidates" :key="user.id" :label="user.name + ' (' + user.username + ')'"
              :value="user.name" />
          </el-select>

          <div v-else-if="field.type === 'attachment'" class="attach-field">
            <div v-for="(item, index) in model[field.key] || []" :key="index" class="attach-item">
              <el-link type="primary" :href="item.url" target="_blank">{{ item.name }}</el-link>
              <el-button v-if="!readonly" link type="danger" icon="Delete" @click="removeAttachment(field, index)" />
            </div>
            <el-upload v-if="!readonly" :show-file-list="false" :http-request="(options: any) => doUpload(field, options)">
              <el-button size="small" icon="Upload">上传附件</el-button>
            </el-upload>
            <span v-if="!(model[field.key] || []).length && readonly" class="attach-empty">无附件</span>
          </div>

          <div v-else-if="field.type === 'desc'" class="desc-field">{{ field.placeholder || field.label }}</div>

          <el-input v-else v-model="model[field.key]" :placeholder="field.placeholder || '请输入'" />
        </el-form-item>
      </el-col>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { flowCandidates } from '@/api/flow'
import { uploadFile } from '@/api/extras'

const props = defineProps<{
  fields: any[]
  model: Record<string, any>
  readonly?: boolean
}>()

const candidates = ref<any[]>([])

function removeAttachment(field: any, index: number) {
  const list = props.model[field.key] || []
  list.splice(index, 1)
  props.model[field.key] = [...list]
}

async function doUpload(field: any, options: any) {
  try {
    const res: any = await uploadFile('flow', [options.file])
    const file = Array.isArray(res) ? res[0] : res
    if (!file) {
      throw new Error('上传失败')
    }
    const list = props.model[field.key] || []
    props.model[field.key] = [
      ...list,
      { id: file.fileId, name: file.originalName || file.fileName, url: file.url, size: file.fileSize }
    ]
    options.onSuccess(res)
    ElMessage.success('附件上传成功')
  } catch (e) {
    options.onError(e)
  }
}

onMounted(async () => {
  if (props.fields.some((field) => field.type === 'user')) {
    try {
      candidates.value = (await flowCandidates()) as any
    } catch (e) {
      candidates.value = []
    }
  }
})
</script>

<style scoped>
.flow-form {
  padding-top: 6px;
}

.attach-field {
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.attach-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--el-fill-color-lighter);
}

.attach-empty {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.desc-field {
  width: 100%;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}
</style>
