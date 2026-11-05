<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 登录页瞳孔动画组件
-->

<template>
  <div
    ref="pupilRef"
    class="pupil"
    :style="{
      width: `${size}px`,
      height: isBlinking ? '2px' : `${size}px`,
      backgroundColor: pupilColor,
      transform: `translate(${pupilPosition.x}px, ${pupilPosition.y}px)`
    }"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    size?: number
    maxDistance?: number
    pupilColor?: string
    forceLookX?: number
    forceLookY?: number
    isBlinking?: boolean
  }>(),
  {
    size: 12,
    maxDistance: 5,
    pupilColor: 'black',
    forceLookX: undefined,
    forceLookY: undefined,
    isBlinking: false
  }
)

const pupilRef = ref<HTMLElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)

function handleMouseMove(e: MouseEvent) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

const pupilPosition = computed(() => {
  if (!pupilRef.value) return { x: 0, y: 0 }
  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY }
  }
  const pupil = pupilRef.value.getBoundingClientRect()
  const dx = mouseX.value - (pupil.left + pupil.width / 2)
  const dy = mouseY.value - (pupil.top + pupil.height / 2)
  const distance = Math.min(Math.sqrt(dx ** 2 + dy ** 2), props.maxDistance)
  const angle = Math.atan2(dy, dx)
  return { x: Math.cos(angle) * distance, y: Math.sin(angle) * distance }
})

onMounted(() => window.addEventListener('mousemove', handleMouseMove))
onUnmounted(() => window.removeEventListener('mousemove', handleMouseMove))
</script>

<style scoped>
.pupil {
  border-radius: 50%;
  transition: transform 0.1s ease-out, height 0.15s ease-out;
  will-change: transform, height;
}
</style>
