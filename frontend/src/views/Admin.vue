<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'

interface UserVO {
  id: number
  username: string
  nickname: string
  role: string
  enabled: boolean
  total_focus_seconds: number
  class_id: number | null
  created_at: string
}
interface RoomVO {
  id: number
  roomNo: string
  name: string
  ownerId: number
  capacity: number
  pomodoroMode: boolean
  createdAt: string
}
interface Stats {
  users: number
  rooms: number
  focusRecords: number
  totalFocusSeconds: number
}

const userStore = useUserStore()
const stats = ref<Stats>({ users: 0, rooms: 0, focusRecords: 0, totalFocusSeconds: 0 })
const users = ref<UserVO[]>([])
const rooms = ref<RoomVO[]>([])
const loading = ref(false)

function humanSeconds(sec: number) {
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}

async function loadStats() {
  stats.value = await api.get<Stats>('/api/admin/stats')
}
async function loadUsers() {
  const data = await api.get<{ users: UserVO[] }>('/api/admin/users')
  users.value = data.users
}
async function loadRooms() {
  const data = await api.get<{ rooms: RoomVO[] }>('/api/admin/rooms')
  rooms.value = data.rooms
}
async function refresh() {
  loading.value = true
  try { await Promise.all([loadStats(), loadUsers(), loadRooms()]) }
  catch (e: any) { ElMessage.error(e.message || '加载失败') }
  finally { loading.value = false }
}
onMounted(refresh)

const statCards = computed(() => [
  { label: '注册用户', value: stats.value.users },
  { label: '自习房间', value: stats.value.rooms },
  { label: '专注记录', value: stats.value.focusRecords },
  { label: '累计专注时长', value: humanSeconds(stats.value.totalFocusSeconds) }
])

async function toggleRole(row: UserVO) {
  const next = row.role === 'ADMIN' ? 'USER' : 'ADMIN'
  try {
    await ElMessageBox.confirm(
      `确定将「${row.nickname}」设为${next === 'ADMIN' ? '管理员' : '普通用户'}？`,
      '角色变更', { type: 'warning' })
    await api.post('/api/admin/user/role', { userId: row.id, role: next })
    ElMessage.success('已更新角色')
    await loadUsers()
  } catch (e: any) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '操作失败') }
}

async function toggleEnabled(row: UserVO) {
  const next = !row.enabled
  try {
    await ElMessageBox.confirm(
      `确定${next ? '解封' : '封禁'}「${row.nickname}」？封禁后该用户将无法登录使用系统。`,
      next ? '解封用户' : '封禁用户', { type: 'warning' })
    await api.post('/api/admin/user/enabled', { userId: row.id, enabled: next })
    ElMessage.success(next ? '已解封' : '已封禁')
    await loadUsers()
  } catch (e: any) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '操作失败') }
}

async function deleteRoom(row: RoomVO) {
  try {
    await ElMessageBox.confirm(
      `确定删除房间「${row.name}」（房间号 ${row.roomNo}）？该房间的所有记录将被清除，不可恢复。`,
      '删除房间', { type: 'error', confirmButtonText: '确认删除' })
    await api.post('/api/admin/room/delete', { roomId: row.id })
    ElMessage.success('已删除房间')
    await Promise.all([loadRooms(), loadStats()])
  } catch (e: any) { if (e !== 'cancel' && e !== 'close') ElMessage.error(e.message || '操作失败') }
}
</script>

<template>
  <div class="page-wrap" v-loading="loading">
    <div class="admin-head">
      <div>
        <h2>管理后台</h2>
        <p class="sub">平台数据一览与用户、房间管理</p>
      </div>
      <el-button @click="refresh">刷新数据</el-button>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :span="6" v-for="s in statCards" :key="s.label">
        <div class="hb-card stat-card">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
        </div>
      </el-col>
    </el-row>

    <el-tabs type="border-card" class="admin-tabs">
      <el-tab-pane label="用户管理">
        <el-table :data="users" stripe border style="width:100%" max-height="480">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="nickname" label="昵称" min-width="120" />
          <el-table-column prop="username" label="用户名" min-width="120" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">
              <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'" size="small">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '正常' : '已封禁' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="累计专注" width="120">
            <template #default="{ row }">{{ humanSeconds(row.total_focus_seconds) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button size="small" @click="toggleRole(row)">{{ row.role === 'ADMIN' ? '撤销管理员' : '设为管理员' }}</el-button>
              <el-button size="small" :type="row.enabled ? 'danger' : 'success'" @click="toggleEnabled(row)">
                {{ row.enabled ? '封禁' : '解封' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="房间管理">
        <el-table :data="rooms" stripe border style="width:100%" max-height="480">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="roomNo" label="房间号" width="110" />
          <el-table-column prop="name" label="房间名" min-width="140" />
          <el-table-column prop="ownerId" label="房主ID" width="90" />
          <el-table-column prop="capacity" label="容量" width="80" />
          <el-table-column label="模式" width="100">
            <template #default="{ row }">{{ row.pomodoroMode ? '番茄钟' : '自由' }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="180" />
          <el-table-column label="操作" width="100" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="danger" :disabled="row.ownerId === userStore.user?.id" @click="deleteRoom(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.admin-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 18px; }
.admin-head h2 { margin: 0; font-size: 22px; }
.admin-head .sub { margin: 6px 0 0; color: #909399; font-size: 13px; }
.stat-row { margin-bottom: 18px; }
.stat-card { padding: 18px 20px; }
.stat-label { color: #909399; font-size: 13px; }
.stat-value { margin-top: 8px; font-size: 26px; font-weight: 600; color: var(--hb-primary); }
.admin-tabs :deep(.el-tabs__header) { margin: 0 0 16px; }
@media (max-width: 900px) {
  .stat-row :deep(.el-col) { margin-bottom: 12px; }
}
</style>
