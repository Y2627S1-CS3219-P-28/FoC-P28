import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"

import { OrderActions } from "@/components/orders/order-actions"
import type { Order } from "@/lib/orders"

const mocks = vi.hoisted(() => ({
  api: vi.fn(),
  success: vi.fn(),
  error: vi.fn(),
}))

vi.mock("@/hooks/use-api", () => ({
  useApi: () => mocks.api,
}))
vi.mock("@/lib/order-command-storage", () => ({ readOrderIntent: async () => null }))

vi.mock("@/components/providers/auth-provider", () => ({
  useAuth: () => ({ user: { uid: "courier-1" } }),
}))

vi.mock("sonner", () => ({
  toast: {
    success: mocks.success,
    error: mocks.error,
  },
}))

const acceptedOrder: Order = {
  id: "order-1",
  requesterId: "requester-1",
  courierId: "courier-1",
  itemDescription: "Pick up a parcel",
  pickupSupplierId: "store-a",
  deliverySupplierId: "hall-b",
  offeredCredits: 12,
  status: "ACCEPTED",
  createdAt: "2026-10-01T09:00:00Z",
  expiresAt: "2026-10-01T10:00:00Z",
  deliveryTimeLimitMinutes: 30,
  version: 4,
  originalOrderId: null,
  repostedOrderId: null,
}

describe("OrderActions accepted cancellation", () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it("shows cancellation only to the courier for an accepted errand", async () => {
    const onUpdated = vi.fn()

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={onUpdated} />)
    await waitFor(() => expect(screen.getByRole("button", { name: "Abort errand" })).not.toBeDisabled())

    expect(screen.getByRole("button", { name: "Start errand" })).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Abort errand" })).toBeInTheDocument()

    cleanup()
    render(<OrderActions order={{ ...acceptedOrder, status: "IN_PROGRESS" }} mode="courier" onUpdated={onUpdated} />)
    await act(async () => {})
    expect(screen.queryByRole("button", { name: "Abort errand" })).not.toBeInTheDocument()

    cleanup()
    render(<OrderActions order={acceptedOrder} mode="requester" onUpdated={onUpdated} />)
    await act(async () => {})
    expect(screen.queryByRole("button", { name: "Abort errand" })).not.toBeInTheDocument()
  })

  it("confirms cancellation, calls the existing versioned API, and reports a reopened errand", async () => {
    const reopenedOrder = { ...acceptedOrder, status: "OPEN", courierId: null } as unknown as Order
    const onUpdated = vi.fn()
    mocks.api.mockResolvedValue(reopenedOrder)

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={onUpdated} />)
    await waitFor(() => expect(screen.getByRole("button", { name: "Abort errand" })).not.toBeDisabled())

    fireEvent.click(screen.getByRole("button", { name: "Abort errand" }))
    expect(screen.getByRole("heading", { name: "Abort this accepted errand?" })).toBeInTheDocument()
    expect(mocks.api).not.toHaveBeenCalledWith(expect.anything(), expect.objectContaining({ method: "POST" }))

    fireEvent.click(screen.getByRole("button", { name: "Yes, abort errand" }))

    await waitFor(() => {
      expect(mocks.api).toHaveBeenCalledWith("/api/orders/order-1/cancel-accepted", {
        method: "POST",
        body: expect.objectContaining({
          actorId: "courier-1",
          expectedVersion: 4,
        }),
      })
    })
    expect(onUpdated).toHaveBeenCalledWith(reopenedOrder)
    expect(mocks.success).toHaveBeenCalledWith("Errand reopened for other couriers")
  })

  it("reports a final cancellation when the API returns an aborted errand", async () => {
    const abortedOrder = { ...acceptedOrder, status: "ABORTED", courierId: null } as unknown as Order
    mocks.api.mockResolvedValue(abortedOrder)

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={vi.fn()} />)
    await waitFor(() => expect(screen.getByRole("button", { name: "Abort errand" })).not.toBeDisabled())
    fireEvent.click(screen.getByRole("button", { name: "Abort errand" }))
    fireEvent.click(screen.getByRole("button", { name: "Yes, abort errand" }))

    await waitFor(() => expect(mocks.success).toHaveBeenCalledWith("Expired errand aborted"))
  })

  it("shows the API error when an accepted cancellation cannot be completed", async () => {
    mocks.api.mockImplementation(async (path: string) => {
      if (path.endsWith("capabilities")) return { enabled: false }
      throw new Error("This errand is no longer accepted.")
    })

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={vi.fn()} />)
    await waitFor(() => expect(screen.getByRole("button", { name: "Abort errand" })).not.toBeDisabled())
    fireEvent.click(screen.getByRole("button", { name: "Abort errand" }))
    fireEvent.click(screen.getByRole("button", { name: "Yes, abort errand" }))

    await waitFor(() => {
      expect(mocks.error).toHaveBeenCalledWith("This errand is no longer accepted.")
    })
  })
})
