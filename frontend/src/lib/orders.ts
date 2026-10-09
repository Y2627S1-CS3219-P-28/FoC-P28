export type OrderMode = "requester" | "courier"

export type OrderStatus =
  | "OPEN"
  | "ACCEPTED"
  | "IN_PROGRESS"
  | "PICKED_UP"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "ABORTED"
  | "EXPIRED"

export type Order = {
  id: string
  attemptId?: string | null
  requesterId: string
  courierId: string | null
  itemDescription: string
  pickupSupplierId: string
  deliverySupplierId: string
  offeredCredits: number
  status: OrderStatus
  createdAt: string
  expiresAt: string
  deliveryTimeLimitMinutes: number
  version: number
  originalOrderId: string | null
  repostedOrderId: string | null
  automaticRepostEnabled?: boolean
  repostDueAt?: string | null
  repostExpiresAt?: string | null
  repostFailureCode?: string | null
  repostFailureMessage?: string | null
  repostFailureAt?: string | null
  repostCreditAmount?: number
  repostDeliveryDurationMinutes?: number
}

export type OrderPage = {
  items: Order[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type CreateOrderForm = {
  itemDescription: string
  pickupSupplierId: string
  deliverySupplierId: string
  offeredCredits: number
  deliveryTimeLimitMinutes: number
  expiresAt: string
  automaticRepost: boolean
  repostDueAt: string
  repostExpiresAt: string
  repostCreditAmount: number
  repostDeliveryDurationMinutes: number
}

export type CreateOrderPayload = {
  commandId: string
  requesterId: string
  itemDescription: string
  pickupSupplierId: string
  deliverySupplierId: string
  offeredCredits: number
  deliveryTimeLimitMinutes: number
  expiresAt: string
  automaticRepost: boolean
  repostDueAt: string | null
  repostExpiresAt: string | null
  repostCreditAmount: number
  repostDeliveryDurationMinutes: number
}

export type RepostConfigPayload = {
  commandId: string
  actorId: string
  expectedVersion: number
  enabled: boolean
  dueAt: string
  creditAmount: number
  deliveryDurationMinutes: number
}

export type OrderActionPayload = {
  commandId: string
  actorId: string
  expectedVersion: number
}

export const MIN_ORDER_EXPIRY_MINUTES = 30

export function minOrderExpiryDateTimeLocal(now = new Date()): string {
  return quarterHourDateTimeLocal(new Date(now.getTime() + MIN_ORDER_EXPIRY_MINUTES * 60_000))
}

export function validateCreateOrderForm(form: CreateOrderForm, now = new Date()): string | null {
  const expiry = new Date(form.expiresAt)
  if (Number.isNaN(expiry.getTime())) return "Choose an order expiry time."
  if (expiry.getTime() < now.getTime() + MIN_ORDER_EXPIRY_MINUTES * 60_000) {
    return "Order expiry must be at least 30 minutes from now. Choose a later time."
  }
  if (!isQuarterHourDateTime(form.expiresAt)) return "Choose expiry minutes of 00, 15, 30, or 45."
  if (form.automaticRepost) {
    const repostDueAt = new Date(form.repostDueAt)
    if (Number.isNaN(repostDueAt.getTime())) return "Choose a repost time when automatic repost is enabled."
    if (!isQuarterHourDateTime(form.repostDueAt)) return "Choose repost minutes of 00, 15, 30, or 45."
    if (repostDueAt.getTime() < expiry.getTime()) return "Repost time must be at or after the original order expiry."
    const repostExpiry = new Date(form.repostExpiresAt)
    if (Number.isNaN(repostExpiry.getTime())) return "Choose a repost expiry when automatic repost is enabled."
    if (!isQuarterHourDateTime(form.repostExpiresAt)) return "Choose repost expiry minutes of 00, 15, 30, or 45."
    if (repostExpiry.getTime() <= repostDueAt.getTime()) return "Repost expiry must be later than the repost time."
    if (form.repostCreditAmount < 1) return "Repost credits must be at least 1."
    if (form.repostDeliveryDurationMinutes < 15) return "Repost delivery time must be at least 15 minutes."
  }
  return null
}

const commandId = () => crypto.randomUUID()

export function orderMinePath(mode: OrderMode, userId: string, page = 1): string {
  const params = new URLSearchParams({ mode, userId })
  params.set("page", String(Math.max(1, page)))
  params.set("size", "20")
  return `/api/orders/mine?${params.toString()}`
}

export function orderAvailablePath(page = 1): string {
  return `/api/orders/available?page=${Math.max(1, page)}&size=20`
}

export function buildCreateOrderPayload(form: CreateOrderForm, requesterId: string): CreateOrderPayload {
  return {
    commandId: commandId(),
    requesterId,
    itemDescription: form.itemDescription.trim(),
    pickupSupplierId: form.pickupSupplierId.trim(),
    deliverySupplierId: form.deliverySupplierId.trim(),
    offeredCredits: form.offeredCredits,
    deliveryTimeLimitMinutes: form.deliveryTimeLimitMinutes,
    expiresAt: new Date(form.expiresAt).toISOString(),
    automaticRepost: form.automaticRepost,
    repostDueAt: form.automaticRepost ? new Date(form.repostDueAt).toISOString() : null,
    repostExpiresAt: form.automaticRepost ? new Date(form.repostExpiresAt).toISOString() : null,
    repostCreditAmount: form.automaticRepost ? form.repostCreditAmount : 0,
    repostDeliveryDurationMinutes: form.automaticRepost ? form.repostDeliveryDurationMinutes : 0,
  }
}

export function buildRepostConfigPayload(form: CreateOrderForm, actorId: string, expectedVersion: number): RepostConfigPayload {
  return {
    commandId: commandId(),
    actorId,
    expectedVersion,
    enabled: form.automaticRepost,
    dueAt: new Date(form.repostDueAt).toISOString(),
    creditAmount: form.repostCreditAmount,
    deliveryDurationMinutes: form.repostDeliveryDurationMinutes,
  }
}

export function buildOrderActionPayload(actorId: string, expectedVersion: number): OrderActionPayload {
  return { commandId: commandId(), actorId, expectedVersion }
}

export function formatOrderStatus(status: OrderStatus): string {
  return status.replaceAll("_", " ").toLowerCase().replace(/(^| )\S/g, (letter) => letter.toUpperCase())
}

export function updateCourierOrderList(orders: Order[], updated: Order): Order[] {
  if (updated.status === "ABORTED" || (updated.status === "OPEN" && updated.courierId === null)) {
    return orders.filter((order) => order.attemptId || order.id !== updated.id)
  }

  return orders.map((order) => !order.attemptId && order.id === updated.id ? updated : order)
}

export function updateRequesterOrderList(orders: Order[], updated: Order): Order[] {
  if (updated.originalOrderId) {
    return [updated, ...orders.filter((order) => order.id !== updated.id &&
      !(order.id === updated.originalOrderId && order.status === "EXPIRED"))]
  }
  return orders.map((order) => order.id === updated.id ? updated : order)
}

export function isoToDateTimeLocal(iso: string): string {
  const date = new Date(iso)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

export function quarterHourDateTimeLocal(date: Date): string {
  const rounded = new Date(date.getTime())
  const minutes = rounded.getMinutes() + rounded.getSeconds() / 60 + rounded.getMilliseconds() / 60_000
  rounded.setMinutes(Math.ceil(minutes / 15) * 15, 0, 0)
  return isoToDateTimeLocal(rounded.toISOString())
}

export function isQuarterHourDateTime(value: string): boolean {
  const date = new Date(value)
  return !Number.isNaN(date.getTime()) && date.getMinutes() % 15 === 0
    && date.getSeconds() === 0 && date.getMilliseconds() === 0
}
