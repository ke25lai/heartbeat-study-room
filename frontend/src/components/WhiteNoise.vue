<script setup lang="ts">
import { ref, onUnmounted } from 'vue'

/**
 * 白噪音：Web Audio API 实时合成，零外部资源、零版权问题、点击即响。
 *
 * 面试可讲点：
 * - AudioContext 受浏览器自动播放策略限制，必须在用户手势（点击）中创建/恢复；
 * - AudioBuffer 填充采样生成噪声，AudioBufferSourceNode.loop 无缝循环；
 * - 白噪声 / 粉噪声（Paul Kellet 近似）频谱不同：粉噪声低频更重，听感更像环境音；
 * - BiquadFilterNode 做低通/带通塑形，GainNode 做平滑音量控制（setTargetAtTime 防爆音）。
 */
type TrackKey = 'rain' | 'cafe' | 'white'
interface Track { key: TrackKey; name: string; icon: string; desc: string }
const tracks: Track[] = [
  { key: 'rain',  name: '雨声',   icon: '🌧', desc: '轻柔雨幕' },
  { key: 'cafe',  name: '咖啡馆', icon: '☕', desc: '环境人声' },
  { key: 'white', name: '白噪音', icon: '🎚', desc: '纯白噪声' }
]

let ctx: AudioContext | null = null
let source: AudioBufferSourceNode | null = null
let gain: GainNode | null = null
const current = ref<TrackKey | null>(null)
const volume = ref(0.5)

function ensureCtx(): AudioContext | null {
  try {
    if (!ctx) ctx = new AudioContext()
    if (ctx.state === 'suspended') void ctx.resume()
    return ctx
  } catch {
    return null
  }
}

/** 生成 2 秒噪声缓冲，循环播放 */
function buildBuffer(audio: AudioContext, key: TrackKey): AudioBuffer {
  const length = Math.floor(audio.sampleRate * 2)
  const buffer = audio.createBuffer(1, length, audio.sampleRate)
  const data = buffer.getChannelData(0)

  if (key === 'white') {
    // 纯白噪声：各频率等能量
    for (let i = 0; i < length; i++) data[i] = Math.random() * 2 - 1
  } else if (key === 'rain') {
    // 白噪声 + 一阶低通叠加，听感接近沙沙雨声
    let last = 0
    for (let i = 0; i < length; i++) {
      const white = Math.random() * 2 - 1
      last = (last + 0.02 * white) / 1.02
      data[i] = white * 0.6 + last * 3
    }
  } else {
    // 粉噪声（Paul Kellet 近似）：低频更重，像咖啡馆嘈杂人声底噪
    let b0 = 0, b1 = 0, b2 = 0, b3 = 0, b4 = 0, b5 = 0, b6 = 0
    for (let i = 0; i < length; i++) {
      const white = Math.random() * 2 - 1
      b0 = 0.99886 * b0 + white * 0.0555179
      b1 = 0.99332 * b1 + white * 0.0750759
      b2 = 0.96900 * b2 + white * 0.1538520
      b3 = 0.86650 * b3 + white * 0.3104856
      b4 = 0.55000 * b4 + white * 0.5329522
      b5 = -0.7616 * b5 - white * 0.0168980
      data[i] = (b0 + b1 + b2 + b3 + b4 + b5 + b6 + white * 0.5362) * 0.11
      b6 = white * 0.115926
    }
  }
  return buffer
}

function stop() {
  if (source) {
    try { source.stop() } catch { /* 已停止 */ }
    source.disconnect()
    source = null
  }
}

function toggle(t: Track) {
  if (current.value === t.key) { stop(); current.value = null; return }
  const audio = ensureCtx()
  if (!audio) return
  stop()

  gain = audio.createGain()
  gain.gain.value = volume.value

  const filter = audio.createBiquadFilter()
  if (t.key === 'rain') {
    filter.type = 'lowpass'; filter.frequency.value = 2000
  } else if (t.key === 'cafe') {
    filter.type = 'bandpass'; filter.frequency.value = 800; filter.Q.value = 0.4
  } else {
    filter.type = 'lowpass'; filter.frequency.value = 12000
  }

  source = audio.createBufferSource()
  source.buffer = buildBuffer(audio, t.key)
  source.loop = true
  source.connect(filter)
  filter.connect(gain)
  gain.connect(audio.destination)
  source.start()
  current.value = t.key
}

function onVolume() {
  if (gain && ctx) gain.gain.setTargetAtTime(volume.value, ctx.currentTime, 0.02)
}

onUnmounted(() => {
  stop()
  if (ctx) void ctx.close()
})
</script>

<template>
  <div class="noise">
    <div class="noise-title">白噪音 <span class="badge">实时合成</span></div>
    <div class="noise-tracks">
      <button v-for="t in tracks" :key="t.key" class="noise-btn"
              :class="{ active: current === t.key }" @click="toggle(t)">
        <span class="ico">{{ t.icon }}</span>
        <span>{{ t.name }}</span>
        <span class="desc">{{ t.desc }}</span>
      </button>
    </div>
    <div class="noise-vol">
      <span>音量</span>
      <el-slider v-model="volume" :min="0" :max="1" :step="0.05" @update:model-value="onVolume" />
    </div>
    <p class="noise-tip">点击即响，无需下载音频文件；再次点击同一项可停止</p>
  </div>
</template>

<style scoped>
.noise-title { font-size: 13px; color: #9095a5; margin-bottom: 10px; display: flex; align-items: center; gap: 6px; }
.badge { font-size: 10px; color: var(--hb-primary); background: #f0f0ff; padding: 1px 6px; border-radius: 8px; }
.noise-tracks { display: flex; gap: 8px; flex-wrap: wrap; }
.noise-btn {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  min-width: 78px; padding: 8px 10px; border: 1px solid #e8eaf0; border-radius: 10px;
  background: #fff; cursor: pointer; font-size: 12px; color: #606a7a;
}
.noise-btn .ico { font-size: 18px; }
.noise-btn .desc { font-size: 10px; color: #b0b4c0; }
.noise-btn.active { border-color: var(--hb-primary); color: var(--hb-primary); background: #f5f5ff; }
.noise-btn.active .desc { color: var(--hb-primary); }
.noise-vol { display: flex; align-items: center; gap: 10px; margin-top: 14px; font-size: 12px; color: #9095a5; }
.noise-vol :deep(.el-slider) { flex: 1; }
.noise-tip { font-size: 11px; color: #c0c4d0; margin: 10px 0 0; }
</style>
