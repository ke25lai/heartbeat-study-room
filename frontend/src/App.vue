<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from './store/user'

const router = useRouter()
const userStore = useUserStore()

onMounted(() => { userStore.fetchMe() })

async function handleCommand(cmd: string) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'rank') router.push('/rank')
  else if (cmd === 'lobby') router.push('/lobby')
  else if (cmd === 'admin') router.push('/admin')
  else if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
    await userStore.logout()
    router.push('/login')
  }
}
</script>

<template>
  <div>
    <el-header class="hb-header" height="56px">
      <div class="header-inner">
        <div class="brand" @click="router.push('/lobby')">
          <span class="brand-dot"></span>
          <span>心跳自习室</span>
        </div>
        <el-menu mode="horizontal" :default-active="router.currentRoute.value.path"
                 :ellipsis="false" @select="(i: string) => router.push(i)" class="hb-menu"
                 v-if="userStore.user">
          <el-menu-item index="/lobby">大厅</el-menu-item>
          <el-menu-item index="/rank">排行榜</el-menu-item>
          <el-menu-item index="/profile">我的</el-menu-item>
          <el-menu-item index="/admin" v-if="userStore.isAdmin">管理后台</el-menu-item>
        </el-menu>
        <el-dropdown v-if="userStore.user" @command="handleCommand">
          <span class="user-chip">
            {{ userStore.user.nickname }}
            <el-avatar :size="26" style="margin-left:6px">{{ userStore.user.nickname.charAt(0) }}</el-avatar>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人主页</el-dropdown-item>
              <el-dropdown-item command="rank">排行榜</el-dropdown-item>
              <el-dropdown-item command="admin" v-if="userStore.isAdmin">管理后台</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>
    <el-main class="hb-main">
      <router-view />
    </el-main>
  </div>
</template>

<style scoped>
.hb-header {
  background: #fff;
  border-bottom: 1px solid #eef0f5;
  padding: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}
.header-inner {
  max-width: 960px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 0 20px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: var(--hb-primary);
  cursor: pointer;
  white-space: nowrap;
}
.brand-dot {
  width: 10px; height: 10px; border-radius: 50%;
  background: var(--hb-primary);
  box-shadow: 0 0 0 4px rgba(99,102,241,.15);
}
.hb-menu { flex: 1; border-bottom: none !important; }
.hb-menu :deep(.el-menu-item) { height: 56px; }
.user-chip { display: flex; align-items: center; cursor: pointer; font-size: 14px; }
.hb-main { padding: 0; min-height: calc(100vh - 56px); }
@media (max-width: 600px) {
  .hb-menu { display: none; }
}
</style>
