<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 403 无权限提示页
-->

<template>
  <div class="error-page">
    <div class="code">403</div>
    <p>抱歉，您没有访问该页面的权限</p>
    <el-button type="primary" @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<style scoped>
.error-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background: #f8fafc;
}
.code {
  font-size: 96px;
  font-weight: 800;
  color: #f59e0b;
  letter-spacing: 4px;
}
p {
  color: #6b7280;
}
</style>
