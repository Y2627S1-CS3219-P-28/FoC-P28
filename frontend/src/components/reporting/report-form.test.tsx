/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-13
 * Mode: Implementation.
 * Scope: Generated or updated frontend code for the team-specified report form, order association, navigation, and completion reminder.
 * Author review: I reviewed for correctness
 */
import type { ReactNode } from "react"
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { ReportForm } from "@/components/reporting/report-form"
import { ApiError } from "@/lib/api"
import type { Order } from "@/lib/orders"

const mocks = vi.hoisted(() => ({ api: vi.fn() }))
vi.mock("@/hooks/use-api", () => ({ useApi: () => mocks.api }))
vi.mock("@/components/require-auth", () => ({ RequireAuth: ({ children }: { children: ReactNode }) => children }))

const order = (id: string, status: Order["status"]): Order => ({
  id, requesterId: "user", courierId: status === "ABORTED" ? "user" : null,
  itemDescription: `Order ${id}`, pickupSupplierId: "a", deliverySupplierId: "b", offeredCredits: 2,
  status, createdAt: "2026-10-12T00:00:00Z", expiresAt: "2026-10-12T01:00:00Z",
  deliveryTimeLimitMinutes: 30, version: 0, originalOrderId: null, repostedOrderId: null,
})

beforeEach(() => {
  vi.clearAllMocks()
  Object.defineProperty(URL, "createObjectURL", { configurable: true, value: vi.fn(() => "blob:preview") })
  Object.defineProperty(URL, "revokeObjectURL", { configurable: true, value: vi.fn() })
})
afterEach(cleanup)

