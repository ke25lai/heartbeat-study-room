/** WebSocket 连接封装：自动重连 + 心跳保活 + 消息分发 */
export type MessageHandler = (msg: any) => void

export class WsConnection {
  private ws: WebSocket | null = null
  private url: string
  private handlers: MessageHandler[] = []
  private heartbeatTimer: number | null = null
  private reconnectTimer: number | null = null
  private closedByUser = false
  private reconnectAttempts = 0

  constructor(roomId: number) {
    const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
    this.url = `${protocol}//${location.host}/ws/room/${roomId}`
  }

  connect() {
    this.closedByUser = false
    this.ws = new WebSocket(this.url)
    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      this.startHeartbeat()
    }
    this.ws.onmessage = (e) => {
      try {
        const msg = JSON.parse(e.data)
        this.handlers.forEach(h => h(msg))
      } catch { /* ignore */ }
    }
    this.ws.onclose = () => {
      this.stopHeartbeat()
      if (!this.closedByUser) this.scheduleReconnect()
    }
    this.ws.onerror = () => { /* close 会触发重连 */ }
  }

  private startHeartbeat() {
    this.stopHeartbeat()
    this.heartbeatTimer = window.setInterval(() => {
      if (this.ws?.readyState === WebSocket.OPEN) {
        this.send({ type: 'HEARTBEAT' })
      }
    }, 25000)
  }

  private stopHeartbeat() {
    if (this.heartbeatTimer !== null) {
      clearInterval(this.heartbeatTimer)
      this.heartbeatTimer = null
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer !== null) return
    this.reconnectAttempts++
    const delay = Math.min(1000 * this.reconnectAttempts, 10000)
    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null
      this.connect()
    }, delay)
  }

  send(obj: unknown) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(obj))
    }
  }

  onMessage(handler: MessageHandler) {
    this.handlers.push(handler)
  }

  close() {
    this.closedByUser = true
    this.stopHeartbeat()
    if (this.reconnectTimer !== null) clearTimeout(this.reconnectTimer)
    this.ws?.close()
  }
}
