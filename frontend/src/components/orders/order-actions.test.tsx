import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
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

  it("shows cancellation only to the courier for an accepted errand", () => {
    const onUpdated = vi.fn()

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={onUpdated} />)

    expect(screen.getByRole("button", { name: "Start errand" })).toBeInTheDocument()
    expect(screen.getByRole("button", { name: "Cancel errand" })).toBeInTheDocument()

    cleanup()
    render(<OrderActions order={{ ...acceptedOrder, status: "IN_PROGRESS" }} mode="courier" onUpdated={onUpdated} />)
    expect(screen.queryByRole("button", { name: "Cancel errand" })).not.toBeInTheDocument()

    cleanup()
    render(<OrderActions order={acceptedOrder} mode="requester" onUpdated={onUpdated} />)
    expect(screen.queryByRole("button", { name: "Cancel errand" })).not.toBeInTheDocument()
  })

  it("confirms cancellation, calls the existing versioned API, and returns the aborted order", async () => {
    const abortedOrder = { ...acceptedOrder, status: "ABORTED", courierId: null } as unknown as Order
    const onUpdated = vi.fn()
    mocks.api.mockResolvedValue(abortedOrder)

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={onUpdated} />)

    fireEvent.click(screen.getByRole("button", { name: "Cancel errand" }))
    expect(screen.getByRole("heading", { name: "Cancel this accepted errand?" })).toBeInTheDocument()
    expect(mocks.api).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole("button", { name: "Yes, cancel errand" }))

    await waitFor(() => {
      expect(mocks.api).toHaveBeenCalledWith("/api/orders/order-1/cancel-accepted", {
        method: "POST",
        body: expect.objectContaining({
          actorId: "courier-1",
          expectedVersion: 4,
        }),
      })
    })
    expect(onUpdated).toHaveBeenCalledWith(abortedOrder)
    expect(mocks.success).toHaveBeenCalledWith("Errand cancelled")
  })

  it("shows the API error when an accepted cancellation cannot be completed", async () => {
    mocks.api.mockRejectedValue(new Error("This errand is no longer accepted."))

    render(<OrderActions order={acceptedOrder} mode="courier" onUpdated={vi.fn()} />)
    fireEvent.click(screen.getByRole("button", { name: "Cancel errand" }))
    fireEvent.click(screen.getByRole("button", { name: "Yes, cancel errand" }))

    await waitFor(() => {
      expect(mocks.error).toHaveBeenCalledWith("This errand is no longer accepted.")
    })
  })
})
