import { request } from './http'
import type {
  AiChatMessage,
  AiChatReply,
  AiChatRequest,
  AiChatSession,
  AiChatSessionQuery,
  AiFaq,
  AiPromptTemplate,
  AiPromptTemplatePayload,
  DashboardStatistics,
  FeeRule,
  FeeRulePayload,
  FeeRuleQuery,
  LoginRequest,
  LoginResult,
  PageResponse,
  ParkingRecord,
  ParkingRecordQuery,
  ParkingSpace,
  ParkingSpacePayload,
  ParkingSpaceQuery,
  RegisterRequest,
  Reservation,
  ReservationPayload,
  ReservationQuery,
  Suggestion,
  SuggestionHandlePayload,
  SuggestionPayload,
  SuggestionQuery,
  User,
  UserQuery,
  Vehicle,
  VehiclePayload,
  VehicleQuery,
  Id,
} from './types'

function cleanParams<T extends Record<string, unknown>>(params: T) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== '' && value !== null && value !== undefined),
  )
}

export const authApi = {
  login(data: LoginRequest) {
    return request<LoginResult>({ url: '/auth/login', method: 'post', data })
  },
  register(data: RegisterRequest) {
    return request<User>({ url: '/auth/register', method: 'post', data })
  },
  me() {
    return request<User>({ url: '/users/me', method: 'get' })
  },
}

export const userApi = {
  updateProfile(data: Pick<User, 'name' | 'phone'>) {
    return request<User>({ url: '/users/me', method: 'put', data })
  },
  pageAdmin(params: UserQuery) {
    return request<PageResponse<User>>({
      url: '/admin/users',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  updateStatus(userId: Id, status: string) {
    return request<User>({ url: `/admin/users/${userId}/status`, method: 'put', data: { status } })
  },
}

export const vehicleApi = {
  listMine() {
    return request<Vehicle[]>({ url: '/vehicles', method: 'get' })
  },
  create(data: VehiclePayload) {
    return request<Vehicle>({ url: '/vehicles', method: 'post', data })
  },
  update(vehicleId: Id, data: VehiclePayload) {
    return request<Vehicle>({ url: `/vehicles/${vehicleId}`, method: 'put', data })
  },
  delete(vehicleId: Id) {
    return request<void>({ url: `/vehicles/${vehicleId}`, method: 'delete' })
  },
  pageAdmin(params: VehicleQuery) {
    return request<PageResponse<Vehicle>>({
      url: '/admin/vehicles',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
}

export const parkingSpaceApi = {
  page(params: ParkingSpaceQuery) {
    return request<PageResponse<ParkingSpace>>({
      url: '/parking-spaces',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  createAdmin(data: ParkingSpacePayload) {
    return request<ParkingSpace>({ url: '/admin/parking-spaces', method: 'post', data })
  },
  updateAdmin(spaceId: Id, data: Required<ParkingSpacePayload>) {
    return request<ParkingSpace>({ url: `/admin/parking-spaces/${spaceId}`, method: 'put', data })
  },
  deleteAdmin(spaceId: Id) {
    return request<void>({ url: `/admin/parking-spaces/${spaceId}`, method: 'delete' })
  },
}

export const reservationApi = {
  create(data: ReservationPayload) {
    return request<Reservation>({ url: '/reservations', method: 'post', data })
  },
  pageMine(params: ReservationQuery) {
    return request<PageResponse<Reservation>>({
      url: '/reservations',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  cancel(reservationId: Id) {
    return request<void>({ url: `/reservations/${reservationId}/cancel`, method: 'put' })
  },
  pageAdmin(params: ReservationQuery) {
    return request<PageResponse<Reservation>>({
      url: '/admin/reservations',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
}

export const parkingRecordApi = {
  pageMine(params: ParkingRecordQuery) {
    return request<PageResponse<ParkingRecord>>({
      url: '/parking-records',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  pageAdmin(params: ParkingRecordQuery) {
    return request<PageResponse<ParkingRecord>>({
      url: '/admin/parking-records',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  manualEntry(data: { plateNumber: string; spaceId: Id }) {
    return request<ParkingRecord>({ url: '/admin/parking-records/entry', method: 'post', data })
  },
  manualExit(data: { plateNumber: string }) {
    return request<ParkingRecord>({ url: '/admin/parking-records/exit', method: 'post', data })
  },
}

export const feeRuleApi = {
  pageAdmin(params: FeeRuleQuery) {
    return request<PageResponse<FeeRule>>({
      url: '/admin/fee-rules',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  createAdmin(data: FeeRulePayload) {
    return request<FeeRule>({ url: '/admin/fee-rules', method: 'post', data })
  },
  updateAdmin(ruleId: Id, data: FeeRulePayload) {
    return request<FeeRule>({ url: `/admin/fee-rules/${ruleId}`, method: 'put', data })
  },
  enable(ruleId: Id) {
    return request<FeeRule>({ url: `/admin/fee-rules/${ruleId}/enable`, method: 'put' })
  },
}

export const dashboardApi = {
  statistics() {
    return request<DashboardStatistics>({ url: '/admin/dashboard/statistics', method: 'get' })
  },
}

export const aiApi = {
  chat(data: AiChatRequest) {
    return request<AiChatReply>({ url: '/ai/chat', method: 'post', data })
  },
  faq() {
    return request<AiFaq[]>({ url: '/ai/faq', method: 'get' })
  },
  sessions(params: AiChatSessionQuery) {
    return request<PageResponse<AiChatSession>>({
      url: '/ai/chat-sessions',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  messages(sessionId: Id) {
    return request<AiChatMessage[]>({ url: `/ai/chat-sessions/${sessionId}/messages`, method: 'get' })
  },
  deleteSession(sessionId: Id) {
    return request<void>({ url: `/ai/chat-sessions/${sessionId}`, method: 'delete' })
  },
  pageAdminSessions(params: AiChatSessionQuery) {
    return request<PageResponse<AiChatSession>>({
      url: '/admin/ai/chat-sessions',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  adminMessages(sessionId: Id) {
    return request<AiChatMessage[]>({ url: `/admin/ai/chat-sessions/${sessionId}/messages`, method: 'get' })
  },
  prompts() {
    return request<AiPromptTemplate[]>({ url: '/admin/ai/prompts', method: 'get' })
  },
  updatePrompt(templateId: Id, data: AiPromptTemplatePayload) {
    return request<AiPromptTemplate>({ url: `/admin/ai/prompts/${templateId}`, method: 'put', data })
  },
}

export const suggestionApi = {
  create(data: SuggestionPayload) {
    return request<Suggestion>({ url: '/suggestions', method: 'post', data })
  },
  pageMine(params: SuggestionQuery) {
    return request<PageResponse<Suggestion>>({
      url: '/suggestions',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  getMine(suggestionId: Id) {
    return request<Suggestion>({ url: `/suggestions/${suggestionId}`, method: 'get' })
  },
  pageAdmin(params: SuggestionQuery) {
    return request<PageResponse<Suggestion>>({
      url: '/admin/suggestions',
      method: 'get',
      params: cleanParams(params as unknown as Record<string, unknown>),
    })
  },
  getAdmin(suggestionId: Id) {
    return request<Suggestion>({ url: `/admin/suggestions/${suggestionId}`, method: 'get' })
  },
  handle(suggestionId: Id, data: SuggestionHandlePayload) {
    return request<Suggestion>({ url: `/admin/suggestions/${suggestionId}`, method: 'put', data })
  },
}