describe("report form", () => {
  it("follows every association page, renders all statuses, and applies only a valid prefill", async () => {
    mocks.api.mockImplementation((path: string) => path.includes("page=1")
      ? Promise.resolve({ items: [order("completed", "COMPLETED"), order("aborted", "ABORTED")], page: 1, size: 100, totalItems: 4, totalPages: 2 })
      : Promise.resolve({ items: [order("cancelled", "CANCELLED"), order("expired", "EXPIRED")], page: 2, size: 100, totalItems: 4, totalPages: 2 }))
    render(<ReportForm initialCategory="ORDER" initialOrderId="cancelled" />)
    await waitFor(() => expect(mocks.api).toHaveBeenCalledWith("/api/orders/mine/associated?page=2&size=100", expect.anything()))
    expect(screen.queryByText("Private report")).not.toBeInTheDocument()
    expect(screen.getByText("Category (required)")).toBeInTheDocument()
    expect(screen.getByText("Associated order (required)")).toBeInTheDocument()
    expect(screen.getByText("Description (required)")).toBeInTheDocument()
    expect(screen.getByText("Photos")).toBeInTheDocument()
    expect(screen.getByRole("combobox", { name: "Associated order" })).toHaveTextContent(/CANCELLED/)
    fireEvent.click(screen.getByRole("combobox", { name: "Associated order" }))
    expect(await screen.findByRole("option", { name: /COMPLETED/ })).toBeInTheDocument()
    expect(screen.getByRole("option", { name: /CANCELLED/ })).toBeInTheDocument()
    expect(screen.getByRole("option", { name: /ABORTED/ })).toBeInTheDocument()
    expect(screen.getByRole("option", { name: /EXPIRED/ })).toBeInTheDocument()
  })

  it("requires an order for ORDER and COURIER but submits optional CREDITS without one as FormData", async () => {
    mocks.api.mockImplementation((path: string) => path.startsWith("/api/orders")
      ? Promise.resolve({ items: [], page: 1, size: 100, totalItems: 0, totalPages: 0 })
      : Promise.resolve({ id: "report-1" }))
    render(<ReportForm initialCategory="ORDER" />)
    await waitFor(() => expect(mocks.api).toHaveBeenCalled())
    fireEvent.change(screen.getByLabelText("Description (required)"), { target: { value: "Missing package" } })
    fireEvent.click(screen.getByRole("button", { name: "Submit report" }))
    expect(await screen.findByText("Select an associated order for this category.")).toBeInTheDocument()
    expect(mocks.api.mock.calls.filter(([path]) => path === "/api/reports")).toHaveLength(0)

    fireEvent.click(screen.getByRole("combobox", { name: "Report category" }))
    fireEvent.pointerDown(await screen.findByRole("option", { name: "Credits" }), { pointerType: "mouse" })
    fireEvent.click(screen.getByRole("option", { name: "Credits" }))
    expect(screen.getByText("Associated order")).toBeInTheDocument()
    expect(screen.queryByText("Associated order (optional)")).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Submit report" }))
    await waitFor(() => expect(mocks.api.mock.calls.some(([path]) => path === "/api/reports")).toBe(true))
    const options = mocks.api.mock.calls.find(([path]) => path === "/api/reports")?.[1]
    expect(options.body).toBeInstanceOf(FormData)
    expect((options.body as FormData).getAll("photos")).toHaveLength(0)
  })

  it("previews and removes valid photos, rejects invalid files, and keeps contents for retry", async () => {
    let attempts = 0
    mocks.api.mockImplementation((path: string) => {
      if (path.startsWith("/api/orders")) return Promise.resolve({ items: [], page: 1, size: 100, totalItems: 0, totalPages: 0 })
      attempts += 1
      return attempts === 1 ? Promise.reject(new ApiError(503, "SERVICE_UNAVAILABLE", "Storage is unavailable.")) : Promise.resolve({ id: "report-2" })
    })
    render(<ReportForm initialCategory="GENERAL" />)
    await waitFor(() => expect(mocks.api).toHaveBeenCalled())
    fireEvent.change(screen.getByLabelText("Description (required)"), { target: { value: "Keep this description" } })
    const photos = screen.getByLabelText("Photos")
    fireEvent.change(photos, { target: { files: [new File([new Uint8Array([1])], "bad.gif", { type: "image/gif" })] } })
    expect(await screen.findByText(/non-empty JPEG, PNG, or WebP/)).toBeInTheDocument()
    const png = new File([new Uint8Array([0x89, 0x50, 0x4e, 0x47])], "receipt.png", { type: "image/png" })
    fireEvent.change(photos, { target: { files: [png] } })
    expect(await screen.findByAltText("Preview of receipt.png")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Remove receipt.png" }))
    expect(screen.queryByAltText("Preview of receipt.png")).not.toBeInTheDocument()
    fireEvent.change(photos, { target: { files: [png] } })
    fireEvent.click(screen.getByRole("button", { name: "Submit report" }))
    expect(await screen.findByText("Storage is unavailable.")).toBeInTheDocument()
    expect(screen.getByLabelText("Description (required)")).toHaveValue("Keep this description")
    expect(screen.getByAltText("Preview of receipt.png")).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Submit report" }))
    expect(await screen.findByText("Reference: report-2")).toBeInTheDocument()
  })

  it("does not accept a prefilled order absent from the authoritative response", async () => {
    mocks.api.mockResolvedValue({ items: [order("other", "EXPIRED")], page: 1, size: 100, totalItems: 1, totalPages: 1 })
    render(<ReportForm initialCategory="ORDER" initialOrderId="unrelated" />)
    expect(await screen.findByText(/linked order is not available/)).toBeInTheDocument()
    expect(screen.getByRole("combobox", { name: "Associated order" })).not.toHaveTextContent("unrelated")
  })

  it("prevents duplicate submission while the first request is pending", async () => {
    let resolveReport!: (value: { id: string }) => void
    const pending = new Promise<{ id: string }>(resolve => { resolveReport = resolve })
    mocks.api.mockImplementation((path: string) => path.startsWith("/api/orders")
      ? Promise.resolve({ items: [], page: 1, size: 100, totalItems: 0, totalPages: 0 }) : pending)
    render(<ReportForm initialCategory="GENERAL" />)
    await waitFor(() => expect(mocks.api).toHaveBeenCalled())
    fireEvent.change(screen.getByLabelText("Description (required)"), { target: { value: "One report only" } })
    const submit = screen.getByRole("button", { name: "Submit report" })
    fireEvent.click(submit); fireEvent.click(submit)
    expect(mocks.api.mock.calls.filter(([path]) => path === "/api/reports")).toHaveLength(1)
    expect(screen.getByRole("button", { name: /Submitting/ })).toBeDisabled()
    resolveReport({ id: "once" })
    expect(await screen.findByText("Reference: once")).toBeInTheDocument()
  })
})
