import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import * as React from "react"
import { AdminReportsPage } from "./reports-page"
const mocks = vi.hoisted(() => ({ mode: "mock", list: vi.fn(), detail: vi.fn(), resolve: vi.fn(), refresh: vi.fn(), uid: "courier" }))
vi.mock("@/components/providers/config-provider", () => ({ useConfig: () => ({ reportApiMode: mocks.mode }) }))
vi.mock("@/components/providers/auth-provider", () => ({ useAuth: () => ({ user: { uid: mocks.uid }, loading: false }) }))
vi.mock("@/components/require-auth", () => ({ RequireAuth: ({ children }: { children: React.ReactNode }) => children }))
vi.mock("@/hooks/use-reports", () => ({ useReportApi: () => mocks,
  useReportResource: (load: (signal: AbortSignal) => Promise<unknown>, enabled: boolean) => {
    const [data, setData] = React.useState<unknown>(null)
    React.useEffect(() => { if (enabled) void load(new AbortController().signal).then(setData) }, [load, enabled])
    return { data, loading: !data && enabled, error: null, refresh: mocks.refresh }
  },
}))
vi.mock("@/hooks/use-admin-api", () => ({ useAdminApi: () => ({ mode: mocks.mode }) }))
vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }))
const report = { id: "report-1", requesterId: "owner", courierId: "courier", status: "AWAITING_COURIER_RESPONSE",
  violationCategory: "WRONG_LOCATION_DELIVERY", requesterExplanation: "Delivered to a different building", requesterAttachments: [], courierAttachments: [],
  courierExplanation: null, courierResponseSubmittedAt: null, createdAt: "2026-10-18T10:00:00Z",
  order: { id: "order-1", status: "DELIVERED", isCompletionHold: true, isOverdue: true, requesterId: "owner", courierId: "courier", requesterName: "Alex", courierName: "Jamie", itemDescription: "Books", pickupSupplierId: "p", deliverySupplierId: "d", offeredCredits: 5, deliveryTimeLimitMinutes: 30, expiresAt: "2026-10-18T11:00:00Z" } }
