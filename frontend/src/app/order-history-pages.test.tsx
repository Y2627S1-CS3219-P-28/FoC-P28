import type { ReactNode } from "react"
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import MyRequestsPage from "@/app/my-requests/page"
import MyErrandsPage from "@/app/my-errands/page"
import type { Order } from "@/lib/orders"

const mocks = vi.hoisted(() => ({ api: vi.fn(), user: { uid: "user" } }))
vi.mock("@/hooks/use-api", () => ({ useApi: () => mocks.api }))
vi.mock("@/components/providers/auth-provider", () => ({ useAuth: () => ({ user: mocks.user }) }))
vi.mock("@/components/require-auth", () => ({ RequireAuth: ({ children }: { children: ReactNode }) => children }))
vi.mock("@/hooks/use-supplier-names", () => ({ useSupplierNames: () => ({ names: {}, ready: true }) }))
vi.mock("@/components/orders/order-card", () => ({ OrderCard: ({ order, actions, footer }: {
  order: Order; actions: ReactNode; footer: ReactNode
}) => <article>{order.itemDescription} {order.status} {order.attemptId}{actions}{footer}</article> }))
vi.mock("@/components/orders/repost-controls", () => ({ RepostControls: ({ order, onUpdated }: {
  order: Order; onUpdated: (order: Order) => void
}) => <button onClick={() => onUpdated({ ...order, id: "new", originalOrderId: order.id,
  status: "OPEN", itemDescription: "New request" })}>Repost</button> }))
vi.mock("@/components/orders/order-actions", () => ({ OrderActions: ({ order, onUpdated }: {
  order: Order; onUpdated: (order: Order) => void
}) => order.status === "ACCEPTED" ? <button onClick={() => onUpdated({ ...order,
  status: "OPEN", courierId: null })}>Abort</button> : null }))

const order: Order = {
  id: "old", requesterId: "user", courierId: null, itemDescription: "Old request",
  pickupSupplierId: "pickup", deliverySupplierId: "delivery", offeredCredits: 3, status: "EXPIRED",
  createdAt: "2026-10-08T00:00:00Z", expiresAt: "2026-10-08T01:00:00Z", deliveryTimeLimitMinutes: 30,
  version: 0, originalOrderId: null, repostedOrderId: null,
}

beforeEach(() => vi.clearAllMocks())
afterEach(cleanup)

describe("Order history page integration", () => {
  it("shows the new manual repost immediately and removes its expired original", async () => {
    mocks.api.mockResolvedValueOnce({ items: [order] })
      .mockResolvedValue({ items: [{ ...order, id: "new", originalOrderId: order.id,
        status: "OPEN", itemDescription: "New request" }] })
    render(<MyRequestsPage />)
    await screen.findByText(/Old request EXPIRED/)
    await act(async () => fireEvent.click(screen.getByRole("button", { name: "Repost" })))
    expect(screen.getByText(/New request OPEN/)).toBeInTheDocument()
    expect(screen.queryByText(/Old request EXPIRED/)).not.toBeInTheDocument()
  })

  it("reloads aborted courier attempts after the current order reopens", async () => {
    mocks.api.mockResolvedValueOnce({ items: [{ ...order, courierId: "user", status: "ACCEPTED" }] })
      .mockResolvedValueOnce({ items: [{ ...order, courierId: "user", status: "ABORTED", attemptId: "attempt-1" }] })
    render(<MyErrandsPage />)
    fireEvent.click(await screen.findByRole("button", { name: "Abort" }))
    await waitFor(() => expect(mocks.api).toHaveBeenCalledTimes(2))
    expect(await screen.findByText(/ABORTED attempt-1/)).toBeInTheDocument()
    expect(screen.queryByRole("button", { name: "Abort" })).not.toBeInTheDocument()
  })
})
