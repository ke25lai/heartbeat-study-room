<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { api } from '../api'
import { useUserStore } from '../store/user'

const userStore = useUserStore()
const period = ref<'daily' | 'weekly'>('daily')
const groupByClass = ref(false)
const result = ref<any>({})

function fmt(sec: number) {
  const h = Math.floor(sec / 3600)
  const m = Math.floor((sec % 3600) / 60)
  return h > 0 ? `${h}h ${m}m` : `${m}m`
}
function medal(rank: number) {
  return ['🥇', '🥈', '🥉'][rank - 1] || ''
}

async function load() {
  try {
    result.value = await api.get(`/api/rank/${period.value}?groupByClass=${groupByClass.value}`)
  } catch { /* ignore */ }
}
watch([period, groupByClass], load)
onMounted(load)
</script>

<template>
  <div class="page-wrap">
    <div class="rank-head">
      <h2>专注排行榜</h2>
      <div class="rank-controls">
        <el-radio-group v-model="period" size="default">
          <el-radio-button value="daily">每日</el-radio-button>
          <el-radio-button value="weekly">每周</el-radio-button>
        </el-radio-group>
        <el-switch v-model="groupByClass" active-text="按班级分组" />
      </div>
    </div>

    <template v-if="!groupByClass">
      <div class="rank-card hb-card">
        <div v-if="result.list?.length" class="rank-list">
          <div v-for="row in result.list" :key="row.userId" class="rank-row"
               :class="{ me: row.userId === userStore.user?.id }">
            <span class="rank-num">{{ medal(row.rank) || '#' + row.rank }}</span>
            <el-avatar :size="36">{{ row.nickname?.charAt(0) }}</el-avatar>
            <span class="rank-name">{{ row.nickname }}</span>
            <span class="rank-time">{{ fmt(row.totalSeconds) }}</span>
          </div>
        </div>
        <el-empty v-else description="暂无排行数据" />
      </div>
    </template>

    <template v-else>
      <div v-for="g in result.groups" :key="g.classId" class="rank-group hb-card">
        <div class="group-title">{{ g.className }}</div>
        <div v-for="row in g.list" :key="row.userId" class="rank-row" :class="{ me: row.userId === userStore.user?.id }">
          <span class="rank-num">{{ medal(row.rank) || '#' + row.rank }}</span>
          <el-avatar :size="32">{{ row.nickname?.charAt(0) }}</el-avatar>
          <span class="rank-name">{{ row.nickname }}</span>
          <span class="rank-time">{{ fmt(row.totalSeconds) }}</span>
        </div>
      </div>
      <el-empty v-if="!result.groups?.length" description="暂无排行数据" />
    </template>
  </div>
</template>

<style scoped>
.rank-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 18px; flex-wrap: wrap; gap: 12px; }
.rank-head h2 { margin: 0; }
.rank-controls { display: flex; align-items: center; gap: 16px; }
.rank-card, .rank-group { padding: 12px 20px; margin-bottom: 16px; }
.group-title { font-weight: 500; padding: 8px 0; border-bottom: 1px solid #eef0f5; margin-bottom: 8px; }
.rank-row { display: flex; align-items: center; gap: 14px; padding: 10px 8px; border-radius: 8px; }
.rank-row.me { background: #f5f5ff; }
.rank-num { width: 40px; font-size: 16px; text-align: center; }
.rank-name { flex: 1; }
.rank-time { color: var(--hb-primary); font-weight: 500; font-variant-numeric: tabular-nums; }
</style>
