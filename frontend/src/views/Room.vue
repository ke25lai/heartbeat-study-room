<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElNotification, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { useUserStore } from '../store/user'
import { WsConnection } from '../ws/connection'
import { playChime, primeAudio } from '../utils/sound'
import WhiteNoise from '../components/WhiteNoise.vue'

interface Member {
  userId: number | null; clientKey: string; nickname: string; anonymous: boolean
  status: 'IDLE' | 'STUDYING' | 'RESTING'; focusSeconds: number; owner: boolean
}
interface RoomInfo {
  id: number; roomNo: string; name: string; ownerId: number
  pomodoroMode: boolean; focusMinutes: number; restMinutes: number
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const roomId = Number(route.params.id)

const room = ref<RoomInfo | null>(null)
const members = ref<Member[]>([])
const selfKey = ref<string>('')           // 服务端告知的"我"的 clientKey
const status = ref<'IDLE' | 'STUDYING' | 'RESTING'>('IDLE')
const elapsed = ref(0)                    // 本次专注秒数（客户端展示）
let tickTimer: number | null = null
let ws: WsConnection | null = null
const isOwner = computed(() => !!userStore.user && !!room.value && userStore.user.id === room.value.ownerId)

const statusText = computed(() => ({
  IDLE: '空闲', STUDYING: '学习中', RESTING: '休息中'
}[status.value]))
const timeText = computed(() => {
  const h = Math.floor(elapsed.value / 3600)
  const m = Math.floor((elapsed.value % 3600) / 60)
  const s = elapsed.value % 60
  return `${pad(h)}:${pad(m)}:${pad(s)}`
})
function pad(n: number) { return String(n).padStart(2, '0') }

function startTick() {
  if (tickTimer !== null) clearInterval(tickTimer)
  elapsed.value = 0
  tickTimer = window.setInterval(() => { elapsed.value++ }, 1000)
}
function stopTick() {
  if (tickTimer !== null) { clearInterval(tickTimer); tickTimer = null }
}

onMounted(async () => {
  try {
    room.value = await api.get<RoomInfo>(`/api/room/${roomId}`)
  } catch (e: any) { ElMessage.error(e.message); return }

  ws = new WsConnection(roomId)
  ws.onMessage(handleMessage)
  ws.connect()
})

onUnmounted(() => {
  ws?.close()
  stopTick()
})

function handleMessage(msg: any) {
  const data = msg.data || {}
  switch (msg.type) {
    case 'MEMBER_SNAPSHOT':
      selfKey.value = data.selfKey || ''
      members.value = data.members || []
      break
    case 'MEMBER_ONLINE':
      if (!members.value.find(m => m.clientKey === data.clientKey)) {
        members.value.push({
          userId: data.userId ?? null, clientKey: data.clientKey, nickname: data.nickname,
          anonymous: data.userId == null, status: 'IDLE', focusSeconds: 0, owner: false
        })
      }
      notify(`${data.nickname} 进入了自习室`)
      break
    case 'MEMBER_OFFLINE':
      members.value = members.value.filter(m => m.clientKey !== data.clientKey)
      break
    case 'STATUS_CHANGE': {
      if (data.clientKey === selfKey.value) status.value = data.status
      const mm = members.value.find(m => m.clientKey === data.clientKey)
      if (mm) mm.status = data.status
      break
    }
    case 'POMODORO_SWITCH': {
      // 只驱动自己的计时器与状态；他人状态由 STATUS_CHANGE 已同步
      if (data.clientKey !== selfKey.value) break
      if (data.phase === 'REST') {
        status.value = 'RESTING'
        stopTick()
        playChime('rest')
        ElNotification({ title: '番茄钟 · 专注结束', message: '完成一个番茄，休息一下吧，到点会自动进入下一轮', type: 'info' })
      } else if (data.phase === 'FOCUS') {
        status.value = 'STUDYING'
        playChime('focus')
        ElNotification({ title: '番茄钟 · 休息结束', message: '开始新的专注时段，加油！', type: 'success' })
        startTick()
      }
      break
    }
    case 'CHECKIN_DONE':
      notify(`${data.nickname} 完成了一次 ${Math.floor(data.durationSeconds / 60)} 分钟的专注`, 'success')
      break
    case 'NOTICE':
      notify(data.text, data.level)
      break
    case 'KICKED':
      ElMessage.error(data.reason || '你被移出房间')
      ws?.close()
      router.push('/lobby')
      break
  }
}

function notify(text: string, level: string = 'info') {
  ElNotification({
    title: '消息', message: text,
    type: level === 'success' ? 'success' : level === 'warn' ? 'warning' : 'info',
    duration: 3000
  })
}

function startFocus() {
  primeAudio()   // 用户手势中预热音频上下文，保证后续自动提示音可播放
  status.value = 'STUDYING'
  ws?.send({ type: 'STATUS_CHANGE', data: { status: 'STUDYING' } })
  startTick()
}

function rest() {
  status.value = 'RESTING'
  ws?.send({ type: 'STATUS_CHANGE', data: { status: 'RESTING' } })
  stopTick()
}

async function finish() {
  const seconds = elapsed.value
  let diaryContent = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `本次专注 ${Math.floor(seconds / 60)} 分钟，写一句感想吧（可选）`,
      '打卡',
      {
        confirmButtonText: '提交打卡',
        cancelButtonText: '直接结束',
        inputPlaceholder: '今天状态不错，坚持就是胜利～',
        inputValidator: (v) => !v || v.length <= 200 || '感想最多 200 字'
      }
    )
    diaryContent = value || ''
  } catch {
    // 用户选择直接结束或关闭弹窗
  }

  ws?.send({ type: 'FOCUS_FINISH', data: {
    durationSeconds: seconds,
    mode: room.value?.pomodoroMode ? 'POMODORO' : 'FREE',
    diaryContent
  }})
  ElMessage.success('本次专注已记录')
  status.value = 'IDLE'
  stopTick()
  elapsed.value = 0
}

