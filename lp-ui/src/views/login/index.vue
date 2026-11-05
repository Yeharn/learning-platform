<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 登录页：账号密码登录、验证码与角色动画
-->

<template>
  <div class="login-page">
    <div class="bg-glow bg-glow-1" />
    <div class="bg-glow bg-glow-2" />

    <div class="login-wrapper">
      <section class="brand-side">
        <div class="brand-head">
          <img src="/logo.svg" alt="logo" />
          <div>
            <h1>Learning Platform</h1>
            <p>Spring Cloud Alibaba · Sa-Token · MyBatis-Flex</p>
          </div>
        </div>

        <div class="characters-wrap">
          <AnimatedCharacters
            :is-typing="isTyping"
            :show-password="showPassword"
            :password-length="form.password.length"
            :login-failed="loginFailed"
            :login-success="loginSuccess"
          />
        </div>

        <div class="brand-foot">
          <span class="dot" /> 微服务权限管理系统 · 多数据源 · 代码生成
        </div>
      </section>

      <section class="form-side">
        <div class="form-card">
          <div class="form-head">
            <h2>{{ loginSuccess ? '登录成功' : '欢迎回来' }}</h2>
            <p>{{ loginSuccess ? '正在进入控制台...' : '请登录您的管理账号' }}</p>
          </div>

          <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" clearable @input="onTyping">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                v-model="form.password"
                :type="showPassword ? 'text' : 'password'"
                placeholder="请输入密码"
                @input="onTyping"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
                <template #suffix>
                  <el-icon class="pwd-toggle" @click="showPassword = !showPassword">
                    <View v-if="!showPassword" />
                    <Hide v-else />
                  </el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item v-if="captchaEnabled" prop="code">
              <div class="captcha-row">
                <el-input v-model="form.code" placeholder="请输入验证码" maxlength="6">
                  <template #prefix>
                    <el-icon><Key /></el-icon>
                  </template>
                </el-input>
                <div class="captcha-img" title="点击刷新" @click="loadCaptcha">
                  <img v-if="captchaImg" :src="captchaImg" alt="captcha" />
                  <span v-else>加载中</span>
                </div>
              </div>
            </el-form-item>

            <div class="form-options">
              <el-checkbox v-model="form.rememberMe">记住我</el-checkbox>
              <el-button link type="primary" @click="fillDemo">使用演示账号</el-button>
            </div>

            <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">
              {{ loginSuccess ? '进入系统' : '登 录' }}
            </el-button>
          </el-form>

          <div class="tips">
            <span>默认账号 admin / admin123</span>
            <span>普通账号 test / admin123</span>
          </div>
        </div>

        <p class="copyright">© 2026 Learning Platform · org.yeharn.lp</p>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AnimatedCharacters from './components/AnimatedCharacters.vue'
import { getCaptcha } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import { usePermissionStore } from '@/stores/permission'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const permissionStore = usePermissionStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const showPassword = ref(false)
const isTyping = ref(false)
const loginFailed = ref(false)
const loginSuccess = ref(false)
const captchaEnabled = ref(true)
const captchaImg = ref('')
const typingTimer = ref<ReturnType<typeof setTimeout>>()

const form = reactive({
  username: 'admin',
  password: 'admin123',
  code: '',
  uuid: '',
  rememberMe: false
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

function onTyping() {
  isTyping.value = true
  if (typingTimer.value) clearTimeout(typingTimer.value)
  typingTimer.value = setTimeout(() => {
    isTyping.value = false
  }, 900)
}

async function loadCaptcha() {
  try {
    const res = await getCaptcha()
    captchaEnabled.value = res.enabled
    captchaImg.value = res.img
    form.uuid = res.uuid
    form.code = ''
  } catch (e) {
    captchaEnabled.value = false
  }
}

function fillDemo() {
  form.username = 'admin'
  form.password = 'admin123'
  ElMessage.success('已填入演示账号')
}

async function handleLogin() {
  if (loginSuccess.value) {
    router.push((route.query.redirect as string) || '/')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password,
      code: form.code,
      uuid: form.uuid,
      rememberMe: form.rememberMe
    })
    loginSuccess.value = true
    ElMessage.success('登录成功')
    setTimeout(() => {
      router.push((route.query.redirect as string) || '/')
    }, 900)
  } catch (e: any) {
    loginFailed.value = true
    setTimeout(() => {
      loginFailed.value = false
    }, 2000)
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadCaptcha()
})
</script>

<style scoped lang="scss">
.login-page {
  position: relative;
  width: 100%;
  min-height: 100vh;
  background: radial-gradient(circle at 15% 20%, #1e1b4b 0%, #0f172a 45%, #020617 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.55;
  pointer-events: none;
}

.bg-glow-1 {
  width: 520px;
  height: 520px;
  background: #4f46e5;
  top: -180px;
  left: -120px;
}

.bg-glow-2 {
  width: 460px;
  height: 460px;
  background: #0ea5e9;
  bottom: -200px;
  right: -100px;
  opacity: 0.35;
}

.login-wrapper {
  position: relative;
  z-index: 2;
  width: 1080px;
  max-width: 94vw;
  min-height: 620px;
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  border-radius: 24px;
  overflow: hidden;
  background: rgba(17, 24, 39, 0.72);
  border: 1px solid rgba(255, 255, 255, 0.08);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.55);
  backdrop-filter: blur(18px);
}

.brand-side {
  position: relative;
  padding: 38px 40px 26px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  background: linear-gradient(160deg, rgba(99, 102, 241, 0.18), rgba(15, 23, 42, 0.2));
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 14px;

  img {
    width: 46px;
    height: 46px;
    border-radius: 13px;
    box-shadow: 0 8px 20px rgba(79, 70, 229, 0.45);
  }

  h1 {
    color: #fff;
    font-size: 21px;
    letter-spacing: 0.4px;
  }

  p {
    color: #94a3b8;
    font-size: 12px;
    margin-top: 4px;
  }
}

.characters-wrap {
  flex: 1;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  padding-top: 10px;
}

.brand-foot {
  color: #94a3b8;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 8px;

  .dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: #22c55e;
    box-shadow: 0 0 10px #22c55e;
  }
}

.form-side {
  background: rgba(255, 255, 255, 0.98);
  padding: 48px 46px 26px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.form-head {
  margin-bottom: 26px;

  h2 {
    font-size: 26px;
    color: #111827;
    letter-spacing: 0.5px;
  }

  p {
    color: #6b7280;
    font-size: 13px;
    margin-top: 8px;
  }
}

.pwd-toggle {
  cursor: pointer;
  color: #9ca3af;
}

.captcha-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.captcha-img {
  width: 130px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border: 1px solid #e5e7eb;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  span {
    font-size: 12px;
    color: #9ca3af;
  }
}

.form-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.login-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  letter-spacing: 4px;
  border-radius: 8px;
  background: linear-gradient(90deg, #4f46e5, #6366f1);
  border: none;
}

.tips {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  color: #9ca3af;
  font-size: 12px;
}

.copyright {
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  margin-top: 22px;
}

@media (max-width: 900px) {
  .login-wrapper {
    grid-template-columns: 1fr;
  }

  .brand-side {
    display: none;
  }
}
</style>
