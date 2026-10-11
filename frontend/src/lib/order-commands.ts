import type { Order } from "@/lib/orders"

export type OrderCommandKind = "CREATE" | "ACCEPT" | "ABORT" | "REPOST"
export type OrderCommandIntent = {
  key: string
  kind: OrderCommandKind
  path: string
  body: Record<string, unknown>
}
export type OrderCommandStatus = {
  commandId: string
  kind?: OrderCommandKind
  orderId?: string
  status: "PENDING" | "COMPLETED"
  outcome?: "SUCCESS" | "REJECTED"
  reason?: string
  message?: string
  attemptCount?: number
  result?: Order
  displayDetails?: OrderCommandDisplayDetails | null
}

export type OrderCommandDisplayDetails = Pick<Order, "itemDescription" | "pickupSupplierId" | "deliverySupplierId" | "offeredCredits" | "deliveryTimeLimitMinutes" | "expiresAt"> & {
  orderStatus?: Order["status"] | null
  version?: number | null
}

export function commandPath(kind: OrderCommandKind, legacyPath: string): string {
  if (kind === "CREATE") return "/api/orders/commands/create"
  const orderId = legacyPath.split("/")[3]
  const action = kind === "ACCEPT" ? "accept" : kind === "REPOST" ? "repost" : "abort"
  return `/api/orders/commands/${encodeURIComponent(orderId)}/${action}`
}
