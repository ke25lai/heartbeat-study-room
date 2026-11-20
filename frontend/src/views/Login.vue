<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const router = useRouter()
const userStore = useUserStore()
const mode = ref<'login' | 'register'>('login')
const form = ref({ username: '', password: '', nickname: '' })
const loading = ref(false)

async function submit() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') {
      await userStore.login(form.value.username, form.value.password)
    } else {
      await userStore.register(form.value.username, form.value.password, form.value.nickname || undefined)
    }
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    router.push('/lobby')
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-card hb-card">
      <div class="login-brand">
        <span class="brand-dot"></span>
        <h1>心跳自习室</h1>
      </div>
      <p class="login-sub">一个人走得快，一群人走得远</p>
      <el-tabs v-model="mode" class="login-tabs">
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>
      <el-form @submit.prevent="submit" label-position="top">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码"
                    size="large" show-password @keyup.enter="submit" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="昵称（选填）">
          <el-input v-model="form.nickname" placeholder="默认与用户名相同" size="large" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="submit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  background: linear-gradient(160deg, #eef2ff 0%, #f7f8fc 60%);
}
.login-card { width: 100%; max-width: 400px; padding: 36px 32px; }
.login-brand { display: flex; align-items: center; gap: 10px; }
.login-brand h1 { margin: 0; font-size: 24px; color: var(--hb-primary); }
.brand-dot { width: 12px; height: 12px; border-radius: 50%; background: var(--hb-primary); box-shadow: 0 0 0 5px rgba(99,102,241,.15); }
.login-sub { color: #9095a5; margin: 10px 0 22px; font-size: 14px; }
.login-tabs :deep(.el-tabs__header) { margin-bottom: 22px; }
</style>