beforeEach(() => { vi.clearAllMocks(); mocks.mode = "mock"; mocks.uid = "courier"; mocks.detail.mockResolvedValue(report); mocks.list.mockImplementation(async (_user: string, _mode: string, _page: number, status: string) => ({ items: status === "RESOLVED" ? [] : [report], page: 1, totalPages: status === "RESOLVED" ? 0 : 1, totalItems: status === "RESOLVED" ? 0 : 1 })); mocks.resolve.mockResolvedValue({ ...report, status: "RESOLVED" }) })
afterEach(cleanup)
describe("Reports", () => {
  it("shows Coming soon without fetching samples in live mode", () => {
    mocks.mode = "live"; render(<AdminReportsPage />)
    expect(screen.getByText("Coming soon")).toBeInTheDocument(); expect(mocks.list).not.toHaveBeenCalled()
    expect(screen.queryByText(/Sample reports/)).not.toBeInTheDocument()
  })
  it("searches exact Order ID, filters report status and clears both without a refresh button", async () => {
    mocks.list.mockImplementation(async (_user: string, _mode: string, page: number) => ({ items: [report], page, totalPages: 2, totalItems: 21 }))
    render(<AdminReportsPage />)
    expect(screen.getByText(/Sample reports/)).toBeInTheDocument()
    fireEvent.click(await screen.findByRole("button", { name: "Next" }))
    await waitFor(() => expect(mocks.list).toHaveBeenCalledWith("courier", "admin", 2, "", expect.any(AbortSignal), { orderId: "", status: "" }))
    expect(screen.queryByRole("button", { name: "Refresh reports" })).not.toBeInTheDocument()
    fireEvent.change(screen.getByLabelText("Order ID"), { target: { value: " order-1 " } })
    fireEvent.click(screen.getByRole("button", { name: "Search" }))
    await waitFor(() => expect(mocks.list).toHaveBeenCalledWith("courier", "admin", 1, "", expect.any(AbortSignal), { orderId: "order-1", status: "" }))
    expect(screen.getByRole("combobox", { name: "Report status" }).tagName).toBe("SELECT")
    fireEvent.change(screen.getByRole("combobox", { name: "Report status" }), { target: { value: "READY_FOR_REVIEW" } })
    await waitFor(() => expect(mocks.list).toHaveBeenCalledWith("courier", "admin", 1, "", expect.any(AbortSignal), { orderId: "order-1", status: "READY_FOR_REVIEW" }))
    fireEvent.click(screen.getByRole("button", { name: "Clear" }))
    expect(screen.getByLabelText("Order ID")).toHaveValue("")
    await waitFor(() => expect(mocks.list).toHaveBeenCalledWith("courier", "admin", 1, "", expect.any(AbortSignal), { orderId: "", status: "" }))
  })
  it("shows active and resolved cards in one list without Open/Closed headings", async () => {
    mocks.list.mockResolvedValue({ items: [report, { ...report, id: "resolved-1", status: "RESOLVED", resolvedAt: "2026-10-18T19:00:00Z" }], page: 1, totalPages: 1, totalItems: 2 })
    render(<AdminReportsPage />)
    expect(await screen.findAllByRole("link", { name: "View report" })).toHaveLength(2)
    expect(screen.queryByRole("heading", { name: "Open reports" })).not.toBeInTheDocument()
    expect(screen.queryByRole("heading", { name: "Closed reports" })).not.toBeInTheDocument()
    expect(screen.getByText("Resolved", { selector: '[data-slot="badge"]' })).toBeInTheDocument()
    expect(screen.queryByText("Books")).not.toBeInTheDocument()
    expect(screen.getAllByRole("link", { name: "View report" })[1]).toHaveAttribute("href", "/admin/reports/resolved-1")
    expect(mocks.list).toHaveBeenCalledTimes(1)
  })
  it("keeps closed reports viewable without leaking the private reply or repeating the Order description", async () => {
    mocks.list.mockResolvedValue({ items: [{ ...report, status: "RESOLVED", resolutionNote: "Private reply", resolvedAt: "2026-10-18T19:00:00Z" }], page: 1, totalPages: 1, totalItems: 1 })
    render(<AdminReportsPage />)
    await screen.findAllByRole("link", { name: "View report" })
    expect(screen.queryByText("Private reply")).not.toBeInTheDocument()
    expect(screen.queryByText("Books")).not.toBeInTheDocument()
    for (const link of screen.getAllByRole("link", { name: "View report" })) expect(link).toHaveAttribute("href", "/admin/reports/report-1")
  })
  it("puts a clickable order first in one panel and keeps only report resolution", async () => {
    mocks.uid = "admin"; mocks.detail.mockResolvedValue({ ...report, status: "READY_FOR_REVIEW" })
    render(<AdminReportsPage reportId="report-1" />)
    const orderLink = await screen.findByRole("link", { name: /Open order order-1/ })
    expect(screen.getByText("Description")).toBeInTheDocument()
    expect(screen.getAllByText("Alex").length).toBeGreaterThan(0)
    expect(screen.getAllByText("Jamie").length).toBeGreaterThan(0)
    expect(orderLink).toHaveAttribute("href", "/admin/orders/order-1")
    expect(screen.queryByText("View full order details")).not.toBeInTheDocument()
    for (const name of ["Revert overdue", "Mark as completed", "Cancel order"]) expect(screen.queryByRole("button", { name })).not.toBeInTheDocument()
    expect(document.querySelectorAll('[data-slot="card"]')).toHaveLength(1)
    expect(screen.getByRole("heading", { name: "Order" }).compareDocumentPosition(screen.getByRole("heading", { name: "Report information" })) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.getByRole("link", { name: "Alex" })).toHaveAttribute("href", "/admin/users/owner")
    expect(screen.getByText("Requester name")).toBeInTheDocument()
    expect(screen.getByText("Requester ID")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Mark report resolved" }))
    expect(screen.getByText(/only visible to the submitter/)).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Confirm resolved" })).toBeDisabled()
    fireEvent.change(screen.getByLabelText("Admin reply"), { target: { value: "Evidence reviewed." } })
    fireEvent.click(screen.getByRole("button", { name: "Confirm resolved" }))
    await waitFor(() => expect(mocks.resolve).toHaveBeenCalledWith("report-1", "admin", "Evidence reviewed."))
  })
  it("shows resolution history without requester or courier editing", async () => {
    mocks.detail.mockResolvedValue({ ...report, status: "RESOLVED", resolvedAt: "2026-10-18T19:00:00Z", resolvedBy: "admin" })
    render(<AdminReportsPage reportId="report-1" />)
    await screen.findByRole("heading", { name: "Resolution" })
    expect(screen.queryByRole("button", { name: "Submit response" })).not.toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Submit report" })).not.toBeInTheDocument()
    expect(screen.getByText("Admin reply")).toBeInTheDocument()
  })
})
