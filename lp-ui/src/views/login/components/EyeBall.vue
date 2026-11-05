<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 登录页眼球动画组件
-->

<template>
  <div
    ref="eyeRef"
    class="eyeball"
    :style="{
      width: `${size}px`,
      height: isBlinking ? '2px' : isSad ? `${size * 0.5}px` : `${size}px`,
      backgroundColor: eyeColor,
      borderRadius: isSad ? `0 0 ${size}px ${size}px` : '50%',
      transform: isSad ? `rotate(${sadRotate}deg)` : 'rotate(0deg)'
    }"
  >
    <div
      v-if="!isBlinking"
      class="pupil"
      :style="{
        width: `${pupilSize}px`,
        height: `${pupilSize}px`,
        backgroundColor: pupilColor,
        transform: `translate(${pupilPosition.x}px, ${isSad ? -1 : pupilPosition.y}px)`
      }"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    size?: number
    pupilSize?: number
    maxDistance?: number
    eyeColor?: string
    pupilColor?: string
    isBlinking?: boolean
    forceLookX?: number
    forceLookY?: number
    isSad?: boolean
    sadRotate?: number
  }>(),
  {
    size: 48,
    pupilSize: 16,
    maxDistance: 10,
    eyeColor: 'white',
    pupilColor: 'black',
    isBlinking: false,
    forceLookX: undefined,
    forceLookY: undefined,
    isSad: false,
    sadRotate: 0
  }
)

const eyeRef = ref<HTMLElement | null>(null)
const mouseX = ref(0)
const mouseY = ref(0)

function handleMouseMove(e: MouseEvent) {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

const pupilPosition = computed(() => {
  if (!eyeRef.value) return { x: 0, y: 0 }
  if (props.forceLookX !== undefined && props.forceLookY !== undefined) {
    return { x: props.forceLookX, y: props.forceLookY }
  }
  const eye = eyeRef.value.getBoundingClientRect()
  const dx = mouseX.value - (eye.left + eye.width / 2)
  const dy = mouseY.value - (eye.top + eye.height / 2)
  const distance = Math.min(Math.sqrt(dx ** 2 + dy ** 2), props.maxDistance)
  const angle = Math.atan2(dy, dx)
  return { x: Math.cos(angle) * distance, y: Math.sin(angle) * distance }
})

onMounted(() => window.addEventListener('mousemove', handleMouseMove))
onUnmounted(() => window.removeEventListener('mousemove', handleMouseMove))
</script>

<style scoped>
.eyeball {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  will-change: height, border-radius, transform;
}

.pupil {
  border-radius: 50%;
  transition: transform 0.1s ease-out;
  will-change: transform;
}
</style>
