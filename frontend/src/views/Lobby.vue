<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

interface Room {
  id: number; roomNo: string; name: string; capacity: number
  hasPassword: boolean; pomodoroMode: boolean; focusMinutes: number
}

const router = useRouter()
const rooms = ref<Room[]>([])
const createVisible = ref(false)
const joinVisible = ref(false)
const createForm = ref({ name: '', password: '', capacity: 10, pomodoroMode: false, focusMinutes: 25, restMinutes: 5 })
const joinForm = ref({ roomNo: '', password: '', anonymous: false })

async function loadRooms() {
  try { rooms.value = await api.get<Room[]>('/api/room/list') }
  catch (e: any) { ElMessage.error(e.message) }
}
onMounted(loadRooms)

async function createRoom() {
  try {
    const room = await api.post<Room>('/api/room/create', createForm.value)
    createVisible.value = false
    router.push(`/room/${room.id}`)
  } catch (e: any) { ElMessage.error(e.message) }
}

async function joinRoom(roomNo?: string) {
  if (roomNo) joinForm.value.roomNo = roomNo
  try {
    const room = await api.post<Room>('/api/room/join', joinForm.value)
    joinVisible.value = false
    router.push(`/room/${room.id}`)
  } catch (e: any) { ElMessage.error(e.message) }
}

function joinSelected(room: Room) {
  if (room.hasPassword) {
    joinForm.value.roomNo = room.roomNo
    joinForm.value.password = ''
    joinVisible.value = true
  } else {
    joinRoom(room.roomNo)
  }
}
</script>

<template>
  <div class="page-wrap">
    <div class="lobby-head">
      <div>
        <h2>自习室大厅</h2>
        <p class="sub">找一个房间，和大家一起专注</p>
      </div>
      <div class="actions">
        <el-button @click="joinVisible = true">输入房间号加入</el-button>
        <el-button type="primary" @click="createVisible = true">创建自习室</el-button>
      </div>
    </div>

    <el-row :gutter="16" v-if="rooms.length">
      <el-col v-for="r in rooms" :key="r.id" :xs="24" :sm="12" :md="8">
        <div class="room-card hb-card" @click="joinSelected(r)">
          <div class="room-title">
            <span>{{ r.name }}</span>
            <el-tag v-if="r.hasPassword" size="small" type="warning">密码</el-tag>
            <el-tag v-if="r.pomodoroMode" size="small" type="success">番茄钟</el-tag>
          </div>
          <div class="room-no">房间号 <b>{{ r.roomNo }}</b></div>
          <div class="room-meta">
            <span>{{ r.capacity }} 人上限</span>
            <span v-if="r.pomodoroMode">{{ r.focusMinutes }} 分钟专注</span>
            <span v-else>自由计时</span>
          </div>
        </div>
      </el-col>
    </el-row>
    <el-empty v-else description="还没有自习室，创建一个吧" />

    <!-- 创建弹窗 -->
    <el-dialog v-model="createVisible" title="创建自习室" width="90%" style="max-width:460px">
      <el-form :model="createForm" label-position="top">
        <el-form-item label="房间名">
          <el-input v-model="createForm.name" placeholder="例如：考研冲刺自习室" />
        </el-form-item>
        <el-form-item label="房间密码（选填）">
          <el-input v-model="createForm.password" placeholder="留空则公开" />
        </el-form-item>
        <el-form-item label="人数上限">
          <el-input-number v-model="createForm.capacity" :min="1" :max="100" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="createForm.pomodoroMode">开启番茄钟模式</el-checkbox>
        </el-form-item>
        <template v-if="createForm.pomodoroMode">
          <el-form-item label="专注时长（分钟）">
            <el-input-number v-model="createForm.focusMinutes" :min="1" :max="120" />
          </el-form-item>
          <el-form-item label="休息时长（分钟）">
            <el-input-number v-model="createForm.restMinutes" :min="1" :max="60" />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="createRoom">创建</el-button>
      </template>
    </el-dialog>

    <!-- 加入弹窗 -->
    <el-dialog v-model="joinVisible" title="加入自习室" width="90%" style="max-width:400px">
      <el-form :model="joinForm" label-position="top">
        <el-form-item label="房间号">
          <el-input v-model="joinForm.roomNo" placeholder="6 位房间号" />
        </el-form-item>
        <el-form-item label="房间密码（如有）">
          <el-input v-model="joinForm.password" placeholder="公开房间可留空" />
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="joinForm.anonymous">匿名加入</el-checkbox>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="joinVisible = false">取消</el-button>
        <el-button type="primary" @click="joinRoom()">加入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.lobby-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; gap: 12px; flex-wrap: wrap; }
.lobby-head h2 { margin: 0; }
.sub { color: #9095a5; margin: 6px 0 0; font-size: 14px; }
.actions { display: flex; gap: 10px; }
.room-card { padding: 18px; margin-bottom: 16px; cursor: pointer; transition: transform .15s, box-shadow .15s; }
.room-card:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(30,40,90,.1); }
.room-title { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: 500; margin-bottom: 12px; }
.room-no { color: #606a7a; font-size: 13px; margin-bottom: 10px; }
.room-no b { color: var(--hb-primary); letter-spacing: 1px; }
.room-meta { display: flex; gap: 14px; color: #a0a5b5; font-size: 12px; }
</style>
