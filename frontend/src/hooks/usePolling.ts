import { useEffect, useRef } from 'react'

/**
 * 轮询：立即执行一次，之后按固定间隔重复。
 *
 * <p>这是刻意的设计选择——项目明确不引入 WebSocket（见 PROJECT_PLAN 第 5.4 节）。
 * 本地单用户场景下，一个 5 秒的轮询和数据推送的体验差别很小，
 * 却省掉了连接管理、重连、心跳这一整套复杂度。
 *
 * @param callback 每次轮询执行的动作
 * @param intervalMs 间隔毫秒数
 */
export function usePolling(callback: () => void | Promise<void>, intervalMs: number) {
  // 用 ref 保存最新的回调，避免调用方每次渲染都重新订阅定时器
  const savedCallback = useRef(callback)

  useEffect(() => {
    savedCallback.current = callback
  }, [callback])

  useEffect(() => {
    let active = true

    const tick = () => {
      if (active) {
        void savedCallback.current()
      }
    }

    tick()
    const timer = window.setInterval(tick, intervalMs)

    return () => {
      active = false
      window.clearInterval(timer)
    }
  }, [intervalMs])
}