async function moderate(m: Member, action: 'kick' | 'ban') {
  if (m.userId == null) return
  const verb = action === 'kick' ? '移出房间' : '封禁（此后无法再加入）'
  try {
    await ElMessageBox.confirm(`确定将「${m.nickname}」${verb}吗？`, action === 'kick' ? '移出成员' : '封禁成员', {
      type: 'warning', confirmButtonText: '确定', cancelButtonText: '取消'
    })
  } catch { return }
  try {
    await api.post(`/api/room/${roomId}/${action}`, { targetUserId: m.userId })
    ElMessage.success(action === 'kick' ? '已移出房间' : '已封禁')
    members.value = members.value.filter(x => x.clientKey !== m.clientKey)
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

function statusTag(m: Member) {
  return m.status === 'STUDYING' ? 'success' : m.status === 'RESTING' ? 'warning' : 'info'
}
</script>

<template>
  <div class="page-wrap" v-if="room">
    <div class="room-header">
      <div>
        <h2>{{ room.name }}</h2>
        <span class="room-no">房间号 {{ room.roomNo }} · {{ members.length }} 人在线</span>
      </div>
      <el-tag v-if="room.pomodoroMode" type="success">番茄钟 {{ room.focusMinutes }}+{{ room.restMinutes }} 自动循环</el-tag>
    </div>

    <el-row :gutter="16">
      <el-col :xs="24" :md="15">
        <div class="timer-card hb-card">
          <div class="status-line">
            <el-tag :type="status === 'STUDYING' ? 'success' : status === 'RESTING' ? 'warning' : 'info'" effect="dark">
              {{ statusText }}
            </el-tag>
          </div>
          <div class="timer-display">{{ timeText }}</div>
          <div class="timer-actions">
            <el-button type="primary" size="large" :disabled="status === 'STUDYING'" @click="startFocus">
              开始学习
            </el-button>
            <el-button size="large" :disabled="status !== 'STUDYING'" @click="rest">休息一下</el-button>
            <el-button type="success" size="large" :disabled="status === 'IDLE'" @click="finish">结束并记录</el-button>
          </div>
          <p class="timer-tip">计时以服务端为准；番茄钟模式专注/休息到点自动切换并提示</p>
        </div>

        <div class="noise-card hb-card">
          <WhiteNoise />
        </div>
      </el-col>

      <el-col :xs="24" :md="9">
        <div class="member-card hb-card">
          <div class="member-title">在线成员 <span>{{ members.length }}</span></div>
          <div v-if="members.length" class="member-list">
            <div v-for="m in members" :key="m.clientKey" class="member-item">
              <el-avatar :size="36">{{ m.anonymous ? '匿' : m.nickname.charAt(0) }}</el-avatar>
              <div class="member-info">
                <div class="member-name">
                  {{ m.nickname }}
                  <el-tag v-if="m.owner" size="small" type="warning">房主</el-tag>
                </div>
                <el-tag :type="statusTag(m) as any" size="small" effect="plain">{{ m.status === 'STUDYING' ? '学习中' : m.status === 'RESTING' ? '休息中' : '空闲' }}</el-tag>
              </div>
              <div v-if="isOwner && !m.owner && m.userId != null" class="member-actions">
                <el-button link type="danger" size="small" @click="moderate(m, 'kick')">移出</el-button>
                <el-button link type="danger" size="small" @click="moderate(m, 'ban')">封禁</el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无其他成员" :image-size="60" />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.room-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; }
.room-header h2 { margin: 0; }
.room-no { color: #9095a5; font-size: 13px; }
.timer-card { padding: 28px 20px; text-align: center; margin-bottom: 16px; }
.status-line { margin-bottom: 12px; }
.timer-actions { display: flex; gap: 12px; justify-content: center; margin-top: 28px; flex-wrap: wrap; }
.timer-tip { color: #b0b4c0; font-size: 12px; margin-top: 18px; }
.noise-card { padding: 18px 20px; }
.member-card { padding: 18px; }
.member-title { font-size: 15px; font-weight: 500; margin-bottom: 14px; }
.member-title span { color: var(--hb-primary); }
.member-list { display: flex; flex-direction: column; gap: 12px; }
.member-item { display: flex; align-items: center; gap: 10px; }
.member-info { flex: 1; display: flex; justify-content: space-between; align-items: center; }
.member-name { display: flex; align-items: center; gap: 6px; font-size: 14px; }
.member-actions { display: flex; gap: 2px; }
</style>
