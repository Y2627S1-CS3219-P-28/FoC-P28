export type Supplier = { id: string; name: string; location: string }
export type Errand = {
  id: string; requesterId: string; description: string; pickupSupplierId: string; deliverySupplierId: string
  creditAmount: number; deliveryDurationMinutes: number; expiresAt: string; createdAt: string
  status: "OPEN" | "ACCEPTED"; orderId: string | null; version: number; pickup: Supplier; delivery: Supplier
}
export type Delivery = {
  id: string; errandId: string; courierId: string; status: "ACCEPTED" | "IN_PROGRESS" | "PICKED_UP" | "DELIVERED"
  deliveryDurationMinutes: number; version: number; acceptedAt: string; startedAt: string | null
  pickedUpAt: string | null; deliveredAt: string | null; deliveryDeadline: string | null; errand: Errand
  checkpoints: { id: string; status: string; occurredAt: string; supplierId: string | null }[]
}
export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number }
export const orderPath = "/api/orders"
export function singaporeTime(value: string) {
  return new Intl.DateTimeFormat("en-SG", {
    timeZone: "Asia/Singapore", dateStyle: "medium", timeStyle: "short",
  }).format(new Date(value))
}
