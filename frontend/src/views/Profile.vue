<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { api } from '../api'

echarts.use([BarChart, TitleComponent, TooltipComponent, GridComponent, CanvasRenderer])
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const diary = ref<any[]>([])
const totalSeconds = ref(0)
const chartEl = ref<HTMLElement | null>(null)

function fmtDuration(sec: number) {
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}小时${m}分` : `${m}分钟`
}
function fmtTime(v: any) {
  if (!v) return ''
  return String(v).replace('T', ' ').slice(0, 16)
}

onMounted(async () => {
  try {
    const [d, t] = await Promise.all([
      api.get<{ records: any[] }>('/api/user/diary'),
      api.get<{ points: any[] }>('/api/user/trend')
    ])
    diary.value = d.records || []
    totalSeconds.value = userStore.user?.totalFocusSeconds || 0
    await nextTick()
    if (chartEl.value) {
      const chart = echarts.init(chartEl.value)
      const points = t.points || []
      chart.setOption({
        title: { text: '近 14 天专注时长', textStyle: { fontSize: 14 } },
        tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].axisValue}<br/>${fmtDuration(p[0].data.value ?? p[0].data)}` },
        grid: { left: 50, right: 20, top: 40, bottom: 30 },
        xAxis: { type: 'category', data: points.map((p: any) => p.day) },
        yAxis: { type: 'value', name: '分钟', min: 0 },
        series: [{
          type: 'bar',
          data: points.map((p: any) => Math.round((p.total || 0) / 60)),
          itemStyle: { color: '#6366f1', borderRadius: [4, 4, 0, 0] }
        }]
      })
      window.addEventListener('resize', () => chart.resize())
    }
  } catch { /* ignore */ }
})
</script>

<template>
  <div class="page-wrap">
    <div class="profile-head hb-card">
      <el-avatar :size="64" style="background:#6366f1">{{ userStore.user?.nickname.charAt(0) }}</el-avatar>
      <div class="profile-meta">
        <h2>{{ userStore.user?.nickname }}</h2>
        <span>累计专注 <b>{{ fmtDuration(totalSeconds) }}</b></span>
      </div>
    </div>

    <div class="chart-card hb-card">
      <div ref="chartEl" style="height:260px"></div>
    </div>

    <div class="diary-card hb-card">
      <h3>打卡日记</h3>
      <el-timeline v-if="diary.length">
        <el-timeline-item v-for="(d, i) in diary" :key="i" :timestamp="fmtTime(d.createdAt)" placement="top">
          <div class="diary-item">
            <el-tag size="small" type="primary">专注 {{ fmtDuration(d.durationSeconds) }}</el-tag>
            <span v-if="d.roomName" class="diary-room">在 {{ d.roomName }}</span>
            <p v-if="d.content">{{ d.content }}</p>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="还没有打卡记录，去自习室开始吧" />
    </div>
  </div>
</template>

<style scoped>
.profile-head { display: flex; align-items: center; gap: 18px; padding: 24px; margin-bottom: 16px; }
.profile-meta h2 { margin: 0 0 6px; }
.profile-meta span { color: #9095a5; font-size: 14px; }
.profile-meta b { color: var(--hb-primary); font-size: 18px; }
.chart-card { padding: 16px; margin-bottom: 16px; }
.diary-card { padding: 20px 24px; }
.diary-card h3 { margin: 0 0 18px; }
.diary-item p { margin: 8px 0 0; color: #606a7a; }
.diary-room { color: #a0a5b5; font-size: 13px; margin-left: 8px; }
</style>
