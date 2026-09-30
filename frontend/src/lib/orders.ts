export type OrderMode = "requester" | "courier"

export type OrderStatus =
  | "OPEN"
  | "ACCEPTED"
  | "IN_PROGRESS"
  | "PICKED_UP"
  | "DELIVERED"
  | "COMPLETED"
  | "CANCELLED"
  | "EXPIRED"

export type Order = {
  id: string
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
}

export type OrderPage = {
  content: Order[]
  number: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
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
  return isoToDateTimeLocal(new Date(now.getTime() + MIN_ORDER_EXPIRY_MINUTES * 60_000).toISOString())
}

export function validateCreateOrderForm(form: CreateOrderForm, now = new Date()): string | null {
  const expiry = new Date(form.expiresAt)
  if (Number.isNaN(expiry.getTime())) return "Choose an order expiry time."
  if (expiry.getTime() < now.getTime() + MIN_ORDER_EXPIRY_MINUTES * 60_000) {
    return "Order expiry must be at least 30 minutes from now. Choose a later time."
  }
  return null
}

const commandId = () => crypto.randomUUID()

export function orderMinePath(mode: OrderMode, userId: string, page = 0): string {
  const params = new URLSearchParams({ mode, userId })
  if (page > 0) params.set("page", String(page))
  params.set("size", "20")
  return `/api/orders/mine?${params.toString()}`
}

export function orderAvailablePath(page = 0): string {
  return `/api/orders/available?page=${Math.max(0, page)}&size=20`
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

export function isoToDateTimeLocal(iso: string): string {
  const date = new Date(iso)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}
