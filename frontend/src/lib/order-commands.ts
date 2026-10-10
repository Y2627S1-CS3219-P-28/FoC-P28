import type { Order } from "@/lib/orders"

export type OrderCommandKind = "CREATE" | "ACCEPT" | "ABORT"
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
}

export function commandPath(kind: OrderCommandKind, legacyPath: string): string {
  if (kind === "CREATE") return "/api/orders/commands/create"
  const orderId = legacyPath.split("/")[3]
  return `/api/orders/commands/${encodeURIComponent(orderId)}/${kind === "ACCEPT" ? "accept" : "abort"}`
}
