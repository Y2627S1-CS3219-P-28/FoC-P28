/*
 * AI Assistance Disclosure:
 * Tool: OpenAI Codex (GPT-5), date: 2026-10-27
 * Mode: Test generation and verification.
 * Scope: Generated tests for reporting tabs, scoped APIs, courier responses, retry preservation, and expired response UI.
 * Author review: I reviewed for correctness
 */
import type { ReactNode } from "react"
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { ReportingHub } from "@/components/reporting/reporting-hub"
import { ApiError } from "@/lib/api"
import type { ReportResponse } from "@/lib/reporting"

const mocks = vi.hoisted(() => ({ api: vi.fn() }))
vi.mock("@/hooks/use-api", () => ({ useApi: () => mocks.api }))
vi.mock("@/components/providers/auth-provider", () => ({ useAuth: () => ({ user: { uid: "courier-1" }, loading: false }) }))
vi.mock("@/components/require-auth", () => ({ RequireAuth: ({ children }: { children: ReactNode }) => children }))

const baseReport: ReportResponse = {
  id: "report-1",
  reporterId: "requester-1",
  category: "COURIER",
  description: "The delivery was damaged.",
  orderId: "order-1",
  courierId: "courier-1",
  status: "AWAITING_COURIER_RESPONSE",
  createdAt: "2026-10-27T00:00:00Z",
  responseDeadlineAt: "2026-10-28T00:00:00Z",
  courierResponse: null,
  respondedAt: null,
  resolvedAt: null,
  attachments: [],
  responseAttachments: [],
}

const page = (items: ReportResponse[]) => ({ items, page: 1, size: 20, totalItems: items.length, totalPages: 1 })

beforeEach(() => vi.clearAllMocks())
afterEach(cleanup)

describe("reporting hub", () => {
  it("loads submitted and against-me reports from their scoped endpoints", async () => {
    mocks.api.mockImplementation((path: string) => {
      if (path.includes("view=submitted")) return Promise.resolve(page([{ ...baseReport, courierResponse: "I packed it carefully.", respondedAt: "2026-10-27T01:00:00Z", status: "READY_FOR_REVIEW" }]))
      if (path.includes("view=against-me")) return Promise.resolve(page([baseReport]))
      return Promise.resolve(page([]))
    })

    render(<ReportingHub initialTab="submitted" initialCategory="GENERAL" />)
    expect(await screen.findByText("I packed it carefully.")).toBeInTheDocument()
    expect(mocks.api).toHaveBeenCalledWith("/api/reports/mine?view=submitted&page=1&size=20", expect.anything())

    fireEvent.click(screen.getByRole("tab", { name: "Against me" }))
    expect(await screen.findByRole("button", { name: "Submit response" })).toBeInTheDocument()
    expect(mocks.api).toHaveBeenCalledWith("/api/reports/mine?view=against-me&page=1&size=20", expect.anything())
  })

  it("submits one multipart courier response and keeps its fields after a failed attempt", async () => {
    let responseAttempts = 0
    mocks.api.mockImplementation((path: string) => {
      if (path === "/api/reports/report-1/response") {
        responseAttempts += 1
        return responseAttempts === 1
          ? Promise.reject(new ApiError(503, "SERVICE_UNAVAILABLE", "Evidence storage is unavailable."))
          : Promise.resolve({ ...baseReport, courierResponse: "The item was intact at delivery.", respondedAt: "2026-10-27T01:00:00Z", status: "READY_FOR_REVIEW" })
      }
      return Promise.resolve(page([baseReport]))
    })

    render(<ReportingHub initialTab="against-me" initialCategory="GENERAL" />)
    const explanation = await screen.findByLabelText("Your response (required)")
    fireEvent.change(explanation, { target: { value: "The item was intact at delivery." } })
    const photo = new File([new Uint8Array([1])], "handoff.webp", { type: "image/webp" })
    fireEvent.change(screen.getByLabelText("Evidence photos"), { target: { files: [photo] } })
    fireEvent.click(screen.getByRole("button", { name: "Submit response" }))

    expect(await screen.findByText("Evidence storage is unavailable.")).toBeInTheDocument()
    expect(explanation).toHaveValue("The item was intact at delivery.")
    expect(screen.getByText(/1 selected/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole("button", { name: "Submit response" }))
    await waitFor(() => expect(responseAttempts).toBe(2))
    const responseCall = mocks.api.mock.calls.filter(([path]) => path === "/api/reports/report-1/response").at(-1)
    expect(responseCall?.[1]).toMatchObject({ method: "POST", body: expect.any(FormData) })
  })

  it("shows an expired report as closed without a response form", async () => {
    mocks.api.mockResolvedValue(page([{ ...baseReport, status: "READY_FOR_REVIEW" }]))
    render(<ReportingHub initialTab="against-me" initialCategory="GENERAL" />)
    expect(await screen.findByText("Response window closed")).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Submit response" })).not.toBeInTheDocument()
  })
})
