import type { ThemeConfig } from 'antd'

/**
 * 字体栈。
 *
 * 刻意不引入 Web 字体：这是一个本地运行的工具，外部字体既依赖网络、又拖慢首屏，
 * 中文渲染也不可控。改为按平台优先级挑选系统中文字体，离线可用且渲染稳定。
 */
export const FONT_STACK =
  '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", "Source Han Sans SC", sans-serif'

/** 正文墨色，比纯黑柔和，长时间看表格文字不刺眼。 */
export const INK = '#1c1f23'
/** 次要文字色。 */
export const MUTED = '#6b7480'
/** 主色：深青，与「编码」的活动色同源，避开组件库默认蓝。 */
export const PRIMARY = '#1f6f6b'

export const theme: ThemeConfig = {
  token: {
    colorPrimary: PRIMARY,
    colorTextBase: INK,
    colorBgLayout: '#f4f5f6',
    borderRadius: 6,
    fontFamily: FONT_STACK,
    fontSize: 14,
  },
  components: {
    Layout: { headerBg: '#12181a' },
    Card: { headerFontSize: 14 },
  },
}
