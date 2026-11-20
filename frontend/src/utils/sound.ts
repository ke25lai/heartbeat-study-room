/**
 * 提示音工具：用 Web Audio API 实时合成，零外部资源、零版权问题。
 *
 * 面试可讲点：
 * - AudioContext 必须由用户手势（点击）后创建/恢复，否则浏览器自动播放策略会拦截；
 * - OscillatorNode + GainNode 做 ADSR 包络，避免方波骤起骤停的"爆音"；
 * - 单例复用 AudioContext，避免每次新建带来的资源开销。
 */
type Phase = 'focus' | 'rest'

let ctx: AudioContext | null = null

function getCtx(): AudioContext | null {
  try {
    if (!ctx) ctx = new AudioContext()
    if (ctx.state === 'suspended') void ctx.resume()
    return ctx
  } catch {
    return null
  }
}

/** 播放一个带平滑包络的音符 */
function tone(freq: number, startAt: number, duration: number, peak: number) {
  if (!ctx) return
  const osc = ctx.createOscillator()
  const gain = ctx.createGain()
  osc.type = 'sine'
  osc.frequency.setValueAtTime(freq, startAt)
  // ADSR：极短 attack + 平滑 decay/release，避免爆音
  gain.gain.setValueAtTime(0.0001, startAt)
  gain.gain.exponentialRampToValueAtTime(peak, startAt + 0.02)
  gain.gain.exponentialRampToValueAtTime(0.0001, startAt + duration)
  osc.connect(gain)
  gain.connect(ctx.destination)
  osc.start(startAt)
  osc.stop(startAt + duration + 0.02)
}

/**
 * 番茄钟阶段提示音：
 * - focus（休息结束→开始专注）：上行两音，明快；
 * - rest （专注结束→休息）：下行两音，柔和。
 */
export function playChime(phase: Phase) {
  const audio = getCtx()
  if (!audio) return
  const t = audio.currentTime
  if (phase === 'focus') {
    tone(660, t, 0.18, 0.18)
    tone(880, t + 0.16, 0.32, 0.18)
  } else {
    tone(620, t, 0.20, 0.16)
    tone(440, t + 0.18, 0.38, 0.16)
  }
}

/** 供首次用户手势预热 AudioContext（点击"开始学习"时调用可提升提示音可靠性） */
export function primeAudio() {
  getCtx()
}
