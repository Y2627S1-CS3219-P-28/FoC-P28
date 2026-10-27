/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated or updated frontend code for the team-specified report form, order association, navigation, and completion reminder.
 * Author review: I reviewed for correctness
 */
import type { Order, OrderPage } from "@/lib/orders"

export type ReportCategory = "ORDER" | "COURIER" | "CREDITS" | "GENERAL"
export type ReportStatus = "AWAITING_COURIER_RESPONSE" | "READY_FOR_REVIEW" | "RESOLVED"

export type ReportAttachment = {
  id: string
  filename: string
  contentType: string
  size: number
  uploadedAt: string
}

export type ReportResponse = {
  id: string
  reporterId: string
  category: ReportCategory
  description: string
  orderId: string | null
  courierId: string | null
  status: ReportStatus
  createdAt: string
  responseDeadlineAt: string | null
  courierResponse: string | null
  respondedAt: string | null
  resolvedAt: string | null
  attachments: ReportAttachment[]
  responseAttachments: ReportAttachment[]
}

export type ReportPage = {
  items: ReportResponse[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export type AssociatedOrderPage = OrderPage

export const REPORT_CATEGORIES: Record<ReportCategory, string> = {
  ORDER: "Order",
  COURIER: "Courier",
  CREDITS: "Credits",
  GENERAL: "General",
}

export function reportCategoryRequiresOrder(category: ReportCategory) {
  return category === "ORDER" || category === "COURIER"
}

export function validReportCategory(value: string | undefined): ReportCategory {
  return value && value in REPORT_CATEGORIES ? value as ReportCategory : "GENERAL"
}

export function associatedOrderLabel(order: Order) {
  return `${order.itemDescription} — ${order.status.replaceAll("_", " ")} — ${order.id}`
}

export const REPORT_STATUS_LABELS: Record<ReportStatus, string> = {
  AWAITING_COURIER_RESPONSE: "Awaiting courier response",
  READY_FOR_REVIEW: "Ready for review",
  RESOLVED: "Resolved",
}
