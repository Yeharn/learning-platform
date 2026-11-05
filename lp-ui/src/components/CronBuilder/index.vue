<!--
  @Author: Yeharn
  @Date:2026-11-05
  @Version: 1.0.0
  @Description: cron 可视化构建器：秒/分/时/日/月/周/年 + 未来 6 次运行时间预览
-->

<template>
  <div class="cron-builder">
    <el-input v-model="expr" class="cron-expr" placeholder="秒 分 时 日 月 周 [年]，如 0 0 3 * * ?" @change="onManual">
      <template #prefix><span class="expr-tag">CRON</span></template>
    </el-input>

    <el-tabs v-model="activeTab" class="cron-tabs">
      <el-tab-pane v-for="f in FIELDS" :key="f.key" :label="f.label" :name="f.key">
        <el-radio-group v-model="state[f.key].mode" size="small" @change="rebuild">
          <el-radio-button v-for="m in f.modes" :key="m.value" :value="m.value">{{ m.label }}</el-radio-button>
        </el-radio-group>

        <div class="cron-ctrl">
          <template v-if="state[f.key].mode === 'step'">
            <span class="ctrl-text">每</span>
            <el-input-number v-model="state[f.key].step" :min="1" :max="f.max" :step="1" size="small"
              controls-position="right" style="width: 100px" @change="rebuild" />
            <span class="ctrl-text">{{ f.unit }}</span>
          </template>
          <template v-else-if="state[f.key].mode === 'specify'">
            <el-select v-model="state[f.key].values" multiple collapse-tags collapse-tags-tooltip size="small"
              placeholder="选择具体值" style="width: 320px" @change="rebuild">
              <el-option v-for="o in f.options" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </template>
          <span v-else class="ctrl-hint">{{ f.hint }}</span>
        </div>

        <div class="cron-field-tip">{{ f.tip }}</div>
      </el-tab-pane>
    </el-tabs>

    <div class="cron-preview">
      <div class="preview-head">
        <i class="preview-led" />
        <span>未来 6 次运行时间</span>
        <small>NEXT 6 EXECUTIONS</small>
      </div>
      <ol v-if="previews.length" class="preview-list">
        <li v-for="(t, i) in previews" :key="i">
          <span class="idx">{{ String(i + 1).padStart(2, '0') }}</span>
          <span class="time">{{ t }}</span>
        </li>
      </ol>
      <div v-else class="preview-empty">{{ previewError || '等待有效表达式…' }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import cronParser from 'cron-parser'

const props = defineProps<{ modelValue?: string }>()
const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

type Mode = 'every' | 'step' | 'specify'
interface FieldState {
  mode: Mode | 'none'
  step: number
  values: number[]
}
interface FieldDef {
  key: 'sec' | 'min' | 'hour' | 'day' | 'month' | 'week' | 'year'
  label: string
  unit: string
  max: number
  hint: string
  tip: string
  modes: Array<{ value: string; label: string }>
  options: Array<{ value: number; label: string }>
}

const range = (from: number, to: number, fmt?: (n: number) => string) =>
  Array.from({ length: to - from + 1 }, (_, i) => {
    const v = from + i
    return { value: v, label: fmt ? fmt(v) : String(v) }
  })

const WEEK_NAMES = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']

const FIELDS: FieldDef[] = [
  {
    key: 'sec', label: '秒', unit: '秒', max: 59, hint: '每秒执行',
    tip: '支持「每秒 / 每 N 秒 / 指定秒」，如 0 表示整分钟触发',
    modes: [{ value: 'every', label: '每秒' }, { value: 'step', label: '每 N 秒' }, { value: 'specify', label: '指定' }],
    options: range(0, 59)
  },
  {
    key: 'min', label: '分钟', unit: '分钟', max: 59, hint: '每分钟执行',
    tip: '如 每 5 分钟 = */5；指定 0 表示整点触发',
    modes: [{ value: 'every', label: '每分钟' }, { value: 'step', label: '每 N 分钟' }, { value: 'specify', label: '指定' }],
    options: range(0, 59)
  },
  {
    key: 'hour', label: '小时', unit: '小时', max: 23, hint: '每小时执行',
    tip: '如 每 2 小时 = */2；指定 3 表示凌晨 3 点',
    modes: [{ value: 'every', label: '每小时' }, { value: 'step', label: '每 N 小时' }, { value: 'specify', label: '指定' }],
    options: range(0, 23)
  },
  {
    key: 'day', label: '日', unit: '天', max: 31, hint: '每天执行',
    tip: '选择「周」后将按周调度，此处自动置为 ?（Quartz 日与周互斥）',
    modes: [{ value: 'every', label: '每天' }, { value: 'step', label: '每 N 天' }, { value: 'specify', label: '指定' }],
    options: range(1, 31)
  },
  {
    key: 'month', label: '月', unit: '月', max: 12, hint: '每月执行',
    tip: '如 每季度 = */3；指定 1,7 表示 1 月与 7 月',
    modes: [{ value: 'every', label: '每月' }, { value: 'step', label: '每 N 月' }, { value: 'specify', label: '指定' }],
    options: range(1, 12, (n) => `${n} 月`)
  },
  {
    key: 'week', label: '周', unit: '周', max: 7, hint: '不指定（按日调度）',
    tip: '按周调度时「日」置为 ?；1 = 周日 … 7 = 周六',
    modes: [{ value: 'none', label: '不指定' }, { value: 'step', label: '每 N 周' }, { value: 'specify', label: '指定' }],
    options: WEEK_NAMES.map((name, i) => ({ value: i + 1, label: `${name}（${i + 1}）` }))
  },
  {
    key: 'year', label: '年', unit: '年', max: 2099, hint: '每年执行（省略年段）',
    tip: '指定年份或每 N 年时生成 7 段表达式；仅支持 4 位年份',
    modes: [{ value: 'every', label: '每年' }, { value: 'step', label: '每 N 年' }, { value: 'specify', label: '指定' }],
    options: range(new Date().getFullYear(), new Date().getFullYear() + 20)
  }
]

const activeTab = ref('sec')
const expr = ref('')
const previews = ref<string[]>([])
const previewError = ref('')

const state = reactive<Record<string, FieldState>>({
  sec: { mode: 'specify', step: 5, values: [0] },
  min: { mode: 'specify', step: 5, values: [0] },
  hour: { mode: 'every', step: 1, values: [] },
  day: { mode: 'every', step: 1, values: [] },
  month: { mode: 'every', step: 1, values: [] },
  week: { mode: 'none', step: 1, values: [] },
  year: { mode: 'every', step: 1, values: [] }
})

function partOf(key: keyof typeof state): string {
  const s = state[key]
  if (s.mode === 'every') return '*'
  if (s.mode === 'step') return `*/${s.step || 1}`
  return s.values.length ? s.values.join(',') : '*'
}

function rebuild() {
  const weekActive = state.week.mode !== 'none'
  const dayPart = weekActive ? '?' : partOf('day')
  const weekPart = weekActive ? partOf('week') : '?'
  const parts = [partOf('sec'), partOf('min'), partOf('hour'), dayPart, partOf('month'), weekPart]
  if (state.year.mode !== 'every') {
    parts.push(partOf('year'))
  }
  expr.value = parts.join(' ')
  sync()
}

function sync() {
  emit('update:modelValue', expr.value)
  computePreview()
}

function onManual() {
  sync()
}

function format(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} `
    + `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function computePreview() {
  previews.value = []
  previewError.value = ''
  const raw = (expr.value || '').trim()
  if (!raw) return
  const parts = raw.split(/\s+/)
  let base = raw
  if (parts.length === 7) {
    base = parts.slice(0, 6).join(' ')
  } else if (parts.length === 5) {
    base = ['0', ...parts].join(' ')
  }
  const normalized = base.replace(/\?/g, '*')
  try {
    const interval = cronParser.parseExpression(normalized, { tz: 'Asia/Shanghai' })
    const list: string[] = []
    for (let i = 0; i < 6; i++) {
      list.push(format(interval.next().toDate()))
    }
    previews.value = list
  } catch (e: any) {
    previewError.value = `表达式无效：${e?.message || e}`
  }
}

watch(
  () => props.modelValue,
  (val) => {
    if (val && val !== expr.value) {
      expr.value = val
      computePreview()
    }
  }
)

onMounted(() => {
  if (props.modelValue) {
    expr.value = props.modelValue
    computePreview()
  } else {
    rebuild()
  }
})

defineExpose({ getExpression: () => expr.value })
</script>

<style scoped lang="scss">
.cron-builder {
  width: 100%;
}

.cron-expr {
  :deep(.el-input__wrapper) {
    font-family: 'JetBrains Mono', Consolas, monospace;
    border-radius: var(--lp-radius-sm);
  }
}

.expr-tag {
  font-family: Consolas, monospace;
  font-size: 11px;
  letter-spacing: 1px;
  color: var(--lp-primary);
}

.cron-tabs {
  margin-top: 8px;

  :deep(.el-tabs__header) {
    margin-bottom: 10px;
  }

  :deep(.el-tabs__item) {
    font-size: 13px;
    height: 34px;
    line-height: 34px;
  }
}

.cron-ctrl {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  min-height: 28px;

  .ctrl-text {
    font-size: 13px;
    color: var(--lp-text-secondary);
  }

  .ctrl-hint {
    font-size: 12px;
    color: var(--lp-text-secondary);
    opacity: 0.8;
  }
}

.cron-field-tip {
  margin-top: 8px;
  font-size: 12px;
  color: var(--lp-text-secondary);
  opacity: 0.75;
}

.cron-preview {
  margin-top: 12px;
  padding: 12px 14px;
  border-radius: var(--lp-radius-sm);
  background: color-mix(in srgb, var(--lp-primary) 6%, transparent);
  border: 1px solid color-mix(in srgb, var(--lp-primary) 22%, transparent);

  .preview-head {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 13px;
    font-weight: 600;
    color: var(--lp-text);

    small {
      font-family: Consolas, monospace;
      font-size: 10px;
      letter-spacing: 1px;
      color: var(--lp-primary);
      opacity: 0.8;
    }
  }

  .preview-led {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--lp-primary);
    box-shadow: 0 0 8px var(--lp-primary);
    animation: cronLed 1.8s ease-in-out infinite;
  }

  .preview-list {
    list-style: none;
    margin: 8px 0 0;
    padding: 0;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 4px 18px;

    li {
      display: flex;
      align-items: center;
      gap: 8px;
      font-family: Consolas, monospace;
      font-size: 12px;
      color: var(--lp-text-secondary);
      padding: 3px 6px;
      border-radius: 4px;
      transition: background 0.15s ease, color 0.15s ease;

      &:hover {
        background: color-mix(in srgb, var(--lp-primary) 12%, transparent);
        color: var(--lp-text);
      }

      .idx {
        color: var(--lp-primary);
        opacity: 0.7;
        font-size: 11px;
      }
    }
  }

  .preview-empty {
    margin-top: 8px;
    font-size: 12px;
    color: var(--lp-danger);
  }
}

@keyframes cronLed {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
</style>
