export function formatDateTime(value?: string | null) {
  if (!value) {
    return '-'
  }
  return value.replace('T', ' ').slice(0, 19)
}

export function formatMoney(value?: number | string | null) {
  if (value === null || value === undefined || value === '') {
    return '0.00'
  }
  const amount = Number(value)
  return Number.isFinite(amount) ? amount.toFixed(2) : String(value)
}

export function formatDuration(value?: number | null) {
  if (value === null || value === undefined) {
    return '-'
  }
  if (value < 60) {
    return `${value} 分钟`
  }
  const hours = Math.floor(value / 60)
  const minutes = value % 60
  return minutes > 0 ? `${hours} 小时 ${minutes} 分钟` : `${hours} 小时`
}

export function statusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    ACTIVE: '有效',
    CANCELLED: '已取消',
    EXPIRED: '已过期',
    USED: '已使用',
    FREE: '空闲',
    RESERVED: '已预约',
    OCCUPIED: '占用',
    PARKING: '停车中',
    FINISHED: '已完成',
    ENABLED: '启用',
    DISABLED: '禁用',
    NORMAL: '正常',
  }
  return status ? labels[status] ?? status : '-'
}

export function tagType(status?: string | null) {
  if (status === 'FREE' || status === 'ACTIVE' || status === 'FINISHED' || status === 'NORMAL' || status === 'ENABLED') {
    return 'success'
  }
  if (status === 'RESERVED' || status === 'PARKING') {
    return 'warning'
  }
  if (status === 'OCCUPIED' || status === 'EXPIRED') {
    return 'danger'
  }
  if (status === 'CANCELLED' || status === 'DISABLED') {
    return 'info'
  }
  return ''
}

export function toPositiveNumber(value: number | string | null | undefined) {
  const numberValue = Number(value)
  return Number.isFinite(numberValue) && numberValue > 0 ? numberValue : undefined
}

export function suggestionCategoryLabel(category?: string | null) {
  const labels: Record<string, string> = {
    FUNCTION: '功能建议',
    EXPERIENCE: '体验优化',
    BUG: '问题反馈',
    CHARGE: '收费相关',
    OTHER: '其他',
  }
  return category ? labels[category] ?? category : '-'
}

export function suggestionStatusLabel(status?: string | null) {
  const labels: Record<string, string> = {
    PENDING: '待处理',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    REJECTED: '已驳回',
  }
  return status ? labels[status] ?? status : '-'
}

export function suggestionStatusTag(status?: string | null) {
  if (status === 'RESOLVED') {
    return 'success'
  }
  if (status === 'PROCESSING') {
    return 'warning'
  }
  if (status === 'REJECTED') {
    return 'danger'
  }
  return 'info'
}
