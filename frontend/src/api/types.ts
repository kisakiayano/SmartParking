export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface PageResponse<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
}

export interface PageQuery {
  pageNum: number
  pageSize: number
}

export type Id = string

export interface User {
  userId: Id
  username: string
  name: string
  phone: string
  role: 'ADMIN' | 'USER' | string
  status: string
  createTime: string
  updateTime: string
}

export interface UserQuery extends PageQuery {
  username?: string
  name?: string
  phone?: string
  role?: string
  status?: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface RegisterRequest {
  username: string
  password: string
  name: string
  phone: string
}

export interface LoginResult {
  token: string
  user: User
}

export interface Vehicle {
  vehicleId: Id
  plateNumber: string
  vehicleType: string
  userId: Id
  createTime: string
  updateTime: string
}

export interface VehiclePayload {
  plateNumber: string
  vehicleType: string
}

export interface VehicleQuery extends PageQuery {
  plateNumber?: string
  vehicleType?: string
  userId?: Id
  sortBy?: string
  order?: 'asc' | 'desc'
}

export interface ParkingSpace {
  spaceId: Id
  area: string
  type: string
  status: string
  version: number
  createTime: string
  updateTime: string
}

export interface ParkingSpacePayload {
  area: string
  type: string
  status: string
  version?: number
}

export interface ParkingSpaceQuery extends PageQuery {
  area?: string
  type?: string
  status?: string
}

export interface Reservation {
  reservationId: Id
  userId: Id
  vehicleId: Id
  spaceId: Id
  reserveTime: string
  arriveTime: string
  status: string
  createTime: string
  updateTime: string
}

export interface ReservationPayload {
  vehicleId: Id
  spaceId: Id
  arriveTime: string
}

export interface ReservationQuery extends PageQuery {
  status?: string
  userId?: Id
  vehicleId?: Id
  spaceId?: Id
}

export interface ParkingRecord {
  recordId: Id
  vehicleId: Id
  spaceId: Id
  entryTime: string
  exitTime: string | null
  duration: number | null
  fee: number | string | null
  status: string
  createTime: string
  updateTime: string
}

export interface ParkingRecordQuery extends PageQuery {
  status?: string
  vehicleId?: Id
  spaceId?: Id
}

export interface FeeRule {
  ruleId: Id
  ruleName: string
  unitPrice: number | string
  freeMinute: number
  maxDaily: number | string
  enabled: boolean
  createTime: string
  updateTime: string
}

export interface FeeRulePayload {
  ruleName: string
  unitPrice: number | string
  freeMinute: number
  maxDaily: number | string
  enabled: boolean
}

export interface FeeRuleQuery extends PageQuery {
  enabled?: boolean
  ruleName?: string
}

export interface TrendPoint {
  date: string
  value: number | string
}

export interface DashboardStatistics {
  todayIncome: number | string
  todayParkingCount: number
  totalSpaces: number
  freeSpaces: number
  reservedSpaces: number
  occupiedSpaces: number
  currentOccupancyRate: number | string
  revenueTrend7Days: TrendPoint[]
  revenueTrend30Days: TrendPoint[]
  trafficTrend7Days: TrendPoint[]
  trafficTrend30Days: TrendPoint[]
}

export interface AiChatRequest {
  sessionId?: Id | null
  question: string
}

export interface AiChatReply {
  sessionId: Id
  answer: string
  intent: string
  provider: string
  referencedData: string[]
  createTime: string
}

export interface AiChatSession {
  sessionId: Id
  userId: Id
  title: string
  provider: string
  messageCount: number
  lastMessageTime: string
  createTime: string
  updateTime: string
}

export interface AiChatSessionQuery extends PageQuery {
  userId?: Id
}

export interface AiChatMessage {
  messageId: Id
  sessionId: Id
  role: 'USER' | 'ASSISTANT' | string
  content: string
  intent: string | null
  provider: string | null
  referencedData: string[]
  createTime: string
}

export interface AiFaq {
  question: string
  category: string
}

export interface AiPromptTemplate {
  templateId: Id
  templateCode: string
  templateName: string
  intent: string | null
  systemPrompt: string
  enabled: boolean
  createTime: string
  updateTime: string
}

export interface AiPromptTemplatePayload {
  templateName: string
  systemPrompt: string
  enabled: boolean
}

export interface Suggestion {
  suggestionId: Id
  userId: Id
  category: string
  title: string
  content: string
  contact: string | null
  status: string
  reply: string | null
  handlerId: Id | null
  handleTime: string | null
  createTime: string
  updateTime: string
}

export interface SuggestionPayload {
  category: string
  title: string
  content: string
  contact?: string
}

export interface SuggestionQuery extends PageQuery {
  status?: string
  category?: string
  keyword?: string
}

export interface SuggestionHandlePayload {
  status: string
  reply?: string
}
