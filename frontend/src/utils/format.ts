/**
 * 时间与时长格式化。
 *
 * 后端已把时间统一输出为 `yyyy-MM-dd'T'HH:mm:ss` 的固定格式（见 JacksonConfig），
 * 所以这里可以安全地按位置截取，不必引入日期库。
 */

/** `2026-09-13T09:00:00` → `09:00` */
export function clockOf(iso: string | null): string {
  return iso ? iso.slice(11, 16) : '--:--'
}

/** `2026-09-13T09:00:00` → `2026-09-13` */
export function dateOf(iso: string): string {
  return iso.slice(0, 10)
}

/** 把分钟数写成中文时长：90 → `1 小时 30 分` */
export function formatDuration(minutes: number): string {
  if (minutes <= 0) return '0 分钟'
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const rest = minutes % 60
  return rest === 0 ? `${hours} 小时` : `${hours} 小时 ${rest} 分`
}

/** 时间区间文案：`09:00 - 11:30`，无结束时间时显示「进行中」 */
export function formatRange(start: string | null, end: string | null): string {
  if (!start) return '--:--'
  return end ? `${clockOf(start)} - ${clockOf(end)}` : `${clockOf(start)} 起`
}

/** 今天的日期字符串，用于接口默认参数。 */
export function today(): string {
  const now = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`
}
