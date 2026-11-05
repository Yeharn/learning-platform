<!--
  @Author: Yeharn
  @Date:2026-11-04
  @Version: 1.0.0
  @Description: 个人中心：资料修改与密码重置
-->

<template>
  <div class="app-container">
    <el-row :gutter="16">
      <el-col :xs="24" :md="8">
        <el-card shadow="never" class="profile-card">
          <el-avatar :size="86" :src="userInfo?.avatar">
            {{ (userInfo?.nickname || 'U').slice(0, 1) }}
          </el-avatar>
          <h3>{{ userInfo?.nickname }}</h3>
          <p class="text-muted">{{ userInfo?.username }}</p>
          <el-tag v-for="role in roles" :key="role" size="small" style="margin: 2px">{{ role }}</el-tag>
          <el-divider />
          <div class="kv"><span>所属部门</span><b>{{ userInfo?.deptName || '-' }}</b></div>
          <div class="kv"><span>手机号</span><b>{{ userInfo?.phone || '-' }}</b></div>
          <div class="kv"><span>邮箱</span><b>{{ userInfo?.email || '-' }}</b></div>
          <div class="kv"><span>最后登录</span><b>{{ userInfo?.loginDate || '-' }}</b></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="16">
        <el-card shadow="never">
          <el-tabs v-model="activeTab">
            <el-tab-pane label="基本资料" name="info">
              <el-form :model="form" label-width="90px" style="max-width: 520px">
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" />
                </el-form-item>
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" />
                </el-form-item>
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" />
                </el-form-item>
                <el-form-item label="性别">
                  <el-radio-group v-model="form.sex">
                    <el-radio value="0">男</el-radio>
                    <el-radio value="1">女</el-radio>
                    <el-radio value="2">未知</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="头像地址">
                  <el-input v-model="form.avatar" placeholder="图片 URL" />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="saveProfile">保存资料</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="修改密码" name="password">
              <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px" style="max-width: 480px">
                <el-form-item label="原密码" prop="oldPassword">
                  <el-input v-model="pwdForm.oldPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input v-model="pwdForm.newPassword" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码" prop="confirmPassword">
                  <el-input v-model="pwdForm.confirmPassword" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="savePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="我的权限" name="perms">
              <div class="perms-box">
                <el-tag v-for="perm in permissions" :key="perm" size="small" type="info" style="margin: 3px">
                  {{ perm }}
                </el-tag>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { getInfo, updatePassword, updateProfile } from '@/api/auth'

const activeTab = ref('info')
const userInfo = ref<any>({})
const roles = ref<string[]>([])
const permissions = ref<string[]>([])
const form = reactive<any>({})
const pwdFormRef = ref<FormInstance>()
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [{ required: true, message: '请输入新密码', trigger: 'blur' }],
  confirmPassword: [
    {
      validator: (_rule: any, value: string, callback: any) => {
        if (!value) callback(new Error('请再次输入新密码'))
        else if (value !== pwdForm.newPassword) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function load() {
  const res = await getInfo()
  userInfo.value = res.user
  roles.value = res.roles || []
  permissions.value = res.permissions || []
  Object.assign(form, {
    nickname: res.user.nickname,
    phone: res.user.phone,
    email: res.user.email,
    sex: res.user.sex,
    avatar: res.user.avatar
  })
}

async function saveProfile() {
  await updateProfile(form)
  ElMessage.success('资料已更新')
  load()
}

async function savePassword() {
  const valid = await pwdFormRef.value?.validate().catch(() => false)
  if (!valid) return
  await updatePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
  ElMessage.success('密码修改成功，请重新登录')
  setTimeout(() => {
    localStorage.removeItem('lp-token')
    window.location.href = '/login'
  }, 1000)
}

onMounted(load)
</script>

<style scoped lang="scss">
.profile-card {
  text-align: center;
}

h3 {
  margin-top: 12px;
  font-size: 18px;
}

.kv {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 6px 0;

  span {
    color: #6b7280;
  }
}

.perms-box {
  display: flex;
  flex-wrap: wrap;
  max-height: 420px;
  overflow: auto;
}
</style>
