import type { AdminOrder, AdminRequest, AdminApiMode } from "./admin-api"
import type { Page } from "./suppliers"
export type ReportStatus = "AWAITING_COURIER_RESPONSE" | "READY_FOR_REVIEW" | "RESOLVED"
export type ReportEvidence = { name: string; contentType: string; dataUrl: string }
export type DeliveryReport = {
  id: string; order: AdminOrder; requesterId: string; courierId: string; status: ReportStatus
  violationCategory: string; requesterExplanation: string; requesterAttachments: ReportEvidence[]
  courierExplanation: string | null; courierAttachments: ReportEvidence[]; createdAt: string
  courierResponseSubmittedAt: string | null
  resolvedAt: string | null; resolvedBy: string | null; resolutionNote: string | null
}
export const REPORT_STATUSES: Record<ReportStatus, string> = {
  AWAITING_COURIER_RESPONSE: "Awaiting courier response", READY_FOR_REVIEW: "Ready for review", RESOLVED: "Resolved",
}
export const REPORT_CATEGORIES: Record<string, string> = {
  ITEM_MISHANDLING_OR_DAMAGE: "Parcel damaged or destroyed", WRONG_LOCATION_DELIVERY: "Delivered to the wrong location",
}
export function createReportApi(request: AdminRequest, mode: AdminApiMode) {
  const root = "/api/orders/admin-mock/reports"
  const call: AdminRequest = mode === "mock" ? request : async () => { throw new Error("Coming soon: Report Service is not available yet.") }
  return {
    list: (userId: string, mode: "admin", page: number, status = "", signal?: AbortSignal, filters?: { orderId: string; status: string }) =>
      call<Page<DeliveryReport>>(root + "?" + new URLSearchParams({ userId, mode, page: String(page), status, orderId: filters?.orderId ?? "", reportStatus: filters?.status ?? "" }), { signal }),
    detail: (id: string, userId: string, signal?: AbortSignal) =>
      call<DeliveryReport>(root + "/" + encodeURIComponent(id) + "?" + new URLSearchParams({ userId, admin: "true" }), { signal }),
    resolve: (id: string, userId: string, explanation: string) => call<DeliveryReport>(root + "/" + encodeURIComponent(id) + "/resolve", { method: "PUT", body: { userId, explanation } }),
  }
}
