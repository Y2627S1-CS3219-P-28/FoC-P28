import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

import { RepostControls } from "@/components/orders/repost-controls"
import type { Order } from "@/lib/orders"

const mocks = vi.hoisted(() => ({ api: vi.fn(), error: vi.fn(), user: { uid: "requester-1" } }))
vi.mock("@/hooks/use-api", () => ({ useApi: () => mocks.api }))
vi.mock("@/components/providers/auth-provider", () => ({ useAuth: () => ({ user: mocks.user }) }))
vi.mock("sonner", () => ({ toast: { success: vi.fn(), error: mocks.error } }))

const expiredOrder: Order = {
  id: "old-order", requesterId: "requester-1", courierId: null, itemDescription: "Pick up a parcel",
  pickupSupplierId: "store", deliverySupplierId: "hall", offeredCredits: 2, status: "EXPIRED",
  createdAt: new Date(2026, 9, 7, 10).toISOString(), expiresAt: new Date(2026, 9, 7, 11, 7).toISOString(),
  deliveryTimeLimitMinutes: 15, version: 2, originalOrderId: null, repostedOrderId: null,
}

beforeEach(() => {
  vi.clearAllMocks()
  vi.useFakeTimers({ toFake: ["Date"] })
  vi.setSystemTime(new Date(2026, 9, 8, 10, 1, 30))
})
afterEach(() => { cleanup(); vi.useRealTimers() })

describe("manual repost quarter-hour expiry", () => {
  it("starts at a future quarter-hour even for an old errand and posts the selected expiry", async () => {
    const updated = { ...expiredOrder, id: "repost", status: "OPEN" }
    const onUpdated = vi.fn()
    mocks.api.mockResolvedValue(updated)
    render(<RepostControls order={expiredOrder} onUpdated={onUpdated} />)
    expect(screen.getByLabelText("New expiry date")).toHaveValue("2026-10-08")
    expect(screen.getByRole("combobox", { name: "New expiry minutes" })).toHaveTextContent("45")
    fireEvent.click(screen.getByRole("button", { name: "Create repost" }))
    await waitFor(() => expect(mocks.api).toHaveBeenCalledWith("/api/orders/old-order/repost", {
      method: "POST", body: expect.objectContaining({ actorId: "requester-1",
        expiresAt: new Date(2026, 9, 8, 10, 45).toISOString(), expectedVersion: 2 }),
    }))
    expect(onUpdated).toHaveBeenCalledWith(updated)
  })

  it("does not post when the date is cleared", () => {
    render(<RepostControls order={expiredOrder} onUpdated={vi.fn()} />)
    fireEvent.change(screen.getByLabelText("New expiry date"), { target: { value: "" } })
    fireEvent.click(screen.getByRole("button", { name: "Create repost" }))
    expect(mocks.api).not.toHaveBeenCalled()
    expect(mocks.error).toHaveBeenCalledWith("Choose expiry minutes of 00, 15, 30, or 45.")
  })
})
